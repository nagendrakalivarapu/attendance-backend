# Smart Attendance Backend

Spring Boot REST API for the Smart Attendance application. The project targets Java 21 and uses Maven.

## Build and run

Configure the required environment variables below, then run:

```sh
mvn -DskipTests package
mvn spring-boot:run
```

The API listens on port `8080` by default. The database URL, username, frontend origin, and face service URL also have local development defaults in `application.properties`.

## Required configuration

Set these variables in the runtime environment. Do not commit their values:

- `DB_PASSWORD` — password for the configured MySQL account.
- `JWT_SECRET` — random secret of at least 32 bytes.
- `FACE_API_KEY` — key shared with the face recognition service; at least 32 characters.
- `ADMIN_EMAIL` and `ADMIN_PASSWORD` — initial administrator account. The password must contain at least 12 characters.

Optional overrides include `DB_URL`, `DB_USERNAME`, `SERVER_PORT`, `FRONTEND_URL`, `FACE_API_URL`, and `ADMIN_NAME`. Email reports also require `MAIL_USERNAME` and `MAIL_PASSWORD`.

For production, activate the `prod` profile or set `COOKIE_SECURE=true` so authentication cookies are only sent over HTTPS. Set `COOKIE_SAME_SITE=None` only when the frontend and API are deployed on different sites, and keep secure cookies enabled in that case.

The initial administrator is created only when the configured email is not already present. Changing `ADMIN_PASSWORD` does not change an existing account's password.

## Security note

The supplied source archive contained credentials in application configuration. They have been removed from this copy and replaced with environment variables. Rotate any corresponding database, mail, face-service, administrator, or JWT credentials that were active.
