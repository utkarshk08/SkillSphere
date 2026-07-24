# SkillSphere backend

This Maven/Spring Boot 3.5 application uses Java 21, MySQL, JWT authentication, Google OAuth2,
Swagger/OpenAPI, and local file serving. It is intentionally a single, layered application:
controllers delegate to services, services use repositories, and JPA stores data in MySQL.

## Run locally

1. Install Java 21, Maven, and a running MySQL server.
2. Create `skill_sphere_db` yourself, or leave `createDatabaseIfNotExist=true` in the default JDBC URL.
3. Set the environment variables in [`.env.example`](.env.example), especially `JWT_SECRET`.
   For example, generate one with `openssl rand -base64 48` and export it before starting the
   application. The default database values are `root` / `root` as requested.
4. From this directory, run:

   ```bash
   mvn spring-boot:run
   ```

5. Start the Vite frontend at `http://localhost:5173`.

Swagger UI is available at `http://localhost:8080/swagger-ui.html` and the OpenAPI JSON at
`http://localhost:8080/api-docs`.

## Initial admin account

On the first application start, `CommandLineRunner` checks whether a `ROLE_ADMIN` exists. If not,
it creates the requested initial account:

- Email: `admin@skillsphere.com`
- Password: `Admin@12345`
- Role: `ROLE_ADMIN`

Change this password after its first use. There is intentionally no public admin-registration API.

## Google OAuth setup

1. In [Google Cloud Console](https://console.cloud.google.com/), create/select a project.
2. Configure the OAuth consent screen, then create an **OAuth 2.0 Client ID** of type **Web application**.
3. Add this authorized redirect URI exactly:

   ```text
   http://localhost:8080/login/oauth2/code/google
   ```

4. Set the provided client ID and client secret as `GOOGLE_CLIENT_ID` and
   `GOOGLE_CLIENT_SECRET`. Do not place real credentials in source control.

The frontend opens `/oauth2/authorization/google`. On success, the backend creates/reuses the local
user, generates a 30-minute JWT, and redirects to
`http://localhost:5173/oauth2/callback?token=...` by default. Change
`FRONTEND_OAUTH_REDIRECT_URL` if the frontend address changes.

For this intentionally simple student project, the OAuth redirect carries the short-lived token in
the query string so React can store it without a server-side session. This is easy to explain but
is not ideal for a production deployment because URLs can be retained in browser history or logs;
a production system would use a more protected handoff mechanism.

## Authentication endpoints

- `POST /api/auth/register` — create a local student account and receive a JWT.
- `POST /api/auth/login` — authenticate with email/password and receive a JWT.
- `GET /api/auth/me` — return the current user; send `Authorization: Bearer <token>`.

Passwords must have at least eight characters with uppercase, lowercase, numeric, and special
characters. JWTs contain username, email, role, issued time, and expiry time, expire after 30
minutes, and have no refresh-token flow.
