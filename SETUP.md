# Setup Guide

## Quick Start

### 1. Configure Environment Variables

Copy `.env.example` to `.env` and fill in:

```bash
cp .env.example .env
```

**Required values:**
- `NEXTAUTH_SECRET`: Generate with `openssl rand -base64 32`
- `GOOGLE_CLIENT_ID`: From Google Cloud Console
- `GOOGLE_CLIENT_SECRET`: From Google Cloud Console
- `DATABASE_URL`: SQLite connection string (e.g., `file:./dev.db`)
- `DB_PATH`: Path for the Java SQLite file (default `driveclone.db`)
- `JAVA_API_URL`: Usually `http://localhost:8080`

### 2. Google OAuth Setup

1. Go to https://console.cloud.google.com/
2. Create a new project
3. Enable "Google+ API"
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client ID"
5. Application type: Web application
6. Authorized redirect URIs: `http://localhost:3000/api/auth/callback/google`
7. Copy Client ID and Client Secret to `.env`

### 3. Install Dependencies

**Frontend:**
```bash
npm install
```

**Backend:**
```bash
mvn clean install
```

### 4. Initialize SQLite Databases

**Prisma (NextAuth) file DB:**
```bash
npx prisma generate
npx prisma db push
```

**Java Backend:**
SQLite tables (`driveclone.db`) are created automatically on first run.

### 5. Run the Application

**Terminal 1 - Java Backend:**
```bash
mvn exec:java
```

**Terminal 2 - Next.js Frontend:**
```bash
npm run dev
```

### 8. Access the Application

- Frontend: http://localhost:3000
- Java API: http://localhost:8080

## Troubleshooting

### SQLite file issues

1. Delete `dev.db` or `driveclone.db` if they become corrupted and rerun `npx prisma db push` / restart the Java server.
2. Ensure the folders containing these files are writable.

### Java Backend Won't Start

1. Check Java version: `java -version` (should be 17+)
2. Check Maven: `mvn -version`
3. Ensure the directory containing `driveclone.db` is writable
4. Check port 8080 is not in use

### Next.js Build Errors

1. Run `npm install` again
2. Delete `node_modules` and `.next` folders
3. Run `npm install` and `npm run dev`

### Authentication Issues

1. Verify Google OAuth credentials in `.env`
2. Check redirect URI matches exactly
3. Ensure `NEXTAUTH_SECRET` is set
4. Check browser console for errors

## Development Workflow

1. Make sure `.env` is configured and any existing SQLite files are not locked
2. Start Java backend (`mvn exec:java`)
3. Start Next.js (`npm run dev`)
4. Make changes
5. Java backend auto-reloads (if using IDE)
6. Next.js hot-reloads automatically

## File Storage

Files are stored in the `uploads/` directory:
```
uploads/
  spaces/
    {space-id}/
      files/
        {uuid}.{ext}
```

Make sure the `uploads/` directory is writable.

