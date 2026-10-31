# Chinook API

CST-339 Spring Boot application with a REST API for Chinook albums and a Thymeleaf interface for user management.

## Requirements

- Java 21
- PostgreSQL database containing the Chinook tables
- The `users` table from `sql_scripts/users.sql`

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `DB_PASSWORD` | Yes | Database password |
| `ADMIN_INITIAL_PASSWORD` | Only to create the first administrator | Temporary password for the bootstrap `admin` account |
| `PORT` | No | Server port, default `8080` |

Do not commit real passwords.

## Local startup

1. Run `sql_scripts/users.sql` against the database.
2. Set the environment variables above.
3. Start the application:

   ```
   .\mvnw.cmd spring-boot:run
   ```

4. Open http://localhost:8080.

If no administrator exists at startup and `ADMIN_INITIAL_PASSWORD` is set, the application creates an `admin` account that must change its password at first login.

## Running tests

```
.\mvnw.cmd test
```

Tests connect to the configured database, so `DB_PASSWORD` must be set.

## Pages

| Path | Purpose |
|---|---|
| `/` | Landing page |
| `/register` | User registration |
| `/login` | Credential check |
| `/change-password` | Password change |
| `/admin/users` | User administration |
| `/swagger-ui.html` | REST API documentation |

## Security notice

This version is intentionally insecure. Passwords are stored as plain text, roles are not enforced, and `/admin/users` is reachable without logging in. Do not deploy this version publicly or use real passwords. Lab 5 adds Spring Security and BCrypt.
