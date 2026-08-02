# SkillSphere free dual-database deployment

This repository is prepared for two independent portfolio deployments from one Git commit:

| Stack | Frontend | Backend | Database |
| --- | --- | --- | --- |
| MySQL-compatible | Vercel | Render | TiDB Cloud Starter |
| PostgreSQL | Vercel | Render | Neon Free |

Cloudinary stores profile and project images for both stacks. The two databases are independent;
data entered in one is not copied to the other.

## What to have open

Keep these signed-in tabs available during initial setup:

1. GitHub, on the account that owns the repository.
2. Vercel, connected to that GitHub account.
3. Render.
4. TiDB Cloud.
5. Neon.
6. Cloudinary.
7. Google Cloud Console if Google sign-in will be enabled.
8. Your email and authenticator for verification prompts.

You do not need IntelliJ, local MySQL, the local Spring Boot server, or the Vite development
server after cloud deployment.

## Values to prepare privately

Never commit or send these values in chat:

- TiDB host, database, username, and password.
- Neon host, database, role, and password.
- Cloudinary API secret or `CLOUDINARY_URL`.
- Google OAuth client secret.
- The initial administrator password.

Render can generate `JWT_SECRET` from `render.yaml`. If configuring services manually, generate a
different Base64 secret for each service with `openssl rand -base64 48`.

## 1. Push the deployment-ready commit

Both Vercel and Render deploy from GitHub. Confirm the deployment files are committed and push
`main` before importing the repository.

## 2. Create Cloudinary storage

1. Create or open a Cloudinary account.
2. Copy the account's `CLOUDINARY_URL` from its API environment-variable section.
3. Store it only as a secret environment variable in each Render service.
4. Use separate folders such as `skillsphere/tidb` and `skillsphere/neon`.

## 3. Create TiDB Cloud Starter

1. Create a Starter cluster in the preferred nearby region.
2. Create or select a database for SkillSphere.
3. Create a password and allow the deployment connection according to the TiDB console guidance.
4. Choose Java in the Connect dialog and copy the JDBC values.
5. Preserve the TLS options supplied by TiDB.

Render TiDB variables:

```text
SPRING_PROFILES_ACTIVE=tidb
DB_URL=jdbc:mysql://<host>:4000/<database>?sslMode=VERIFY_IDENTITY
DB_USERNAME=<console username>
DB_PASSWORD=<secret>
```

Use the exact URL and parameters shown by the TiDB console instead of inventing them from the
example.

## 4. Create Neon PostgreSQL

1. Create a Neon project and database.
2. Open Connect and choose Java/JDBC. Use the direct hostname while Hibernate manages the schema
   with `ddl-auto=update`; pooled URLs are better introduced after moving schema changes to a
   separate migration step.
3. Convert/copy the value as a JDBC URL if the console does not already show one.
4. Keep TLS enabled.

Render Neon variables:

```text
SPRING_PROFILES_ACTIVE=neon
DB_URL=jdbc:postgresql://<direct-host>/<database>?sslmode=require&channelBinding=require
DB_USERNAME=<role>
DB_PASSWORD=<secret>
```

## 5. Create both Render services

The root `render.yaml` defines:

- `skillsphere-api-tidb-utkarshk08`
- `skillsphere-api-neon-utkarshk08`

In Render, create a Blueprint from this repository. Supply each service's database secrets,
and `CLOUDINARY_URL`. The non-secret configuration is already declared in the Blueprint.
Google OAuth and administrator seed variables are intentionally omitted so an empty optional
value cannot block the first startup. Add them manually in Render only when ready.

Verify each service:

```text
https://<service>.onrender.com/actuator/health
https://<service>.onrender.com/swagger-ui.html
```

The first request to a sleeping free service can be slow. Wait for it to wake before diagnosing a
frontend timeout.

## 6. Create both Vercel projects

Import the same GitHub repository twice. For each project:

1. Set Root Directory to `frontend`.
2. Keep the Vite framework preset.
3. Use Node.js 22.
4. Add the required API variable. Google OAuth variables are optional until Google credentials
   have been configured.

TiDB frontend:

