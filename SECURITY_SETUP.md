# Security Setup Guide

## 🔒 Important Security Notes

**NEVER commit sensitive files to GitHub!** This includes:
- Firebase service account keys
- Environment files with real credentials
- Database files with real data
- Any files containing secrets or API keys

## 📁 Files That Should NOT Be Committed

The following files are automatically ignored by git (see `.gitignore`):
- `.env` (contains your actual environment variables)
- `*firebase-adminsdk*.json` (Firebase service account keys)
- `*service-account*.json` (Google Cloud service account keys)
- `*credentials*.json` (Any credential files)
- `*.db`, `*.sqlite`, `*.sqlite3` (Database files)

## 🚀 Local Development Setup

### 1. Environment Variables
Copy the example environment file and fill in your actual values:
```bash
cp env.example .env
```

Edit `.env` with your actual credentials:
```bash
# Server Configuration
PORT=8080

# Database Configuration
DB_PATH=driveclone.db

# Firebase Configuration
FIREBASE_PROJECT_ID=your-actual-project-id
FIREBASE_WEB_CLIENT_ID=your-actual-web-client-id
GOOGLE_APPLICATION_CREDENTIALS=./your-firebase-service-account-key.json

# Supabase Configuration
SUPABASE_URL=your-actual-supabase-url
SUPABASE_ANON_KEY=your-actual-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-actual-service-role-key

# Admin Configuration
ADMIN_EMAIL=your-admin-email@gmail.com

# JWT Secret (use a strong, random string)
JWT_SECRET=your-strong-random-jwt-secret
```

### 2. Firebase Service Account Key
1. Download your Firebase service account key from the Firebase Console
2. Save it in the project root with a name like `firebase-service-account.json`
3. Update the `GOOGLE_APPLICATION_CREDENTIALS` path in your `.env` file

### 3. Running the Application
```bash
# Build the project
mvn clean package

# Run the application
java -jar target/DriveClone-1.0.0.jar
```

## ✅ Security Checklist

- [ ] `.env` file is in `.gitignore` ✓
- [ ] Firebase service account keys are in `.gitignore` ✓
- [ ] Database files are in `.gitignore` ✓
- [ ] No sensitive files are tracked by git
- [ ] Environment variables are loaded from `.env` file
- [ ] Firebase Admin SDK initializes with service account credentials

## 🔍 Verifying Security

Check that sensitive files are not tracked:
```bash
git status
git ls-files | grep -E "\.(env|json)$"
```

If any sensitive files appear, remove them:
```bash
git rm --cached filename
git commit -m "Remove sensitive file from tracking"
```

## 🚨 If You Accidentally Committed Secrets

1. **Immediately revoke/regenerate the exposed credentials**
2. Remove the file from git history:
   ```bash
   git filter-branch --force --index-filter "git rm --cached --ignore-unmatch filename" --prune-empty --tag-name-filter cat -- --all
   ```
3. Force push to update the remote repository:
   ```bash
   git push --force-with-lease
   ```
4. Add the file to `.gitignore` to prevent future commits

## 📝 Best Practices

1. **Always use `.env.example`** as a template for others
2. **Never commit real credentials** to version control
3. **Use environment variables** for all configuration
4. **Rotate credentials regularly** in production
5. **Use different credentials** for development and production
6. **Keep service account keys secure** and limit their permissions

## 🛡️ Production Security

For production deployment:
- Use environment variables instead of files
- Store secrets in secure secret management systems
- Use least-privilege service accounts
- Enable audit logging
- Regularly rotate credentials
- Monitor for unauthorized access
