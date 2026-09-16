# Stay Booking

Backend application for an accommodation booking service built with Spring Boot. The project uses Java 21, Maven, PostgreSQL, Spring Data JPA, Flyway, Spring Security, and Actuator.

## Requirements

- JDK 21
- Docker with Docker Compose

You do not need to install Maven separately because the project includes Maven Wrapper.

## Local Development

1. Start PostgreSQL:

   ```bash
   docker compose up -d --wait postgres
   ```

2. Run the application with the local profile:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

   On Windows, use `mvnw.cmd` instead of `./mvnw`.

The application will be available at `http://localhost:8080`. Its health endpoint is available at `http://localhost:8080/actuator/health`.

The default database connection uses the `staybooking` database, username `user`, password `password`, and port `5432`. You can override these settings with the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables.

Docker must be running for both commands because the test suite uses PostgreSQL Testcontainers.

## Build and Test

```bash
# Run all tests from a clean state
./mvnw clean test

# Run tests and build the executable JAR
./mvnw clean verify

# Build without running tests
./mvnw clean package -DskipTests

# Remove build artifacts
./mvnw clean
```

The packaged JAR is created at `target/stay-booking-0.0.1-SNAPSHOT.jar`. Run it with:

```bash
java -jar target/stay-booking-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Production Profile

The `prod` profile currently configures production-style external
credentials only. The application is not production-ready until the
security implementation is completed.

Database connection variables are required when using the `prod` profile:

```bash
DB_URL='jdbc:postgresql://<db-host>:5432/staybooking' \
DB_USERNAME='<db-user>' \
DB_PASSWORD='<value-from-secret-manager>' \
java -jar target/stay-booking-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Stop the Environment

```bash
docker compose down
```

To also delete the PostgreSQL data, run `docker compose down -v`.
