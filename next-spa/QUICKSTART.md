# Quick Start Guide

Get up and running with the OAuth2.1 PKCE demo in 3 minutes.

## Prerequisites

1. **Authorization Server Running**
   ```bash
   cd ../authorization-server
   mvn spring-boot:run
   ```
   Server will start on `http://idp:9000`

2. **Update /etc/hosts** (if not already done)
   ```bash
   echo "127.0.0.1 idp" | sudo tee -a /etc/hosts
   ```

## Installation & Run

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Test the Flow

1. Click **"Sign In"** button
2. You'll be redirected to the authorization server login page
3. Login with:
   - Username: `user1`
   - Password: `password`
4. Grant consent to the requested scopes
5. You'll be redirected back to the app with your user info displayed
6. Click **"Sign Out"** to clear the session

## What's Happening Behind the Scenes?

1. **Sign In Click:**
   - App generates `code_verifier` (random 43-char string)
   - Creates `code_challenge` = Base64Url(SHA256(code_verifier))
   - Stores verifier in sessionStorage
   - Redirects to: `http://idp:9000/oauth2/authorize?...&code_challenge=...`

2. **Authorization Server:**
   - Stores the code_challenge
   - Authenticates user
   - Issues authorization code
   - Redirects to: `http://localhost:3000/callback?code=...&state=...`

3. **Token Exchange:**
   - App validates state (CSRF protection)
   - POSTs to token endpoint with:
     - `code`
     - `code_verifier`
   - Server verifies: SHA256(code_verifier) === stored code_challenge
   - Returns access_token and id_token

4. **User Info:**
   - App calls userinfo endpoint with access_token
   - Displays user information

## Troubleshooting

### "Cannot connect to authorization server"
- Ensure authorization server is running: `cd ../authorization-server && mvn spring-boot:run`
- Check `/etc/hosts` has `idp` entry

### CORS Errors
- Verify authorization server CORS config includes `http://localhost:3000`
- Check `authorization-server/src/main/resources/application.yaml`

### "Invalid state parameter"
- Don't refresh the callback page
- Start the login flow again from the home page

## Next Steps

- Review the code in `lib/pkce.ts` for PKCE implementation
- Check `lib/oauth.ts` for OAuth2 flow logic
- Examine `app/callback/page.tsx` for token exchange
- Read `README.md` for production considerations
