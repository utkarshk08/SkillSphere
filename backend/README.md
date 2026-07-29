# SkillSphere backend

This Maven/Spring Boot 3.5 application uses Java 21, JPA/Hibernate, JWT authentication, Google
OAuth2, Swagger/OpenAPI, and configurable image storage. The same executable supports local MySQL,
TiDB Cloud through MySQL Connector/J, and Neon PostgreSQL through pgJDBC.

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

Administrator creation is disabled by default. To seed one, set all three variables before the
first start:

```text
ADMIN_USERNAME=admin
ADMIN_EMAIL=your-private-admin-email
ADMIN_INITIAL_PASSWORD=a-strong-unique-password
```

`CommandLineRunner` creates the account only when no `ROLE_ADMIN` exists. Remove the initial
password environment variable after confirming the account. There is intentionally no public
admin-registration API.

## TiDB and Neon runtime profiles

Build one executable:

```bash
mvn clean package
```

Use one of these configurations at runtime:

| Target | Profile | JDBC URL |
| --- | --- | --- |
| TiDB Cloud Starter | `tidb` | Exact `jdbc:mysql://...` TLS URL from the TiDB console |
| Neon PostgreSQL | `neon` | `jdbc:postgresql://...?...sslmode=require` URL |

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `SPRING_PROFILES_ACTIVE`. Both JDBC drivers are
included in the same JAR; controllers, services, entities, and repositories remain unchanged.

## Image storage

- Local development: `FILE_STORAGE=local` and `FILE_UPLOAD_DIR=uploads`.
- Render: `FILE_STORAGE=cloudinary` and a secret `CLOUDINARY_URL`.

Render's free filesystem is ephemeral, so local uploads are unsuitable for a hosted demo. With
Cloudinary enabled, the database stores secure HTTPS image URLs. The frontend already accepts
both local upload paths and absolute HTTPS URLs.

## Container and health check

The included `Dockerfile` builds the application with Java 21 and runs it as an unprivileged user.
Render should use `/actuator/health` as its health-check path. The application reads Render's
`PORT` environment variable automatically.

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
