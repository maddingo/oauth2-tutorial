# Automatic Token Refresh

This implementation includes automatic access token refresh using the **reactive strategy**, where tokens are checked and refreshed before making API calls.

## How It Works

### 1. Token Storage
When you log in, the application stores:
- **Access token**: Short-lived token for API authentication
- **Refresh token**: Long-lived token to obtain new access tokens
- **Expiration timestamp**: When the access token expires (with 60-second buffer)

All tokens are stored in both React state and localStorage for persistence across page refreshes.

### 2. Reactive Refresh Strategy
The reactive strategy checks token expiration **before each API call**:

```typescript
// Before making an API call:
1. Check if access token is expired
2. If expired, use refresh token to get a new access token
3. Update stored tokens
4. Make the API call with the new token
```

This approach has several benefits:
- **No background timers**: Tokens only refresh when needed
- **Efficient**: No unnecessary refresh requests
- **Reliable**: Always checks expiration before API calls

## Usage

### Using the Hook (Recommended for React Components)

```typescript
'use client';

import { useAuthenticatedFetch } from '@/lib/authenticatedFetch';

export default function MyComponent() {
  const authenticatedFetch = useAuthenticatedFetch();

  const fetchData = async () => {
    try {
      // This automatically refreshes the token if needed
      const response = await authenticatedFetch('https://api.example.com/data');
      const data = await response.json();
      console.log(data);
    } catch (error) {
      console.error('API call failed:', error);
    }
  };

  return <button onClick={fetchData}>Fetch Data</button>;
}
```

### Using the Direct Function (For Non-React Code)

```typescript
import { authenticatedFetch } from '@/lib/authenticatedFetch';

// In any JavaScript/TypeScript file
async function callApi() {
  const response = await authenticatedFetch('https://api.example.com/data', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
  });
  return response.json();
}
```

### Manual Token Refresh

You can also manually refresh the token using the auth context:

```typescript
import { useAuth } from '@/context/AuthContext';

export default function MyComponent() {
  const { refreshAccessToken, isTokenExpired } = useAuth();

  const handleRefresh = async () => {
    if (isTokenExpired()) {
      try {
        await refreshAccessToken();
        console.log('Token refreshed successfully');
      } catch (error) {
        console.error('Failed to refresh token:', error);
      }
    }
  };

  return <button onClick={handleRefresh}>Refresh Token</button>;
}
```

## Implementation Details

### AuthContext Changes
The `AuthContext` now includes:
- `refreshToken`: The OAuth2 refresh token
- `tokenExpiration`: Timestamp when the access token expires
- `refreshAccessToken()`: Function to manually refresh the token
- `isTokenExpired()`: Function to check if the token is expired

### Token Expiration Buffer
Tokens are considered expired 60 seconds **before** their actual expiration time. This buffer ensures:
- API calls don't fail due to token expiring mid-request
- Time for network latency
- Graceful handling of clock skew

### Error Handling
If token refresh fails:
1. Error is logged to console
2. User is automatically logged out
3. All tokens are cleared from storage
4. User must log in again

This ensures security and prevents using invalid tokens.

## Authorization Server Configuration

For token refresh to work, your authorization server must:

1. **Issue refresh tokens** for the client:
   ```yaml
   spring:
     security:
       oauth2:
         authorizationserver:
           client:
             public-client:
               authorization-grant-types: authorization_code,refresh_token
               token-settings:
                 reuse-refresh-tokens: false  # Recommended for security
   ```

2. **Enable refresh token grant type** in client configuration

3. **Set appropriate token lifetimes**:
   ```yaml
   token-settings:
     access-token-time-to-live: 5m   # Short-lived
     refresh-token-time-to-live: 24h # Longer-lived
   ```

## Testing Token Refresh

### Manual Testing
1. Log in to the application
2. Open browser DevTools > Application > Local Storage
3. Note the `token_expiration` timestamp
4. Wait for the token to expire (or manually set an expired timestamp)
5. Click "Try Authenticated API Call" button
6. Check console logs - you should see "Token expired, refreshing..."
7. The API call should succeed with a refreshed token

### Programmatic Testing
```typescript
// Force token expiration for testing
localStorage.setItem('token_expiration', (Date.now() - 1000).toString());

// Next API call will trigger refresh
const response = await authenticatedFetch('https://api.example.com/data');
```

## Security Considerations

### Current Implementation (Development)
- ✅ Tokens stored in localStorage
- ✅ Refresh token rotation (if server configured)
- ✅ Automatic logout on refresh failure
- ✅ 60-second expiration buffer

### Production Recommendations
- ⚠️ Use httpOnly cookies instead of localStorage
- ⚠️ Implement Backend-for-Frontend (BFF) pattern
- ⚠️ Store tokens on secure backend, not in browser
- ⚠️ Add CSRF protection for token endpoints
- ⚠️ Use secure, SameSite cookies
- ⚠️ Implement refresh token rotation
- ⚠️ Add refresh token revocation

## Troubleshooting

### Token Refresh Fails with 400 Error
**Cause**: Authorization server doesn't support refresh tokens for this client

**Solution**: Add `refresh_token` to `authorization-grant-types` in server config

### Tokens Not Persisting Across Page Reloads
**Cause**: localStorage might be disabled or cleared

**Solution**: Check browser privacy settings and localStorage availability

### Refresh Token Not Received
**Cause**: Authorization server not configured to issue refresh tokens

**Solution**: Check server logs and ensure refresh_token grant type is enabled

### Infinite Refresh Loop
**Cause**: Token expiration time incorrectly calculated

**Solution**: Check that `expires_in` from token response is in seconds (not milliseconds)

## Example: Complete API Integration

```typescript
'use client';

import { useState } from 'react';
import { useAuthenticatedFetch } from '@/lib/authenticatedFetch';

export default function QuotesPage() {
  const authenticatedFetch = useAuthenticatedFetch();
  const [quote, setQuote] = useState<string>('');

  const fetchQuote = async () => {
    const response = await authenticatedFetch('http://localhost:8090/quote');
    const data = await response.json();
    setQuote(data.quote);
  };

  return (
    <div>
      <button onClick={fetchQuote}>Get Quote</button>
      {quote && <p>{quote}</p>}
    </div>
  );
}
```

This automatically handles:
- ✅ Token expiration checking
- ✅ Automatic token refresh
- ✅ Request retry with new token
- ✅ Error handling and logout

## See Also
- [OAuth2.1 Specification](https://oauth.net/2.1/)
- [RFC 6749 - Refresh Token](https://tools.ietf.org/html/rfc6749#section-1.5)
- [PKCE Specification](https://tools.ietf.org/html/rfc7636)
