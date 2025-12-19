# 🏟️ Stadium Booking System (V2 - Production Ready)

A high-performance, secure backend RESTful API built with **Spring Boot 3.4.2** and **Java 21**. This system manages sports stadium reservations with automated conflict detection, ensuring no double-bookings and high availability.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.2-brightgreen?style=for-the-badge&logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8-blue?style=for-the-badge&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-Cache-red?style=for-the-badge&logo=redis)
![Docker](https://img.shields.io/badge/Docker-Container-blue?style=for-the-badge&logo=docker)
![JWT](https://img.shields.io/badge/JWT-Secure-black?style=for-the-badge&logo=jsonwebtokens)

## 🌟 Advanced Engineering Features

* **Smart Conflict Detection:** Custom algorithm to prevent overlapping reservations.
* **Optimistic Locking:** Managed via `@Version` for safe high-concurrency requests.
* **Performance:** **Redis caching** for stadium listings and optimized JPA queries.
* **Data Integrity:** **Soft Delete** implementation and strict **Bean Validation**.
* **Unit Testing:** Comprehensive suite using **JUnit 5 & Mockito**.

## 🏗️ Project Structure

The project follows a **Package-by-Feature** architecture for better maintainability:

```text
src/main/java/com/hamza/stadiumbooking
├── auth          # JWT Filters, Security Config, Auth Services
├── booking       # Logic for conflict detection & reservations
├── stadium       # Stadium management & Redis Caching
├── user          # Profile & Role management
└── common        # Global Exception Handling & DTOs
```

## 🔌 API Documentation

### 1. Authentication & Identity
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/login` | Public | Login & Get JWT Token |
| `POST` | `/api/v1/auth/refresh-token` | Public | Get new Access Token |
| `POST` | `/api/v1/users` | Public | Register new Player |

### 2. User Management
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | `ADMIN` | List all users (Paginated) |
| `GET` | `/api/v1/users/{id}` | `ADMIN` or `OWNER` | Get specific user details |
| `PUT` | `/api/v1/users/{id}` | `ADMIN` or `OWNER` | Update profile data |
| `DELETE` | `/api/v1/users/{id}` | `ADMIN` or `OWNER` | Soft delete user |

### 3. Stadiums & Bookings
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/stadiums` | Public | List stadiums (Cached) |
| `POST` | `/api/v1/stadiums` | `ADMIN / MANAGER` | Add new stadium |
| `POST` | `/api/v1/bookings` | `PLAYER` | Create booking (Conflict Check) |
| `GET` | `/api/v1/bookings/my-bookings` | `PLAYER` | View personal booking history |

## ⚙️ Environment Variables
To run this project, you need to configure the following variables:
`MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`, `REDISHOST`, `REDISPORT`, `JWT_SECRET`.

## 📦 Local Deployment (Docker)

To run the entire stack (App, MySQL, Redis) on your machine, ensure you have Docker installed and run:

```bash
# Clone the repository
git clone https://github.com/hamzasaleh12/stadium-booking-system.git

# Enter the directory
cd stadium-booking-system

# Run the entire stack
docker-compose up --build

```

The API will be available at http://localhost:8082.

👨‍💻 Connect with Me

GitHub: https://github.com/hamzasaleh12/stadium-booking-system

LinkedIn: https://www.linkedin.com/in/hamza-saleh-908662392/
