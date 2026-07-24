# SkillSphere Frontend

This is the separate React + Vite frontend for SkillSphere. It uses JavaScript, Axios, React Router DOM, Context API, and plain CSS only.

## Included pages

- Public home, registration, login, and Google OAuth callback
- Protected dashboard, profile, public student profiles, and student search
- Skills, projects (including image upload), communities, roadmaps, bookmarks, collaboration requests, notifications, and reports
- Role-protected admin dashboard for users, profile verification, reports, skills, and announcements

Each paginated screen reads Spring's standard `Page` response (`content`, `totalPages`, and `totalElements`). All feature API calls are kept in `src/api/platformApi.js` so the frontend/backend contract is easy to follow.

## Run locally

1. Copy the environment example:

   ```bash
   cp .env.example .env
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Start the Vite development server:

   ```bash
   npm run dev
   ```

   The frontend runs at `http://localhost:5173` by default.

4. Start the Spring Boot backend at `http://localhost:8080`. Its CORS configuration must allow the Vite origin.

To verify a production build:

```bash
npm run build
```

## Backend connection

`VITE_API_BASE_URL` defaults to `http://localhost:8080/api`. The Axios client automatically sends the stored JWT on protected requests as:

```text
Authorization: Bearer <access-token>
```

The requested access-token-only authentication flow is used; there are no refresh tokens.

## Google OAuth setup

Google credentials belong only in the Spring Boot backend. Do not put the client secret in this frontend.

1. Create OAuth 2.0 credentials in the [Google Cloud Console](https://console.cloud.google.com/).
2. Add `http://localhost:8080/login/oauth2/code/google` as the authorized redirect URI.
3. Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` for the backend.
4. The backend redirects successful sign-in to `http://localhost:5173/oauth2/callback?token=<JWT>`.

The frontend callback stores the token, and the backend remains responsible for validating the JWT on every protected request.

## Uploads

- Profile pictures: JPG, PNG, or WEBP, up to the backend's configured multipart limit.
- Project images: JPG, PNG, or WEBP; the project screen submits each selected file to the project image upload endpoint after the project is created or updated.
