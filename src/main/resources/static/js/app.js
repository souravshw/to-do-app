/**
 * FocusTask — Modern Vanilla JavaScript Application Client
 * Features:
 * - REST API integration with optimistic UI updates & rollback
 * - Double-click inline task editing
 * - Animated strike-through & bottom-pinning for completed items
 * - Client-side real-time search & status filtering
 * - Toast notification feedback
 */

(function () {
    'use strict';

    // Application State
    let todos = [];
    let currentFilter = 'all'; // 'all' | 'active' | 'completed' | 'high'
    let searchQuery = '';

    // DOM Elements
    const todoListEl = document.getElementById('todo-list');
    const emptyStateEl = document.getElementById('empty-state');
    const newTaskForm = document.getElementById('new-task-form');
    const newTaskInput = document.getElementById('new-task-input');
    const newTaskPriority = document.getElementById('new-task-priority');
    const newTaskDate = document.getElementById('new-task-date');
    const newTaskCategory = document.getElementById('new-task-category');
    const searchInput = document.getElementById('search-input');
    const searchClearBtn = document.getElementById('search-clear-btn');
    const clearCompletedBtn = document.getElementById('clear-completed-btn');
    const progressBarFill = document.getElementById('progress-bar-fill');
    const progressPercentageLabel = document.getElementById('progress-percentage-label');
    const activeTasksLabel = document.getElementById('active-tasks-label');
    const statsPill = document.getElementById('stats-pill');
    const storageBadge = document.getElementById('storage-badge');
    const toastContainer = document.getElementById('toast-container');

    const filterPills = {
        all: document.getElementById('filter-all'),
        active: document.getElementById('filter-active'),
        completed: document.getElementById('filter-completed'),
        high: document.getElementById('filter-high')
    };

    // Initialize
    document.addEventListener('DOMContentLoaded', init);

    function init() {
        setupEventListeners();
        loadTodos();
        loadAppStatus();
    }

    function setupEventListeners() {
        // Form submission
        newTaskForm.addEventListener('submit', (e) => {
            e.preventDefault();
            handleCreateTodo();
        });

        // Search input
        searchInput.addEventListener('input', (e) => {
            searchQuery = e.target.value.trim().toLowerCase();
            if (searchQuery.length > 0) {
                searchClearBtn.classList.remove('hidden');
            } else {
                searchClearBtn.classList.add('hidden');
            }
            renderTodoList();
        });

        searchClearBtn.addEventListener('click', () => {
            searchInput.value = '';
            searchQuery = '';
            searchClearBtn.classList.add('hidden');
            searchInput.focus();
            renderTodoList();
        });

        // Filter pills
        Object.entries(filterPills).forEach(([filterName, element]) => {
            if (element) {
                element.addEventListener('click', () => {
                    setFilter(filterName);
                });
            }
        });

        // Clear completed button
        clearCompletedBtn.addEventListener('click', handleClearCompleted);
    }

    function setFilter(filter) {
        currentFilter = filter;
        Object.entries(filterPills).forEach(([name, btn]) => {
            const isActive = name === filter;
            btn.classList.toggle('active', isActive);
            btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
        });
        renderTodoList();
    }

    // =========================================================================
    // API & State Actions
    // =========================================================================

    async function loadTodos() {
        try {
            const res = await fetch('/api/todos');
            if (!res.ok) throw new Error('Failed to load tasks');
            todos = await res.json();
            renderTodoList();
            updateProgressAndStats();
        } catch (err) {
            showToast('Unable to load tasks from server', 'error');
        }
    }

    async function loadAppStatus() {
        try {
            const res = await fetch('/api/status');
            if (res.ok) {
                const data = await res.json();
                if (data.storageMode && storageBadge) {
                    storageBadge.textContent = data.storageMode;
                }
            }
        } catch (_) {}
    }

    async function handleCreateTodo() {
        const title = newTaskInput.value.trim();
        if (!title) return;

        const priority = newTaskPriority.value;
        const dueDate = newTaskDate.value || null;
        const category = newTaskCategory.value.trim() || null;

        const payload = { title, priority, dueDate, category };

        // Optimistic item
        const tempId = 'temp-' + Date.now();
        const optimisticItem = {
            id: tempId,
            title,
            priority,
            dueDate,
            category,
            completed: false,
            createdAt: Date.now(),
            updatedAt: Date.now()
        };

        // Insert optimistically at the top
        todos.unshift(optimisticItem);
        sortTodosInPlace();
        renderTodoList();
        updateProgressAndStats();

        // Clear input fields
        newTaskInput.value = '';
        newTaskCategory.value = '';
        newTaskDate.value = '';

        try {
            const res = await fetch('/api/todos', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error('Server returned ' + res.status);
            const savedItem = await res.json();

            // Replace temp item with saved item
            const idx = todos.findIndex(t => t.id === tempId);
            if (idx !== -1) {
                todos[idx] = savedItem;
            }
            sortTodosInPlace();
            renderTodoList();
            updateProgressAndStats();
            showToast('Task added successfully', 'success');
        } catch (err) {
            // Rollback
            todos = todos.filter(t => t.id !== tempId);
            renderTodoList();
            updateProgressAndStats();
            showToast('Failed to save task: ' + err.message, 'error');
        }
    }

    async function handleToggleTodo(id) {
        const item = todos.find(t => t.id === id);
        if (!item) return;

        const originalCompleted = item.completed;
        const newCompleted = !originalCompleted;

        // Optimistic update
        item.completed = newCompleted;
        item.updatedAt = Date.now();
        sortTodosInPlace();
        renderTodoList();
        updateProgressAndStats();

        try {
            const res = await fetch(`/api/todos/${id}/toggle`, {
                method: 'PATCH'
            });

            if (!res.ok) throw new Error('Toggle failed');
            const updated = await res.json();
            const idx = todos.findIndex(t => t.id === id);
            if (idx !== -1) {
                todos[idx] = updated;
            }
            sortTodosInPlace();
            renderTodoList();
            updateProgressAndStats();
        } catch (err) {
            // Rollback
            item.completed = originalCompleted;
            sortTodosInPlace();
            renderTodoList();
            updateProgressAndStats();
            showToast('Failed to update task status', 'error');
        }
    }

    async function handleUpdateTitle(id, newTitle, originalTitle, titleEl) {
        newTitle = newTitle.trim();
        if (!newTitle || newTitle === originalTitle) {
            titleEl.textContent = originalTitle;
            return;
        }

        const item = todos.find(t => t.id === id);
        if (!item) return;

        // Optimistic update
        item.title = newTitle;
        item.updatedAt = Date.now();
        titleEl.textContent = newTitle;

        try {
            const res = await fetch(`/api/todos/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    title: newTitle,
                    priority: item.priority,
                    dueDate: item.dueDate,
                    category: item.category
                })
            });

            if (!res.ok) throw new Error('Update failed');
            const updated = await res.json();
            const idx = todos.findIndex(t => t.id === id);
            if (idx !== -1) {
                todos[idx] = updated;
            }
            showToast('Task updated', 'success');
        } catch (err) {
            // Rollback
            item.title = originalTitle;
            titleEl.textContent = originalTitle;
            showToast('Could not save update', 'error');
        }
    }

    async function handleDeleteTodo(id) {
        const idx = todos.findIndex(t => t.id === id);
        if (idx === -1) return;

        const removedItem = todos[idx];

        // Optimistic remove
        todos.splice(idx, 1);
        renderTodoList();
        updateProgressAndStats();

        try {
            const res = await fetch(`/api/todos/${id}`, {
                method: 'DELETE'
            });

            if (!res.ok) throw new Error('Delete failed');
            showToast('Task deleted', 'success');
        } catch (err) {
            // Rollback
            todos.splice(idx, 0, removedItem);
            sortTodosInPlace();
            renderTodoList();
            updateProgressAndStats();
            showToast('Could not delete task', 'error');
        }
    }

    async function handleClearCompleted() {
        const completedCount = todos.filter(t => t.completed).length;
        if (completedCount === 0) return;

        const originalTodos = [...todos];

        // Optimistic filter
        todos = todos.filter(t => !t.completed);
        renderTodoList();
        updateProgressAndStats();

        try {
            const res = await fetch('/api/todos/completed', {
                method: 'DELETE'
            });

            if (!res.ok) throw new Error('Clear completed failed');
            showToast(`Cleared ${completedCount} completed task(s)`, 'success');
        } catch (err) {
            // Rollback
            todos = originalTodos;
            renderTodoList();
            updateProgressAndStats();
            showToast('Could not clear completed tasks', 'error');
        }
    }

    // =========================================================================
    // Sorting & Rendering
    // =========================================================================

    function sortTodosInPlace() {
        const active = todos.filter(t => !t.completed);
        const completed = todos.filter(t => t.completed);

        const priorityWeights = { HIGH: 1, MEDIUM: 2, LOW: 3 };

        active.sort((a, b) => {
            const pA = priorityWeights[a.priority?.toUpperCase()] || 2;
            const pB = priorityWeights[b.priority?.toUpperCase()] || 2;
            if (pA !== pB) return pA - pB;

            if (a.dueDate && b.dueDate) {
                const c = a.dueDate.localeCompare(b.dueDate);
                if (c !== 0) return c;
            } else if (a.dueDate) {
                return -1;
            } else if (b.dueDate) {
                return 1;
            }

            return (b.createdAt || 0) - (a.createdAt || 0);
        });

        completed.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));

        todos = [...active, ...completed];
    }

    function getFilteredTodos() {
        return todos.filter(item => {
            // Filter pill match
            if (currentFilter === 'active' && item.completed) return false;
            if (currentFilter === 'completed' && !item.completed) return false;
            if (currentFilter === 'high' && item.priority !== 'HIGH') return false;

            // Search query match
            if (searchQuery) {
                const titleMatch = item.title?.toLowerCase().includes(searchQuery);
                const categoryMatch = item.category?.toLowerCase().includes(searchQuery);
                if (!titleMatch && !categoryMatch) return false;
            }

            return true;
        });
    }

    function renderTodoList() {
        const filtered = getFilteredTodos();

        todoListEl.innerHTML = '';

        if (filtered.length === 0) {
            emptyStateEl.classList.remove('hidden');
        } else {
            emptyStateEl.classList.add('hidden');
            filtered.forEach(item => {
                const li = createTodoRowElement(item);
                todoListEl.appendChild(li);
            });
        }
    }

    function createTodoRowElement(item) {
        const li = document.createElement('li');
        li.className = 'todo-item' + (item.completed ? ' completed' : '');
        li.dataset.id = item.id;

        // Custom checkbox
        const checkbox = document.createElement('div');
        checkbox.className = 'custom-checkbox';
        checkbox.setAttribute('role', 'checkbox');
        checkbox.setAttribute('aria-checked', item.completed ? 'true' : 'false');
        checkbox.setAttribute('tabindex', '0');
        checkbox.innerHTML = `
            <svg class="checkbox-check-icon" viewBox="0 0 24 24">
                <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
        `;
        checkbox.addEventListener('click', (e) => {
            e.stopPropagation();
            handleToggleTodo(item.id);
        });
        checkbox.addEventListener('keydown', (e) => {
            if (e.key === ' ' || e.key === 'Enter') {
                e.preventDefault();
                handleToggleTodo(item.id);
            }
        });

        // Details container
        const details = document.createElement('div');
        details.className = 'task-details';

        // Title element
        const titleSpan = document.createElement('span');
        titleSpan.className = 'task-title';
        titleSpan.textContent = item.title;
        titleSpan.title = 'Double-click to edit';

        // Double click to inline edit
        titleSpan.addEventListener('dblclick', () => {
            startInlineEdit(item.id, titleSpan);
        });

        details.appendChild(titleSpan);

        // Metadata badges
        const meta = document.createElement('div');
        meta.className = 'task-meta';

        // Priority badge
        const priority = item.priority || 'MEDIUM';
        const priorityBadge = document.createElement('span');
        priorityBadge.className = `badge badge-priority-${priority.toLowerCase()}`;
        priorityBadge.textContent = priority;
        meta.appendChild(priorityBadge);

        // Due date badge
        if (item.dueDate) {
            const dateBadge = document.createElement('span');
            dateBadge.className = 'badge badge-date';
            const dateStatus = getDueDateStatus(item.dueDate);
            if (dateStatus.className) {
                dateBadge.classList.add(dateStatus.className);
            }
            dateBadge.textContent = dateStatus.label;
            meta.appendChild(dateBadge);
        }

        // Category badge
        if (item.category) {
            const catBadge = document.createElement('span');
            catBadge.className = 'badge badge-category';
            catBadge.textContent = '#' + item.category;
            meta.appendChild(catBadge);
        }

        details.appendChild(meta);

        // Actions (Delete button)
        const actions = document.createElement('div');
        actions.className = 'task-actions';

        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn-icon delete-btn';
        deleteBtn.setAttribute('aria-label', 'Delete task');
        deleteBtn.title = 'Delete task';
        deleteBtn.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                <line x1="10" y1="11" x2="10" y2="17"></line>
                <line x1="14" y1="11" x2="14" y2="17"></line>
            </svg>
        `;
        deleteBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            handleDeleteTodo(item.id);
        });

        actions.appendChild(deleteBtn);

        li.appendChild(checkbox);
        li.appendChild(details);
        li.appendChild(actions);

        return li;
    }

    function startInlineEdit(id, titleEl) {
        const originalText = titleEl.textContent;
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'inline-edit-input';
        input.value = originalText;
        input.maxLength = 250;

        let finished = false;

        function commit() {
            if (finished) return;
            finished = true;
            handleUpdateTitle(id, input.value, originalText, titleEl);
        }

        function cancel() {
            if (finished) return;
            finished = true;
            titleEl.textContent = originalText;
        }

        input.addEventListener('blur', commit);
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                input.blur();
            } else if (e.key === 'Escape') {
                e.preventDefault();
                cancel();
            }
        });

        titleEl.textContent = '';
        titleEl.appendChild(input);
        input.focus();
        input.select();
    }

    function getDueDateStatus(dueDateStr) {
        const today = new Date().toISOString().split('T')[0];
        if (dueDateStr < today) {
            return { label: 'Overdue: ' + dueDateStr, className: 'overdue' };
        } else if (dueDateStr === today) {
            return { label: 'Due Today', className: 'today' };
        } else {
            return { label: 'Due: ' + dueDateStr, className: '' };
        }
    }

    function updateProgressAndStats() {
        const total = todos.length;
        const completed = todos.filter(t => t.completed).length;
        const active = total - completed;
        const percentage = total === 0 ? 0 : Math.round((completed / total) * 100);

        if (progressBarFill) {
            progressBarFill.style.width = percentage + '%';
        }
        if (progressPercentageLabel) {
            progressPercentageLabel.textContent = `${percentage}% completed`;
        }
        if (activeTasksLabel) {
            activeTasksLabel.textContent = `${active} active task${active === 1 ? '' : 's'}`;
        }
        if (statsPill) {
            statsPill.textContent = `${completed} of ${total} done`;
        }
    }

    function showToast(message, type = 'info') {
        if (!toastContainer) return;

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        const iconSvg = type === 'error'
            ? `<svg class="toast-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`
            : `<svg class="toast-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>`;

        toast.innerHTML = `${iconSvg}<span>${escapeHtml(message)}</span>`;
        toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(8px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }
})();
