# FocusTask — Modern TODO Web Application

A cloud-ready, responsive TODO web application engineered with **Java 21 LTS**, **Spring Boot 3**, and **Amazon DynamoDB**, featuring a dynamic **Thymeleaf + Vanilla JavaScript** frontend.

---

## Features
- **Task Management**:
  - Add tasks with priority (`HIGH`, `MEDIUM`, `LOW`), due date, and category tags.
  - **Double-click inline editing** with auto-save on blur/Enter.
  - **Strike-down & sink**: Completed tasks receive animated strikethrough styling and are automatically pinned to the bottom of the list.
- **Smart Active Sorting**:
  - Active items sorted by Priority (`HIGH` > `MEDIUM` > `LOW`), earliest Due Date, and newest created.
- **Real-Time Client Filtering**:
  - Instant client-side search across titles and categories.
  - Filter tabs: `All`, `Active`, `Completed`, `High Priority`.
- **Cloud-Native AWS Persistence**:
  - Backed by **Amazon DynamoDB** with On-Demand capacity (`PAY_PER_REQUEST`).
  - Automatic **30-day Time-To-Live (TTL)** on completed tasks.
  - Dual-mode repository pattern: automatically falls back to in-memory mode when offline or without AWS credentials so local testing works out of the box.
- **DevOps & IaC**:
  - **AWS CloudFormation** template (`cloudformation/template.yaml`) for automated provisioning of `${Environment}-TodoItems` table & least-privilege IAM roles.
  - **GitHub Actions** CI/CD pipeline (`.github/workflows/ci-cd.yml`) for automated testing, Maven build, and deployment.
  - Multi-stage **Dockerfile** and **docker-compose.yml** for containerized environments.

---

## Running the Application Locally

### Option 1: Native Maven Run (Recommended for fast dev)
```powershell
# From project root
.\mvnw.cmd spring-boot:run
```
Open your browser to: **[http://localhost:8080](http://localhost:8080)**.
> Note: If AWS credentials are not found, the app automatically runs in **Local Dev Mode** with pre-seeded sample tasks.

### Option 2: Docker Compose with DynamoDB Local
```bash
docker compose up --build
```
This spins up:
- DynamoDB Local on port `8000`
- FocusTask application on port `8080`

---

## REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Web application UI |
| `GET` | `/api/todos` | List all todos (smart sorted) |
| `POST` | `/api/todos` | Create a new task |
| `PUT` | `/api/todos/{id}` | Update task title and details |
| `PATCH` | `/api/todos/{id}/toggle` | Toggle completion status |
| `DELETE` | `/api/todos/{id}` | Delete task |
| `DELETE` | `/api/todos/completed` | Clear all completed tasks |
| `GET` | `/api/status` | Application health and storage mode |

---

## AWS Deployment via CloudFormation

Deploy the DynamoDB table and IAM execution role using AWS CLI:
```bash
aws cloudformation deploy \
  --template-file cloudformation/template.yaml \
  --stack-name dev-focustask-stack \
  --parameter-overrides Environment=dev \
  --capabilities CAPABILITY_NAMED_IAM
```
