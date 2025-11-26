# DriveClone - Next.js + Java Backend

A shared online file storage service built with Next.js frontend and Java backend. Users can sign in using Google (NextAuth.js), create or join workspaces, and upload/download files stored locally on the device.

## Architecture

- **Frontend**: Next.js 14 with NextAuth.js for authentication
- **Backend**: Java 17 with SparkJava web framework
- **Database**: SQLite (file-based)
- **File Storage**: Local file system
- **Authentication**: NextAuth.js with Google OAuth

## Features

- **NextAuth.js Authentication**: Secure Google-based login
- **Workspace Management**: Create and manage shared workspaces with admin controls
- **File Operations**: Upload, download, and delete files
- **Access Control**: Role-based permissions (ADMIN, MEMBER)
- **Member Management**: Invite, remove, and promote/demote members
- **Space Settings**: Update workspace name/description or delete entire space
- **Activity Logging**: Complete audit trail of all operations
- **Local File Storage**: Files stored on local device
- **RESTful API**: Clean HTTP API for all operations

## Prerequisites

- Node.js 18+ and npm/yarn
- Java 17 or higher
- Maven 3.6 or higher
- SQLite (bundled, no separate install)
- Google OAuth credentials (for authentication)

## Setup Instructions

### 1. Environment Variables

Copy the example environment file and configure:

```bash
cp .env.example .env
```

Edit `.env` with your values:

```env
# Next.js Configuration
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-here  # Generate with: openssl rand -base64 32

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Database
DATABASE_URL="file:./dev.db"
DB_PATH=driveclone.db

# Java Backend
JAVA_API_URL=http://localhost:8080
PORT=8080
```

### 2. Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URI: `http://localhost:3000/api/auth/callback/google`
6. Copy Client ID and Client Secret to `.env`

### 3. Install Dependencies

**Next.js Frontend:**
```bash
npm install
```

**Java Backend:**
```bash
mvn clean install
```

### 4. Database Migration (Prisma SQLite file will be created automatically)
```bash
npx prisma generate
npx prisma db push
```

**Java Backend:**
SQLite tables are created automatically on first run.

### 5. Run the Application

**Start Java Backend (Terminal 1):**
```bash
mvn exec:java
# Or
java -jar target/DriveClone-1.0.0.jar
```

The Java backend will run on `http://localhost:8080`

**Start Next.js Frontend (Terminal 2):**
```bash
npm run dev
```

The Next.js app will run on `http://localhost:3000`

## Project Structure

```
.
├── app/                    # Next.js app directory
│   ├── api/               # Next.js API routes (proxies to Java)
│   ├── page.tsx           # Main page
│   └── layout.tsx         # Root layout
├── components/             # React components
│   ├── Dashboard.tsx      # Main dashboard
│   └── SessionProvider.tsx
├── lib/                   # Utility libraries
│   ├── auth.ts            # NextAuth configuration
│   ├── prisma.ts          # Prisma client
│   └── java-api.ts        # Java API client
├── prisma/                # Prisma schema
│   └── schema.prisma
├── src/main/java/         # Java backend
│   └── com/driveclone/
│       ├── DriveCloneApp.java
│       ├── auth/          # NextAuth JWT verifier
│       ├── service/       # Business logic
│       ├── model/         # Data models
│       └── config/         # Configuration
└── uploads/               # Local file storage
```

## API Endpoints

### Next.js API Routes (Proxy to Java)

- `GET /api/spaces` - List user's spaces
- `POST /api/spaces` - Create a new space
- `GET /api/spaces/:id` - Get space details
- `PUT /api/spaces/:id` - Update space
- `DELETE /api/spaces/:id` - Delete space
- `GET /api/spaces/:id/files` - List files in space
- `POST /api/spaces/:id/files` - Upload file
- `GET /api/files/:fileId` - Download file
- `DELETE /api/files/:fileId` - Delete file
- `GET /api/spaces/:id/activity` - Get activity log
- `POST /api/spaces/:id/members` - Add member
- `DELETE /api/spaces/:id/members/:email` - Remove member

### Java Backend API

All endpoints are under `/api/*` and require Bearer token authentication.

## Development

### Running in Development Mode

1. Ensure `.env` is configured and SQLite files are writable
2. Start Java backend: `mvn exec:java`
3. Start Next.js: `npm run dev`

### Building for Production

**Next.js:**
```bash
npm run build
npm start
```

**Java:**
```bash
mvn clean package
java -jar target/DriveClone-1.0.0.jar
```

## Database Schema

The application uses SQLite with the following main tables:

- **users**: User accounts (managed by NextAuth/Prisma)
- **spaces**: Shared workspaces
- **space_members**: Workspace membership
- **space_files**: File metadata
- **activity**: Activity logs

## Security Features

- NextAuth.js with Google OAuth
- JWT token verification in Java backend
- Role-based access control
- Secure file storage on local device
- Complete activity logging

## License

This project is licensed under the MIT License.
