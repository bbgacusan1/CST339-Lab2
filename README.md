# Chinook API - CST-339 Lab 5 (Spring Security)

A Spring Boot application with a REST API for Chinook albums and a Thymeleaf interface for user registration, login and user administration, secured with Spring Security.

**Live app:** https://cst339-lab2-g206.onrender.com/

**API docs:** https://cst339-lab2-g206.onrender.com/swagger-ui.html

> The app runs on Render's free tier and sleeps after about 15 minutes without traffic. The first request after that can take 30-60 seconds.

## Tech stack

- Java 21, Spring Boot 4.1, Spring Security 7
- Spring Data JPA with PostgreSQL (Neon)
- Thymeleaf with the Spring Security dialect (`thymeleaf-extras-springsecurity6`), Bootstrap 5
- springdoc-openapi 3 (Swagger UI)
- Docker, deployed on Render

## Security features

| Feature | Where | What it does |
|---|---|---|
| `SecurityFilterChain` | `config/SecurityConfig.java` | One place for every access rule (see the table below). |
| Database users | `user/DatabaseUserDetailsService.java` | Spring Security loads accounts, roles and the enabled flag from the `users` table. |
| BCrypt passwords | `PasswordEncoder` bean in `SecurityConfig` | Passwords are hashed on registration, password change and admin bootstrap. Plain text is never stored. |
| Form login | `/login` | Login for the Thymeleaf pages. Failures redirect to `/login?error`, and logout to `/login?logout`. |
| HTTP Basic | `/api/**` | Login for Postman and Swagger. Missing or wrong credentials get `401`. |
| CSRF protection | All browser forms | Forms include a CSRF token. CSRF is ignored only for `/api/**`, which uses HTTP Basic, not cookies. |
| Role-based navbar | `templates/fragments/navbar.html` | `sec:authorize` shows Login/Register to visitors, account links to users, and User Administration only to admins. |
| Swagger security scheme | `config/OpenApiConfig.java` | Adds an **Authorize** button so Swagger UI can call the API with HTTP Basic. |
| Disabled accounts | `users.enabled` | Disabled users cannot log in through the form or HTTP Basic. |

## Access rules

| Route | Anonymous | USER | ADMIN |
|---|---|---|---|
| `/`, `/login`, `/register`, `/swagger-ui.html`, `/v3/api-docs` | Allowed | Allowed | Allowed |
| `/change-password` | Redirect to `/login` | Allowed | Allowed |
| `/admin/users/**` | Redirect to `/login` | `403 Forbidden` | Allowed |
| `GET /api/albums`, `GET /api/albums/{id}` | `401` | `200` | `200` |
| `POST /api/albums` | `401` | `403` | `201` |
| `PUT /api/albums/{id}` | `401` | `403` | `200` |
| `DELETE /api/albums/{id}` | `401` | `403` | `204` |

Any other HTTP method under `/api/**` (for example PATCH) is ADMIN only.

New accounts created through `/register` always get the `USER` role. Admins can change roles at `/admin/users`.

## Using the API

### Postman

