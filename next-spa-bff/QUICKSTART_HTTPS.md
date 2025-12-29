# Quick Start: HTTPS Setup (5 minutes)

This guide gets you running with HTTPS locally in 5 minutes using mkcert.

## Prerequisites

- Node.js 18+ installed
- next-spa application code

## Step 1: Install mkcert

Choose your platform:

### macOS
```bash
brew install mkcert
brew install nss  # for Firefox support
```

### Linux (Debian/Ubuntu)
```bash
sudo apt install libnss3-tools
wget https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/mkcert-v1.4.4-linux-amd64
chmod +x mkcert-v1.4.4-linux-amd64
sudo mv mkcert-v1.4.4-linux-amd64 /usr/local/bin/mkcert
```

### Windows (with Chocolatey)
```bash
choco install mkcert
```

## Step 2: Run Setup Script

```bash
cd next-spa
./setup-https.sh
```

This will:
- Install local Certificate Authority
- Generate SSL certificates (localhost+2.pem, localhost+2-key.pem)
- Update .gitignore

## Step 3: Start HTTPS Server

```bash
npm install
npm run dev:https
```

Output:
```
✓ HTTPS Server ready
  Local:            https://localhost:3000
  Network:          https://127.0.0.1:3000

🔒 Secure cookies enabled (httpOnly + secure flags)
```

## Step 4: Update Authorization Server

Add HTTPS redirect URI to `authorization-server/src/main/resources/application.yaml`:

```yaml
redirect-uris:
  # ... existing URIs ...
  - "https://localhost:3000/api/auth/callback"
```

Restart authorization server:
```bash
cd authorization-server
mvn spring-boot:run
```

## Step 5: Update OAuth Config

Update `next-spa/config/oauth.ts`:

```typescript
export const oauthConfig = {
  // ... other config ...
  redirectUri: 'https://localhost:3000/api/auth/callback', // ← HTTPS
  // ...
};
```

## Step 6: Test It!

1. Visit **https://localhost:3000**
2. Click "Login"
3. Authenticate with `user1/password`
4. Check DevTools → Application → Cookies
5. Verify cookies have `Secure: ✓` flag

## Verify Secure Cookies

In browser DevTools → Application → Cookies → https://localhost:3000:

| Name | HttpOnly | Secure | SameSite |
|------|----------|--------|----------|
| access_token | ✓ | ✓ | Lax |
| refresh_token | ✓ | ✓ | Lax |
| token_expiration | ✓ | ✓ | Lax |

All should show checkmarks for HttpOnly and Secure!

## Troubleshooting

### Certificate not trusted
```bash
mkcert -install
```

### "Cannot find certificates"
Make sure you're in the `next-spa` directory when running the setup script.

### Port already in use
Change port in server.js:
```javascript
const port = parseInt(process.env.PORT || '3001', 10);
```

Then run:
```bash
PORT=3001 npm run dev:https
```

## What's Next?

- **Development**: Use `npm run dev` for HTTP (faster reload)
- **Testing HTTPS**: Use `npm run dev:https`
- **Production**: See `HTTPS_SETUP.md` for deployment options

## Production Deployment

For production, see **HTTPS_SETUP.md** for:
- Caddy reverse proxy
- Nginx + Let's Encrypt
- Docker with Traefik
- Cloud deployment (Vercel, AWS, etc.)

---

**Note**: mkcert is for local development only. For production, use proper CA-signed certificates (Let's Encrypt, etc.).
