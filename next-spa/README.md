# Next.js SPA with OAuth2.1 PKCE

A minimal Next.js Single Page Application demonstrating OAuth2.1 Authorization Code Flow with PKCE (Proof Key for Code Exchange).

## Features

- **OAuth2.1 PKCE Flow**: Secure authorization without client secrets
- **TypeScript**: Full type safety
- **Next.js 14**: Using App Router
- **Session Storage**: Secure temporary storage for code verifier and state
- **Local Storage**: Persistent storage for tokens (production apps should use httpOnly cookies)
- **React Context**: Global authentication state management

## Prerequisites

1. Running Authorization Server from this repository on `http://idp:9000`
2. Add to `/etc/hosts`:
   ```
   127.0.0.1 idp
   ```

## Installation

```bash
cd next-spa
npm install
```

## Configuration

Edit `config/oauth.ts` to match your OAuth2 server settings:

```typescript
export const oauthConfig = {
  authorizationEndpoint: 'http://idp:9000/oauth2/authorize',
  tokenEndpoint: 'http://idp:9000/oauth2/token',
  userinfoEndpoint: 'http://idp:9000/userinfo',
  clientId: 'public-client',
  redirectUri: 'http://localhost:3000/callback',
  scope: 'openid profile',
  responseType: 'code',
};
```

## Running the Application

### Development Mode
```bash
npm run dev
```

The application will start on `http://localhost:3000`.

### Production Build
```bash
npm run build
npm start
```

## OAuth2.1 PKCE Flow

This implementation follows the OAuth2.1 Authorization Code Flow with PKCE:

1. **User clicks "Sign In"**
   - App generates random `code_verifier` (43-128 characters)
   - App creates `code_challenge` = SHA256(code_verifier)
   - Stores `code_verifier` and `state` in sessionStorage
   - Redirects to authorization server with challenge

2. **User authenticates at Authorization Server**
   - User logs in (e.g., user1/password)
   - User consents to scopes
   - Authorization server redirects back with `code` and `state`

3. **App exchanges code for tokens**
   - Validates `state` parameter (CSRF protection)
   - Sends `code` and `code_verifier` to token endpoint
   - Authorization server verifies: SHA256(code_verifier) === code_challenge
   - Receives access token and ID token

4. **App fetches user info**
   - Calls userinfo endpoint with access token
   - Displays user information

## Project Structure

```
next-spa/
├── app/
│   ├── callback/
│   │   └── page.tsx          # OAuth2 callback handler
│   ├── layout.tsx             # Root layout with AuthProvider
│   ├── page.tsx               # Home page with login/logout
│   └── globals.css            # Styles
├── config/
│   └── oauth.ts               # OAuth2 configuration
├── context/
│   └── AuthContext.tsx        # Authentication state management
├── lib/
│   ├── oauth.ts               # OAuth2 functions
│   └── pkce.ts                # PKCE utility functions
├── package.json
├── tsconfig.json
└── next.config.js
```

## Security Notes

### PKCE Implementation
- Uses SHA-256 for code challenge
- Code verifier: 43-character base64url-encoded random string
- State parameter: 16-byte random value for CSRF protection

### Token Storage
- **Development**: Uses localStorage for simplicity
- **Production**: Should use:
  - httpOnly cookies (not accessible to JavaScript)
  - Backend-for-Frontend (BFF) pattern
  - Secure, SameSite cookies

### CORS Configuration
The authorization server must allow:
- Origin: `http://localhost:3000`
- Methods: `GET`, `POST`
- Headers: `Authorization`, `Content-Type`

## Testing with the Repository Authorization Server

1. Start the authorization server:
   ```bash
   cd authorization-server
   mvn spring-boot:run
   ```

2. Test credentials:
   - Username: `user1`
   - Password: `password`

3. The client is already configured in `authorization-server/src/main/resources/application.yaml` as `public-client`.

## Troubleshooting

### CORS Errors
Ensure the authorization server has CORS configured for `http://localhost:3000`.

### Token Exchange Fails
Verify that:
- The authorization server is running on `http://idp:9000`
- `/etc/hosts` includes the `idp` entry
- The `public-client` is configured with `require-proof-key: true`

### State Validation Fails
This happens if you refresh the callback page. Start the login flow again from the home page.

## Production Considerations

For production deployments:
1. Use Backend-for-Frontend (BFF) pattern
2. Store tokens in httpOnly cookies
3. Implement token refresh flow
4. Add token expiration handling
5. Use HTTPS for all endpoints
6. Implement proper error logging
7. Add rate limiting
8. Consider using a production-grade OAuth2 library