```text
VITE_API_BASE_URL=https://skillsphere-api-tidb-utkarshk08.onrender.com/api
```

Neon frontend:

```text
VITE_API_BASE_URL=https://skillsphere-api-neon-utkarshk08.onrender.com/api
```

If a provider changes either generated hostname, update the matching variables and redeploy.

## 7. Configure Google OAuth

Google OAuth is optional for the first deployment. Password registration and login work without
it. To enable it, add both exact Render callback URLs as authorized redirect URIs:

```text
https://skillsphere-api-tidb-utkarshk08.onrender.com/login/oauth2/code/google
https://skillsphere-api-neon-utkarshk08.onrender.com/login/oauth2/code/google
```

Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in both Render services. Each backend already
redirects success to its matching Vercel `/oauth2/callback` URL.

For each matching Vercel project, also set:

```text
VITE_GOOGLE_OAUTH_ENABLED=true
VITE_OAUTH_LOGIN_URL=https://<matching-render-service>.onrender.com/oauth2/authorization/google
```

Until those values and the backend credentials are present, the frontend keeps the Google button
hidden so it does not advertise an unavailable login method.

## 8. Acceptance checklist

Test both stacks separately:

- Health endpoint reports `UP`.
- Swagger UI opens after the service wakes.
- Vercel home page loads with no mixed-content error.
- Directly opening a nested React route does not return 404.
- Registration succeeds.
- Login returns a token and opens the dashboard.
- Create/edit/read/delete works for one main feature.
- Profile image upload survives a Render restart.
- Project image URL is a secure Cloudinary URL.
- Data created in TiDB is absent from Neon, proving separate databases.
- Google sign-in returns to the correct Vercel site, if enabled.
- Browser console has no CORS errors.

## Interview explanation

> I deployed the same SkillSphere Git commit as two independent stacks. One Spring Boot service
> uses MySQL Connector/J with TiDB Cloud, and the other uses pgJDBC with Neon PostgreSQL. Spring
> Data JPA kept my controller, service, entity, and repository code the same; runtime profiles,
> JDBC URLs, credentials, and drivers select the database. I still tested both because JPA reduces
> vendor coupling but does not eliminate differences in collations, SQL dialects, identity
> generation, and schema migration.

Vite environment variables are build-time values, so two Vercel projects are used to point the
same frontend source at the two backend URLs. `ddl-auto=update` is acceptable for this empty
portfolio demo; a production version should use Flyway or Liquibase and set schema validation.

## Common failures

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| Render cannot connect to TiDB | Wrong TLS URL or network setting | Copy the Java/JDBC connection values again from TiDB |
| Neon reports no suitable driver | Wrong profile or non-JDBC URL | Use `neon` and a `jdbc:postgresql://` URL |
| Frontend calls localhost | Vite variable missing at build time | Set both `VITE_*` values and redeploy Vercel |
| Browser reports CORS blocked | Render `FRONTEND_URL` differs from Vercel origin | Use the exact origin without a trailing path |
| Uploaded images disappear | Local disk storage is active | Set `FILE_STORAGE=cloudinary` and `CLOUDINARY_URL` |
| OAuth redirect mismatch | Google callback is not exact | Register both Render `/login/oauth2/code/google` URLs |
| First request times out | Free Render service is waking | Wait and retry the health endpoint |

## Official references

- Render documentation: <https://render.com/docs/free>
- Render Blueprint specification: <https://render.com/docs/blueprint-spec>
- Vercel Vite documentation: <https://vercel.com/docs/frameworks/frontend/vite>
- Vercel monorepo documentation: <https://vercel.com/docs/monorepos>
- TiDB Cloud connection guide: <https://docs.pingcap.com/tidbcloud/connect-to-tidb-cluster-serverless/>
- Neon connection guide: <https://neon.com/docs/connect/connect-from-any-app>
- Neon connection pooling: <https://neon.com/docs/connect/connection-pooling>
- Cloudinary Java SDK: <https://cloudinary.com/documentation/java_integration>
- Spring Boot external configuration: <https://docs.spring.io/spring-boot/reference/features/external-config.html>
