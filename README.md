# 🏟️ Stadium Booking System

A robust, backend RESTful API built with **Spring Boot** to manage sports stadium bookings. This system handles user management, stadium administration, and complex booking logic with **automatic conflict detection** to prevent overlapping reservations.

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-brightgreen?style=for-the-badge&logo=spring)
![MySQL](https://img.shields.io/badge/Database-MySQL-blue?style=for-the-badge&logo=mysql)
![Hibernate](https://img.shields.io/badge/ORM-Hibernate-red?style=for-the-badge)

> **🚧 Current Status: Phase 1 Completed**
>
> Please note that this version focuses on demonstrating **Core Business Logic**, **Complex Conflict Handling**, and **REST API Standards**.
>
> The **Security Layer** (Spring Security, JWT, Password Encryption) is currently **Under Development** and will be implemented in the next major update.
## 🚀 Key Features

* **User Management:** Register, update profile, and manage user data securely.
* **Stadium Management:** Administer stadium details, pricing per hour, and ball rental fees.
* **Smart Booking Logic:**
    * Create bookings associated with specific users and stadiums.
    * **Conflict Detection Algorithm:** Automatically rejects any booking request that overlaps with an existing reservation for the same stadium.
    * Dynamic price calculation based on duration and rental fees.
* **Robust Error Handling:** Global Exception Handler for clear API responses (e.g., `409 Conflict`, `404 Not Found`).
* **Clean Architecture:** Usage of **DTO Pattern** (Request/Response records) to separate entities from API layer.

## 🛠️ Tech Stack

* **Core:** Java 17, Spring Boot 3
* **Database:** MySQL
* **Data Access:** Spring Data JPA (Hibernate)
* **Tools:** Maven, Lombok, Postman (for testing)

## 🔌 API Endpoints

### 1. Users (`/api/v1/users`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/` | Register a new user |
| `GET` | `/` | Get all users |
| `GET` | `/{id}` | Get user by ID |
| `PUT` | `/{id}` | Update user details (Body: JSON) |
| `DELETE` | `/{id}` | Delete a user |

### 2. Stadiums (`/api/v1/stadiums`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/` | Add a new stadium |
| `GET` | `/` | List all stadiums |
| `PUT` | `/{id}` | Update stadium info (Body: JSON) |
| `DELETE` | `/{id}` | Delete a stadium |

### 3. Bookings (`/api/v1/bookings`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/` | Create a booking **(Checks for conflicts)** |
| `GET` | `/` | Get all bookings |
| `PUT` | `/{id}` | Update booking time/details |
| `DELETE` | `/{id}` | Cancel a booking |

## 🏗️ Project Structure

The project follows a **Package-by-Feature** structure for better maintainability:

```text
src/main/java/com/hamza/stadiumbooking
├── booking       # Booking Controller, Service, Repository, Entity, DTOs
├── stadium       # Stadium Controller, Service, Repository, Entity, DTOs
├── user          # User Controller, Service, Repository, Entity, DTOs
├── exception     # Global Exception Handling Logic
└── StadiumBookingSystemApplication.java
```
🔜 Future Improvements (Roadmap)
------------------------------------------------------------
[ ] Security Layer: Implementing Spring Security & JWT for authentication and authorization.
[ ] Password Encryption: Using BCrypt to hash passwords.
[ ] Unit Testing: Adding JUnit and Mockito tests.
[ ] Docker: Containerizing the application for easy deployment.


👨‍💻 Author
------------------------------------------------------------
Name:    Hamza Saleh
Profile: [https://github.com/hamzasaleh12](https://github.com/hamzasaleh12)