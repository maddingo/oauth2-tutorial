# Troubleshooting Guide

## Common Issues and Solutions

### "Invalid state parameter" Error

**Symptoms:** After clicking "Sign In" and completing authentication, you see an error: "Invalid state parameter - possible CSRF attack"

**Cause:** This usually happens because the state stored in sessionStorage during login is not available when the callback executes.

**Debug Steps:**

1. **Open Browser DevTools** (F12) and go to the Console tab

2. **Clear storage and try again:**
   - Go to Application tab → Storage → Clear site data
   - Refresh the page
   - Click "Sign In" again

3. **Check the console logs:**

   When you click "Sign In", you should see:
   ```
   OAuth2 Login initiated: {
     state: "abc123...",
     codeVerifier: "xyz789...",
     codeChallenge: "def456..."
   }
   ```

   When you return from the authorization server, you should see:
   ```
   OAuth2 Callback received: {
     hasCode: true,
     hasState: true,
     hasError: false
   }

   SessionStorage contents: {
     hasStoredState: true,
     hasStoredVerifier: true,
     receivedState: "abc123...",
     storedState: "abc123..."
   }
   ```

4. **If `hasStoredState: false`:**
   - Your browser may be blocking sessionStorage
   - Check if you're in Private/Incognito mode with strict privacy settings
   - Check browser extensions that might block storage (Privacy Badger, etc.)
   - Verify that cookies/storage are enabled for `localhost`

5. **If state values don't match:**
   - Don't refresh the callback page - this invalidates the state
   - Make sure you're not opening the callback URL directly
   - Start the flow from the beginning by clicking "Sign In"

**Solution:**
- Clear browser storage completely
- Disable privacy-blocking extensions temporarily
- Use a standard browser window (not Private/Incognito)
- Ensure you complete the full OAuth2 flow without interruption

### React Strict Mode Double Rendering

**Symptoms:** The callback handler runs twice in development mode

**Cause:** React 18+ Strict Mode intentionally double-invokes effects in development to help catch bugs

**Solution:** This is expected behavior and has been handled with a `useRef` guard. The second execution is prevented automatically. This doesn't happen in production builds.

### CORS Errors

**Symptoms:** Browser console shows CORS errors when trying to exchange tokens

**Cause:** Authorization server doesn't allow requests from `http://localhost:3000`

**Solution:**
1. Check `authorization-server/src/main/resources/application.yaml`
2. Ensure CORS configuration includes:
   ```yaml
   auth-server:
     cors:
       allowed-origins:
         - "http://localhost:3000"
       allowed-methods:
         - "GET"
         - "POST"
       allow-credentials: true
   ```
3. Restart the authorization server

### Token Exchange Fails

**Symptoms:** Error: "Token exchange failed: ..."

**Debug Steps:**
1. Check that the authorization server is running on `http://idp:9000`
2. Verify `/etc/hosts` has the entry: `127.0.0.1 idp`
3. Check browser console for the actual error from the server
4. Verify the `public-client` is configured in the authorization server

**Common causes:**
- Authorization server not running
- Wrong client_id in `config/oauth.ts`
- PKCE not enabled for the client
- Network issues

### "No stored state found - session may have expired"

**Symptoms:** This specific error appears in the callback

**Causes:**
1. **Browser closed the tab** - sessionStorage is cleared when tab closes
2. **Long delay** - Took too long to complete login (rare)
3. **Different tab/window** - Opened callback URL in different tab than login
4. **Browser privacy settings** - Blocking sessionStorage

**Solution:**
1. Complete the entire OAuth2 flow in one tab without closing it
2. Don't wait too long at the login page
3. Check browser privacy settings
4. Try a different browser

### Authorization Server Connection Failed

**Symptoms:** Cannot reach `http://idp:9000`

**Solution:**
1. Verify authorization server is running:
   ```bash
   cd ../authorization-server
   mvn spring-boot:run
   ```

2. Check `/etc/hosts`:
   ```bash
   cat /etc/hosts | grep idp
   # Should show: 127.0.0.1 idp
   ```

3. Test the connection:
   ```bash
   curl http://idp:9000/.well-known/openid-configuration
   ```

### Dev Server Won't Start

**Symptoms:** `npm run dev` fails

**Solution:**
1. Delete `node_modules` and `.next`:
   ```bash
   rm -rf node_modules .next
   ```

2. Reinstall dependencies:
   ```bash
   npm install
   ```

3. Try running on a different port:
   ```bash
   npm run dev -- -p 3001
   ```
   (Don't forget to update `redirectUri` in `config/oauth.ts`)

## Getting Help

If you're still stuck:

1. **Clear everything:**
   ```bash
   # Clear browser storage (DevTools → Application → Clear site data)
   # Clear Next.js cache
   rm -rf .next
   # Restart dev server
   npm run dev
   ```

2. **Check the browser console** for the debug logs added to help troubleshoot

3. **Verify the full flow:**
   - Authorization server running on port 9000
   - Next.js app running on port 3000
   - `/etc/hosts` configured correctly
   - Browser allows localStorage and sessionStorage
   - No privacy extensions blocking storage

4. **Try a minimal test:**
   - Open browser DevTools console
   - Go to `http://localhost:3000`
   - Type: `sessionStorage.setItem('test', 'works')`
   - Type: `sessionStorage.getItem('test')`
   - Should return `'works'`
   - If this fails, sessionStorage is blocked by your browser

## Production Considerations

These debugging logs should be removed in production. To do so:

1. Remove all `console.log` statements from:
   - `lib/oauth.ts`
   - `app/callback/page.tsx`

2. Or use a logging library that can be disabled in production:
   ```typescript
   const debug = process.env.NODE_ENV === 'development' ? console.log : () => {};
   debug('Debug message here');
   ```