Import `postman/Lab_5_Secured_API.postman_collection.json`. In the collection's **Variables** tab:

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8080`, or the Render URL |
| `userUsername` / `userPassword` | A USER account (for example one you registered) |
| `adminUsername` / `adminPassword` | The admin account |

Run the folders in order. Each request has a test that checks the expected status: anonymous gets 401, USER gets 200 on reads and 403 on writes, ADMIN gets full CRUD on an album it creates and then deletes, and a wrong password gets 401.

Do not commit real passwords into the collection.

### Swagger UI

1. Open `/swagger-ui.html`. Use a private window if you are logged in through the form, because the session cookie would otherwise authenticate the requests.
2. Click **Authorize**, enter a username and password, then use **Try it out**.
3. For POST and PUT, use an existing artist, for example:

   ```json
   { "title": "Swagger Test Album", "artistId": 1 }
   ```

   An `artistId` that does not exist returns `409 Conflict`.

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `DB_PASSWORD` | Yes | Neon database password |
| `ADMIN_INITIAL_PASSWORD` | Only to create the first admin | If no admin exists at startup, an `admin` account is created with this password (BCrypt-hashed) and must change it at first login |
| `PORT` | No | Server port, default `8080`. Render sets this automatically |
| `SHOW_SQL` | No | Set to `true` to log SQL statements. Default `false` |

Never commit real passwords.

## Running locally

1. Run `sql_scripts/users.sql` against the database. It creates the `users` table and seeds an `admin` account with a BCrypt hash.
2. Set the environment variables above.
3. Start the application:

   ```
   .\mvnw.cmd spring-boot:run
   ```

4. Open http://localhost:8080.

## Tests

```
.\mvnw.cmd test
```

**129 tests, 0 failures.** The tests connect to the configured database, so `DB_PASSWORD` must be set. Integration tests run in a transaction that is rolled back.

| Test class | Covers |
|---|---|
| `config/SecurityAccessMatrixTests` | 28 route and role combinations from the access rules table |
| `config/SecurityBehaviorIntegrationTests` | Logout, CSRF on forms vs. the API, disabled accounts over HTTP Basic, full CRUD with real Basic credentials, hashed passwords on registration |
| `config/PasswordEncoderTests` | BCrypt hashing, matching and salting |
| `config/OpenApiDocsIntegrationTests` | The Swagger HTTP Basic scheme and documented 401/403 responses |
| `album/AlbumSecurityIntegrationTests` | USER read-only and ADMIN-only writes on the album API |
| `album/AlbumIntegrationTests`, `AlbumServiceTests`, `AlbumRepositoryTests`, `AlbumDtoTests` | Album CRUD behavior |
| `user/LoginIntegrationTests` | Form login success and failure, disabled users, password change |
| `user/RegistrationIntegrationTests`, `UserServiceTests` | Registration rules and encoded password storage |
| `user/UserAdminIntegrationTests` | Admin pages, plus 403 for USER and redirect for anonymous |
| `user/DatabaseUserDetailsServiceTests`, `AdminBootstrapInitializerTests` | Loading users for Spring Security and creating the first admin |
| `home/NavigationIntegrationTests`, `HomePageIntegrationTests` | Navbar links per role and the home page |

## Deploying to Render

The repository includes a multi-stage `Dockerfile`: Maven builds the jar, then a Java 21 JRE image runs it. Tests are skipped during the image build because they need the database.

1. In Render, choose **New > Web Service** and connect this GitHub repository (branch `main`).
2. Select **Docker** as the runtime. Render detects the `Dockerfile`.
3. Add the environment variable `DB_PASSWORD`. Do not set `PORT`. `ADMIN_INITIAL_PASSWORD` is only needed if the database has no admin yet.
4. Set the health check path to `/`.
5. Deploy. Every push to `main` redeploys automatically.

Render terminates HTTPS at its proxy. `server.forward-headers-strategy=framework` makes Spring trust the `X-Forwarded-*` headers, so login redirects and the Swagger server URL use `https`.

## Lab 5 branches and pull requests

| # | Branch | Pull request |
|---|---|---|
| 1 | `bgacusan-spring-security-dependency` | [#23](https://github.com/bbgacusan1/CST339-Lab2/pull/23) Add Spring Security dependency |
| 2 | `bgacusan-security-filter-chain` | [#24](https://github.com/bbgacusan1/CST339-Lab2/pull/24) Configure the SecurityFilterChain |
| 3 | `bgacusan-user-details-service` | [#25](https://github.com/bbgacusan1/CST339-Lab2/pull/25) Load users from the database |
| 4 | `bgacusan-bcrypt-authentication` | [#26](https://github.com/bbgacusan1/CST339-Lab2/pull/26) BCrypt and database authentication |
| 5 | `bgacusan-encode-registration` | [#27](https://github.com/bbgacusan1/CST339-Lab2/pull/27) Encode passwords on registration |
| 6 | `bgacusan-admin-role` | [#28](https://github.com/bbgacusan1/CST339-Lab2/pull/28) Restrict admin pages and role-based navbar |
| 7 | `bgacusan-rest-rules` | [#29](https://github.com/bbgacusan1/CST339-Lab2/pull/29) REST rules by HTTP method |
| 8 | `bgacusan-swagger-security` | [#30](https://github.com/bbgacusan1/CST339-Lab2/pull/30) Swagger HTTP Basic security scheme |
| 9 | `bgacusan-security-tests` | [#31](https://github.com/bbgacusan1/CST339-Lab2/pull/31) Security integration tests |
| 10 | `bgacusan-render-deploy` | #32 Deploy to Render and update README <!-- TODO: confirm the number and link once the PR is open --> |

## AI references

This lab was completed with help from **Claude Code** (Anthropic, Claude Opus model), used in the Claude desktop app.

How it was used:

- **Planning and explanations:** breaking the lab into ten branches and explaining Spring Security concepts (the filter chain, `UserDetailsService`, BCrypt, CSRF, form login vs. HTTP Basic, 401 vs. 403).
- **Code:** in early branches it gave hints and reviewed code I wrote. In later branches it provided or wrote code that I reviewed, ran and tested.
- **Tests:** it wrote and updated most test classes, and diagnosed failures (for example, missing CSRF tokens, missing `Accept: text/html` headers, and a 406 on `/v3/api-docs`).
- **Reviews:** it checked each branch before commit and suggested commit messages and PR descriptions.
- **Documentation:** it drafted this README and the Postman collection.

All code was run, tested manually (browser, Postman and Swagger UI) and committed by me.
