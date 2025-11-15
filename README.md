# Kumara Enterprises POS System

A modern Point of Sale (POS) system built with **Spring Boot 3.2.0**, **JavaFX 21**, and **JPA/Hibernate**.

## Features

- ✅ Spring Boot with JPA for data persistence
- ✅ JavaFX for modern desktop UI
- ✅ Barcode scanning (CODE 128 format)
- ✅ Customer and Product Management
- ✅ Order Processing
- ✅ Loyalty Card System
- ✅ Password Encryption with BCrypt

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **JavaFX 21.0.1**
- **MySQL**
- **Lombok** (reduces boilerplate)
- **ZXing** (Barcode generation)
- **JFoenix** (Material Design for JavaFX)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+

## Setup

### 1. Database Configuration

Update `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/robotikka?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
```

### 2. Build and Run

#### Using Maven (Recommended)

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or using JavaFX plugin
mvn javafx:run
```

#### Build JAR and Run

```bash
# Build executable JAR
mvn clean package

# Run JAR
java -jar target/pos-1.0.0.jar
```

### 3. Using IDE

#### IntelliJ IDEA / Eclipse / VS Code

1. Import as Maven project
2. Run `PosApplication.java` as main class
3. Spring Boot will auto-configure everything

**Note:** No need to set `JAVAFX_HOME` anymore! Spring Boot and JavaFX Maven plugin handle everything automatically.

## Project Structure

```
src/com/devstack/pos/
├── PosApplication.java          # Spring Boot main class
├── Initialize.java               # JavaFX Application
├── config/                       # Spring configuration
│   └── SecurityConfig.java
├── entity/                       # JPA entities
├── repository/                   # Spring Data JPA repositories
├── service/                      # Spring services (business logic)
├── controller/                   # JavaFX controllers (with @Component)
├── util/                         # Utility classes
└── view/                         # FXML files
```

## Architecture

- **Repository Layer**: Spring Data JPA repositories
- **Service Layer**: Spring services with `@Service` annotation
- **Controller Layer**: JavaFX controllers with `@Component` annotation
- **Entity Layer**: JPA entities (replaces old DAO/BO pattern)

## Features

### User Management
- User registration and authentication
- Password encryption with BCrypt

### Customer Management
- CRUD operations
- Customer search functionality

### Product Management
- Product creation
- Product detail management with barcode generation
- Barcode scanning for POS

### Order Processing
- Cart management
- Order creation with item details
- Order history

### Loyalty Card System
- Automatic card assignment based on customer salary
- Barcode generation for loyalty cards

## Configuration

All configuration is centralized in `src/main/resources/application.properties`:

- Database connection
- JPA/Hibernate settings
- Logging levels

## Development

### Adding a New Feature

1. Create/update JPA entity in `entity/` package
2. Create repository in `repository/` package (extends `JpaRepository`)
3. Create service in `service/` package (annotated with `@Service`)
4. Update/create controller in `controller/` package (annotated with `@Component`)

### Code Style

- Use Lombok (`@Data`, `@RequiredArgsConstructor`, etc.) to reduce boilerplate
- Follow Spring Boot best practices
- Use dependency injection (constructor injection preferred)

## Troubleshooting

### Application won't start

1. Check database connection in `application.properties`
2. Ensure MySQL is running
3. Check logs for Spring Boot startup errors

### JavaFX errors

- Make sure JavaFX dependencies are in `pom.xml`
- Use `mvn javafx:run` or `mvn spring-boot:run`

### Database errors

- Ensure database `robotikka` exists or will be auto-created
- Check database credentials
- Verify MySQL server is running

## License

This project is for educational purposes.
