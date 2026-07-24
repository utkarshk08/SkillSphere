# SkillSphere API examples

Swagger UI is available at `http://localhost:8080/swagger-ui.html` after starting the backend.
Use its **Authorize** button to enter `Bearer <access-token>` for protected endpoints.

All paginated endpoints return Spring's standard page shape:

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "size": 10,
  "number": 0
}
```

## Authentication

`POST /api/auth/register`

```json
{
  "firstName": "Utkarsh",
  "lastName": "Khandelwal",
  "username": "utkarsh_dev",
  "email": "utkarsh@example.com",
  "password": "Utkarsh@123",
  "confirmPassword": "Utkarsh@123",
  "collegeName": "Example Institute of Technology",
  "course": "B.Tech Computer Science",
  "yearOfStudy": "3rd Year",
  "country": "India",
  "bio": "Learning Spring Boot and React."
}
```

Successful registration and `POST /api/auth/login` return:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": { "id": 1, "username": "utkarsh_dev", "role": "ROLE_USER" }
}
```

Start Google OAuth with `GET /oauth2/authorization/google`. The backend redirects to the React
callback with `?token=<jwt>` after Google verifies the identity.

## Profile and skills

`PUT /api/profiles/me`

```json
{
  "firstName": "Utkarsh",
  "lastName": "Khandelwal",
  "username": "utkarsh_dev",
  "email": "utkarsh@example.com",
  "collegeName": "Example Institute of Technology",
  "course": "B.Tech Computer Science",
  "yearOfStudy": "3rd Year",
  "country": "India",
  "bio": "Building REST APIs.",
  "githubUrl": "https://github.com/utkarsh",
  "linkedinUrl": "https://linkedin.com/in/utkarsh",
  "portfolioUrl": "https://example.dev",
  "interests": ["Backend", "Open Source"],
  "publicProfileVisibility": true
}
```

`POST /api/skills`

```json
{
  "name": "Spring Boot",
  "level": "INTERMEDIATE",
  "intent": "TEACH",
  "description": "Building REST APIs.",
  "experienceMonths": 8
}
```

Search public profiles with, for example:

```text
GET /api/profiles?name=utkarsh&college=example&skill=Spring%20Boot&interest=Backend&page=0&size=10
```

Upload a profile picture with multipart field `file`:

```text
POST /api/profiles/me/picture
Content-Type: multipart/form-data
```

## Projects and communities

`POST /api/projects`

```json
{
  "title": "Expense Tracker",
  "description": "A student expense-tracking web application.",
  "githubLink": "https://github.com/example/expense-tracker",
  "techStack": ["React", "Spring Boot", "MySQL"],
  "requiredSkills": ["React", "Spring Boot"],
  "deadline": "2026-08-30",
  "maximumMembers": 4,
  "status": "OPEN",
  "difficultyLevel": "INTERMEDIATE",
  "communityId": null
}
```

Upload each project image after creating a project:

```text
POST /api/projects/{projectId}/images
Content-Type: multipart/form-data
field: file
```

Community creation and editing are admin-only. `POST /api/communities` accepts:

```json
{
  "name": "Spring Boot Community",
  "description": "A place to learn and build with Spring Boot.",
  "resources": ["https://spring.io/guides"]
}
```

Students join with `POST /api/communities/{communityId}/join` and leave with
`DELETE /api/communities/{communityId}/leave`.

## Roadmaps, bookmarks, and collaboration

`POST /api/roadmaps`

```json
{
  "title": "Spring Boot Roadmap",
  "publicVisible": true,
  "items": [
    { "title": "Module 1", "completed": true },
    { "title": "JWT", "completed": false },
    { "title": "OAuth", "completed": false }
  ]
}
```

`POST /api/bookmarks`

```json
{ "targetType": "PROFILE", "targetId": 12 }
```

Use `"COMMUNITY"` to bookmark a community instead.

`POST /api/collaboration-requests`

```json
{ "receiverId": 12, "message": "Need a Spring Boot developer for my project." }
```

The receiver accepts or rejects through `PUT /api/collaboration-requests/{id}`:

```json
{ "status": "ACCEPTED" }
```

## Notifications, reports, and administration

`POST /api/reports` reports exactly one student or one content item:

```json
{
  "reportedUserId": 12,
  "reason": "FAKE_PROFILE",
  "description": "The profile appears to contain inaccurate details."
}
```

For content, replace `reportedUserId` with both `reportedContentType` and `reportedContentId`,
such as `"PROJECT"` and `42`.

Admin report resolution:

```json
PUT /api/admin/reports/{id}
{
  "status": "RESOLVED",
  "adminAction": "WARNED_USER"
}
```

Admin announcement:

```json
POST /api/admin/announcements
{
  "title": "Community Guidelines",
  "message": "Please keep collaboration requests respectful and relevant.",
  "active": true
}
```

The admin endpoints also provide paginated user/report/announcement management, profile
verification, user deletion, and explicit removal of reported `PROFILE`, `PROJECT`, `COMMUNITY`,
or `SKILL` content.
