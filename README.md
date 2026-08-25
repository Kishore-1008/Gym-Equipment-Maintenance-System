# Gym Equipment Maintenance System

Full-stack application for managing gym equipment, usage, repair requests, maintenance schedules, and repair history.

```text
HTML + CSS + JavaScript
        |
Spring Boot REST API  (backend/)
        |
Spring Data JPA
        |
MySQL Database
```

## Project Layout

```text
backend/     Java 17 / Spring Boot / Maven — REST API, authentication, business logic, MySQL access
frontend/    Static HTML/CSS/JavaScript site calling the REST API
```

## Features

### Module 1 — Authentication & User Management

- User registration and login against MySQL
- BCrypt password hashing
- JWT-based authentication
- Role-based access control enforced by the backend
- Roles: Admin, Gym Manager, Technician
- Technician IDs generated automatically:

```text
TECH001, TECH002, TECH003, ...
```

### Module 2 — Equipment Management

Admin can:

- Add equipment
- View equipment
- Update equipment
- Delete equipment
- Search and filter equipment

Additional features:

- Backend-generated equipment IDs:

```text
EQ001, EQ002, EQ003, ...
```

- Equipment name-to-category mapping enforced by the backend
- Equipment status tracking
- Maintenance interval selection

### Module 3 — Usage Monitoring

Tracks equipment usage per day.

Admin can:

- View current equipment usage
- View usage history
- View usage summaries and statistics
- Identify most and least used equipment
- Monitor highly used equipment
- Filter usage records
- Delete incorrect usage records

Gym Manager can:

- View equipment usage
- Record daily usage
- Update usage records
- View usage history and statistics
- Use batch usage logging

The system supports daily usage tracking and displays equipment with no usage for the selected day as zero usage rather than omitting it.

### Module 4 — Repair Request Management

Gym Manager can:

- Report equipment problems
- Select the affected equipment
- View submitted repair requests
- Track repair request status
- View rejection reasons when a request is rejected

Admin can:

- View all repair requests
- Approve or reject requests
- Provide a rejection reason when rejecting a request
- Assign a technician after approval
- Monitor repair progress
- Change equipment status when necessary

Repair workflow includes:

```text
PENDING
APPROVED
REJECTED
ASSIGNED
IN_PROGRESS
COMPLETED
```

Equipment can be marked unavailable when a serious issue is reported. Equipment status supports states such as:

```text
OPERATIONAL
MAINTENANCE_DUE
UNDER_MAINTENANCE
UNDER_REPAIR
OUT_OF_SERVICE
```

### Module 5 — Maintenance Management

Admin can:

- Schedule maintenance
- Select maintenance type
- Assign a technician
- View scheduled maintenance
- Monitor maintenance status

Maintenance types include:

```text
ROUTINE_MAINTENANCE
PREVENTIVE_MAINTENANCE
INSPECTION
CLEANING
```

Technician can:

- View assigned maintenance tasks
- Update maintenance status
- Add completion details
- Track scheduled and completed maintenance work

Maintenance statuses include:

```text
SCHEDULED
IN_PROGRESS
COMPLETED
CANCELLED
```

### Module 6 — Repair History

Technician can record completed repair details, including:

- Work performed
- Parts used
- Repair cost
- Completion notes

Admin can:

- View complete repair history
- Review repair details for equipment
- Track parts used and repair costs
- View completion information for completed repairs

## No Default Account

There is **no seeded admin account** and **no hardcoded credentials**.

Create the first account through the **Create Account** page.

---

# How to Run the Application

The application has three parts involved in normal operation:

1. **MySQL database** — must be running.
2. **Spring Boot backend** — runs from `backend/` on port `8080`.
3. **Static frontend** — served locally, normally on port `5500`.

> **Important:** Do not open `login.html` directly using a `file://` URL. Serve the frontend through HTTP, for example using VS Code Live Server.

## 1. Open the Project

Open the project folder in VS Code:

```text
Gym-Equipment-Maintenance-System/
```

Project structure:

```text
Gym-Equipment-Maintenance-System/
├── backend/
└── frontend/
```

The Maven project is inside:

```text
Gym-Equipment-Maintenance-System/backend/
```

Therefore, Maven commands must be executed from the `backend` directory.

## 2. Start MySQL

Make sure your local MySQL server is running.

Open:

```text
backend/src/main/resources/application.properties
```

Configure the datasource credentials for your local MySQL installation.

The database schema is:

```text
gym_ams
```

The application uses Hibernate schema updates, so supported tables and schema changes are created or updated when the backend starts.

## 3. Configure the Database Password

Before starting the backend, set your database password in PowerShell:

```powershell
$env:DB_PASSWORD="your_actual_mysql_password"
```

## 4. Check Whether Port 8080 Is Already in Use

Check port 8080:

```powershell
netstat -ano | findstr :8080
```

