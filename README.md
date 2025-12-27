---
title: Stadium Booking System
emoji: 🏟️
colorFrom: blue
colorTo: green
sdk: docker
pinned: false
---


# 🏟️ Stadium Booking System (V2 - Production Ready)

A high-performance, secure backend RESTful API built with **Spring Boot 3.4.2** and **Java 21**. This system manages sports stadium reservations with automated conflict detection, ensuring no double-bookings and smooth operations for admins, managers, and players.

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

## 🔗 Live Demo & Docs
- **Base URL (Hugging Face):** https://hamzasaleh-stadium-booking.hf.space/
- **Swagger UI (Live):** https://hamzasaleh-stadium-booking.hf.space/swagger-ui/index.html
- **Health Check:** https://hamzasaleh-stadium-booking.hf.space/actuator/health

## 🚀 Live Demo & Testing
You can test the live API here: [Hugging Face Live API](https://hamzasaleh-stadium-booking.hf.space/)

| Role    | Email               | Password    |
|---------|---------------------|-------------|
| Admin   | admin@gmail.com     | Admin@1234  |
| Manager | manager@gmail.com   | Manager@1234|
| Player  | player@gmail.com    | Player@1234 |

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
| `GET` | `/api/v1/stadiums` | Public | List stadiums (Paginated) |
| `POST` | `/api/v1/stadiums` | `ADMIN / MANAGER` | Add new stadium |
| `POST` | `/api/v1/bookings` | `PLAYER` | Create booking (Conflict Check) |
| `GET` | `/api/v1/bookings/my-bookings` | `PLAYER` | View personal booking history |

## ⚙️ Cloud Stack & Environment Variables
The system is built using a modern cloud-native architecture:
- **Database:** TiDB Serverless (MySQL Compatible)
- **Cache:** Upstash Redis (Serverless)
- **Deployment:** Hugging Face Spaces (Docker)

```properties
PORT=7860
SPRING_DATASOURCE_URL=jdbc:mysql://<tidb-host>:4000/stadium_db
SPRING_REDIS_URL=rediss://default:<password>@<upstash-host>:6379
SPRING_DATA_REDIS_SSL_ENABLED=true
JWT_SECRET=your-secure-jwt-secret
```
## 📦 Local Deployment (Docker)
```bash
docker build -t stadium-booking .
docker run -p 7860:7860 --env-file .env stadium-booking
```

The API will be available at http://localhost:7860.

👨‍💻 Connect with Me

GitHub: https://github.com/hamzasaleh12  
LinkedIn: https://www.linkedin.com/in/hamza-saleh-908662392/