Example:

```text
TCP    0.0.0.0:8080    0.0.0.0:0    LISTENING    12345
```

The last number is the PID.

Identify the process:

```powershell
tasklist /FI "PID eq 12345"
```

If it is an old backend process that should be stopped:

```powershell
taskkill /PID 12345 /F
```

Verify that the port is free:

```powershell
netstat -ano | findstr :8080
```

> If the backend was started in an open terminal using `mvn spring-boot:run` or `mvnd spring-boot:run`, press `Ctrl+C` in that terminal instead.

## 5. Start the Backend

Open PowerShell in the `backend` directory:

```powershell
cd "C:\Users\kisho\OneDrive\Desktop\Documents\GitHub\sdp\Gym-Equipment-Maintenance-System\backend"
```

Verify that `pom.xml` exists:

```powershell
dir pom.xml
```

Start Spring Boot:

```powershell
mvn spring-boot:run
```

Or:

```powershell
mvnd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

Keep this terminal running while using the application.

## 6. Start the Frontend

The frontend must be served through a local HTTP server.

### Option A — VS Code Live Server

Right-click:

```text
frontend/login.html
```

Select:

```text
Open with Live Server
```

The frontend will normally open at:

```text
http://127.0.0.1:5500/login.html
```

or:

```text
http://localhost:5500/login.html
```

### Option B — http-server

From the project root:

```powershell
npx http-server frontend -p 5500
```

Then open:

```text
http://localhost:5500/login.html
```

## 7. If Port 5500 Is Already in Use

Check the port:

```powershell
netstat -ano | findstr :5500
```

Identify the process:

```powershell
tasklist /FI "PID eq <PID>"
```

After confirming it is the unwanted process:

```powershell
taskkill /PID <PID> /F
```

If using VS Code Live Server, use its **Stop Server** command instead.

## 8. Verify the Application

Backend:

```text
http://localhost:8080
```

Frontend:

```text
http://localhost:5500/login.html
```

The frontend API configuration is in:

```text
frontend/script.js
```

The API base URL should point to:

```javascript
const API_BASE_URL = "http://localhost:8080/api";
```

If the backend host or port changes, update the frontend API URL and backend CORS configuration accordingly.

## 9. Recommended Startup Sequence

1. Start **MySQL**.
2. Configure the database password if required.
3. Check whether port **8080** is occupied.
4. Stop an old backend process if necessary.
5. Open a terminal inside `backend/`.
6. Run:

```powershell
mvn spring-boot:run
```

or:

```powershell
mvnd spring-boot:run
```

7. Start **Live Server** for `frontend/login.html`, or run another local HTTP server.
8. Open:

```text
http://localhost:5500/login.html
```

---

# How to Test the Implemented Modules

## Authentication and Roles

- Create Admin, Gym Manager, and Technician accounts.
- Verify that each role is redirected to the correct dashboard.
- Verify that role-specific actions are restricted by the backend.

## Equipment Management

As Admin:

1. Add equipment.
2. Verify automatic Equipment ID generation.
3. Search and filter equipment.
4. Update equipment details.
5. Delete equipment when appropriate.

## Usage Monitoring

As Gym Manager:

1. Open Usage Monitoring.
2. Enter daily usage.
3. Use Batch Usage Logging where available.
4. Save the records.

As Admin:

1. Verify usage summaries.
2. View usage history.
3. Check most and least used equipment.
4. Delete an incorrect usage record if required.

## Repair Request Management

As Gym Manager:

1. Select equipment with a problem.
2. Submit a repair request.
3. View the submitted request.
4. Track its status.

As Admin:

1. View the request.
2. Approve or reject it.
3. Enter a rejection reason when rejecting.
4. Assign a technician to an approved request.
5. Monitor repair progress and update equipment availability when necessary.

## Maintenance Management

As Admin:

1. Schedule maintenance.
2. Select a maintenance type.
3. Assign a technician.
4. View the maintenance schedule.

As Technician:

1. View assigned maintenance.
2. Update the maintenance status.
3. Add completion details.
4. Mark the task as completed.

## Repair History

After completing a repair, verify that repair history can record and display:

- Work performed
- Parts used
- Repair cost
- Completion notes

As Admin, verify that completed repair information can be viewed.

---

# Technologies Used

## Backend

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT Authentication
- BCrypt Password Hashing
- MySQL
- Maven

## Frontend

- HTML
- CSS
- JavaScript
- Bootstrap 5

---

## Current Project Progress

Completed modules:

- Module 1 — Authentication & User Management
- Module 2 — Equipment Management
- Module 3 — Usage Monitoring
- Module 4 — Repair Request Management
- Module 5 — Maintenance Management
- Module 6 — Repair History

Planned modules:

- Module 7 — Warranty Management
- Module 8 — Dashboard & Reports

> This project is currently under development and additional functionality will be added in future modules.
