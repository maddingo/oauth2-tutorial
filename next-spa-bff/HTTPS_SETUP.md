# Running next-spa with HTTPS in Production Mode

The BFF pattern implementation uses secure HttpOnly cookies that require HTTPS in production. This guide provides several options for running the next-spa application with HTTPS.

## Quick Comparison

| Option | Best For | Complexity | Production Ready |
|--------|----------|------------|------------------|
| **1. mkcert** | Local development/testing | ⭐ Easy | ❌ No (dev only) |
| **2. Caddy Reverse Proxy** | Quick local HTTPS | ⭐⭐ Easy | ✅ Yes |
| **3. Nginx Reverse Proxy** | Traditional production | ⭐⭐⭐ Medium | ✅ Yes |
| **4. Next.js Custom Server** | Direct HTTPS control | ⭐⭐⭐ Medium | ✅ Yes |
| **5. Cloud Deployment** | Production hosting | ⭐ Easy | ✅ Yes |

---

## Option 1: Local HTTPS with mkcert (Development)

**Best for**: Testing HTTPS locally before deployment

### Install mkcert

```bash
# macOS
brew install mkcert
brew install nss # for Firefox

# Linux
sudo apt install libnss3-tools
wget https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/mkcert-v1.4.4-linux-amd64
chmod +x mkcert-v1.4.4-linux-amd64
sudo mv mkcert-v1.4.4-linux-amd64 /usr/local/bin/mkcert

# Windows (with Chocolatey)
choco install mkcert
```

### Generate Local Certificates

```bash
cd next-spa

# Install local CA
mkcert -install

# Generate certificates for localhost
mkcert localhost 127.0.0.1 ::1

# This creates:
# - localhost+2.pem (certificate)
# - localhost+2-key.pem (private key)
```

### Create Custom Server

Create `next-spa/server.js`:

```javascript
const { createServer } = require('https');
const { parse } = require('url');
const next = require('next');
const fs = require('fs');

const dev = process.env.NODE_ENV !== 'production';
const hostname = 'localhost';
const port = 3000;

const app = next({ dev, hostname, port });
const handle = app.getRequestHandler();

const httpsOptions = {
  key: fs.readFileSync('./localhost+2-key.pem'),
  cert: fs.readFileSync('./localhost+2.pem'),
};

app.prepare().then(() => {
  createServer(httpsOptions, async (req, res) => {
    try {
      const parsedUrl = parse(req.url, true);
      await handle(req, res, parsedUrl);
    } catch (err) {
      console.error('Error occurred handling', req.url, err);
      res.statusCode = 500;
      res.end('internal server error');
    }
  }).listen(port, (err) => {
    if (err) throw err;
    console.log(`> Ready on https://${hostname}:${port}`);
  });
});
```

### Update package.json

```json
{
  "scripts": {
    "dev": "next dev",
    "dev:https": "node server.js",
    "build": "next build",
    "start": "NODE_ENV=production node server.js"
  }
}
```

### Run with HTTPS

```bash
npm run dev:https
# Access at: https://localhost:3000
```

### Update OAuth Configuration

Update `next-spa/config/oauth.ts`:

```typescript
export const oauthConfig = {
  authorizationEndpoint: 'http://idp:9000/oauth2/authorize',
  tokenEndpoint: 'http://idp:9000/oauth2/token',
  userinfoEndpoint: 'http://idp:9000/userinfo',
  clientId: 'public-client',
  redirectUri: 'https://localhost:3000/api/auth/callback', // ← Changed to HTTPS
  scope: 'openid profile message.read',
  responseType: 'code',
};
```

### Update Authorization Server

Add HTTPS redirect URI in `authorization-server/src/main/resources/application.yaml`:

```yaml
redirect-uris:
  # ... existing URIs ...
  - "https://localhost:3000/api/auth/callback"
```

---

## Option 2: Caddy Reverse Proxy (Recommended for Local)

**Best for**: Automatic HTTPS with minimal configuration

### Install Caddy

```bash
# macOS
brew install caddy

# Linux
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install caddy
```

### Create Caddyfile

Create `next-spa/Caddyfile`:

```caddy
localhost {
    # Automatic HTTPS with self-signed cert for localhost
    tls internal

    # Reverse proxy to Next.js
    reverse_proxy localhost:3001
}
```

### Update package.json

Change the dev port to avoid conflicts:

```json
{
  "scripts": {
    "dev": "next dev -p 3001",
    "caddy": "caddy run --config Caddyfile",
    "build": "next build",
    "start": "next start -p 3001"
  }
}
```

### Run with Caddy

```bash
# Terminal 1: Start Next.js on port 3001
npm run dev

# Terminal 2: Start Caddy
npm run caddy

# Access at: https://localhost (port 443)
```

### Production Caddyfile (with domain)

```caddy
yourdomain.com {
    # Automatic HTTPS with Let's Encrypt
    reverse_proxy localhost:3001

    # Enable compression
    encode gzip zstd

    # Security headers
    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        X-XSS-Protection "1; mode=block"
        Referrer-Policy "strict-origin-when-cross-origin"
    }
}
```

---

## Option 3: Nginx Reverse Proxy

**Best for**: Production deployments with more control

### Install Nginx

```bash
# Linux
sudo apt update
sudo apt install nginx

# macOS
brew install nginx
```

### Generate Self-Signed Certificate (for testing)

```bash
sudo mkdir -p /etc/nginx/ssl
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/nginx/ssl/localhost.key \
  -out /etc/nginx/ssl/localhost.crt \
  -subj "/CN=localhost"
```

### Create Nginx Configuration

Create `/etc/nginx/sites-available/next-spa`:

```nginx
server {
    listen 443 ssl http2;
    server_name localhost;

    # SSL Configuration
    ssl_certificate /etc/nginx/ssl/localhost.crt;
    ssl_certificate_key /etc/nginx/ssl/localhost.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Proxy to Next.js
    location / {
        proxy_pass http://localhost:3001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # Larger upload size if needed
    client_max_body_size 10M;
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name localhost;
    return 301 https://$server_name$request_uri;
}
```

### Enable Configuration

```bash
# Linux
sudo ln -s /etc/nginx/sites-available/next-spa /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# macOS
sudo nginx -t
sudo nginx -s reload
```

### Production Nginx with Let's Encrypt

Install Certbot:

```bash
sudo apt install certbot python3-certbot-nginx
```

Obtain certificate:

```bash
sudo certbot --nginx -d yourdomain.com
```

Updated config will look like:

```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # ... rest of configuration
}
```

---

## Option 4: Next.js Custom Server with Production Build

### Install Dependencies

```bash
cd next-spa
npm install --save-dev @types/node
```

### Create Production Server

Create `next-spa/server-production.js`:

```javascript
const { createServer } = require('https');
const { parse } = require('url');
const next = require('next');
const fs = require('fs');
const path = require('path');

const hostname = process.env.HOSTNAME || 'localhost';
const port = parseInt(process.env.PORT || '3000', 10);

// Must build first: npm run build
const app = next({
  dev: false,
  hostname,
  port,
  dir: __dirname
});

const handle = app.getRequestHandler();

// Load SSL certificates
const httpsOptions = {
  key: fs.readFileSync(process.env.SSL_KEY_PATH || './localhost+2-key.pem'),
  cert: fs.readFileSync(process.env.SSL_CERT_PATH || './localhost+2.pem'),
};

app.prepare().then(() => {
  createServer(httpsOptions, async (req, res) => {
    try {
      const parsedUrl = parse(req.url, true);
      await handle(req, res, parsedUrl);
    } catch (err) {
      console.error('Error occurred handling', req.url, err);
      res.statusCode = 500;
      res.end('internal server error');
    }
  }).listen(port, (err) => {
    if (err) throw err;
    console.log(`> Production server running on https://${hostname}:${port}`);
  });
});
```

### Update package.json

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "NODE_ENV=production node server-production.js",
    "start:https": "NODE_ENV=production node server-production.js"
  }
}
```

### Build and Run

```bash
# Build for production
npm run build

# Run with HTTPS
npm run start:https
```

### Environment Variables

Create `.env.production`:

```bash
NODE_ENV=production
HOSTNAME=localhost
PORT=3000
SSL_KEY_PATH=/path/to/private.key
SSL_CERT_PATH=/path/to/certificate.crt
RESOURCE_SERVER_URL=http://localhost:8090
```

---

## Option 5: Cloud Deployment (Production)

### Vercel (Easiest)

Vercel automatically provides HTTPS.

```bash
# Install Vercel CLI
npm install -g vercel

# Deploy
cd next-spa
vercel

# Follow prompts, then access via provided HTTPS URL
```

Update OAuth config to use Vercel domain:

```typescript
redirectUri: 'https://your-app.vercel.app/api/auth/callback'
```

### Docker with Traefik (Self-hosted)

Create `next-spa/Dockerfile`:

```dockerfile
FROM node:18-alpine AS base

# Install dependencies
FROM base AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci

# Build application
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# Production image
FROM base AS runner
WORKDIR /app

ENV NODE_ENV=production

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
```

Update `next.config.js`:

```javascript
module.exports = {
  output: 'standalone',
  // ... other config
};
```

Create `docker-compose.yml` with Traefik:

```yaml
version: '3.8'

services:
  traefik:
    image: traefik:v2.10
    command:
      - "--providers.docker=true"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.email=your@email.com"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
      - "--certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./letsencrypt:/letsencrypt

  next-spa:
    build: .
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.next-spa.rule=Host(`yourdomain.com`)"
      - "traefik.http.routers.next-spa.entrypoints=websecure"
      - "traefik.http.routers.next-spa.tls.certresolver=letsencrypt"
      - "traefik.http.services.next-spa.loadbalancer.server.port=3000"
    environment:
      - NODE_ENV=production
      - RESOURCE_SERVER_URL=http://resource-server:8090
```

Deploy:

```bash
docker-compose up -d
```

---

## Testing HTTPS Setup

### 1. Verify Secure Cookies

Open DevTools → Application → Cookies:

```
Name: access_token
Value: [token]
Domain: localhost
Path: /
Secure: ✓ (should be checked)
HttpOnly: ✓ (should be checked)
SameSite: Lax
```

### 2. Test Authentication Flow

```bash
# 1. Visit your HTTPS URL
open https://localhost:3000

# 2. Click "Login"
# 3. Authenticate
# 4. Check cookies are set with Secure flag
# 5. Verify quote fetching works
```

### 3. Check Security Headers

```bash
curl -I https://localhost:3000

# Should include:
# Strict-Transport-Security: max-age=31536000
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
```

---

## Troubleshooting

### Issue: "NET::ERR_CERT_AUTHORITY_INVALID"

**Solution**:
- For mkcert: Run `mkcert -install` to trust local CA
- For self-signed: Accept certificate in browser
- For production: Use proper CA-signed certificate

### Issue: Cookies not being set

**Solution**:
- Verify `secure: true` in cookie options
- Check HTTPS is actually being used
- Ensure `credentials: 'include'` in fetch requests

### Issue: CORS errors with HTTPS

**Solution**: Update authorization server CORS config:

```yaml
auth-server:
  cors:
    allowed-origins:
      - "https://localhost:3000"  # Add HTTPS
      - "http://localhost:3000"   # Keep HTTP for dev
```

### Issue: Mixed content warnings

**Solution**: Ensure all resources (API calls, images) use HTTPS or relative URLs

---

## Recommended Setup by Environment

### Local Development
```
✅ Option 1: mkcert + Custom Server
- Quick setup
- Trusted certificates
- Good for testing secure cookies
```

### Staging/Testing
```
✅ Option 2: Caddy Reverse Proxy
- Automatic HTTPS
- Minimal configuration
- Easy to deploy
```

### Production
```
✅ Option 5: Cloud Deployment (Vercel, AWS, etc.)
OR
✅ Option 3: Nginx + Let's Encrypt
- Reliable and proven
- Full control
- Free SSL certificates
```

---

## Quick Start Script

Create `next-spa/https-dev.sh`:

```bash
#!/bin/bash

echo "Setting up HTTPS for local development..."

# Check if mkcert is installed
if ! command -v mkcert &> /dev/null; then
    echo "mkcert not found. Please install it first:"
    echo "  macOS: brew install mkcert"
    echo "  Linux: See HTTPS_SETUP.md"
    exit 1
fi

# Install local CA
mkcert -install

# Generate certificates
mkcert localhost 127.0.0.1 ::1

echo "✓ Certificates generated"
echo ""
echo "Next steps:"
echo "1. Create server.js (see HTTPS_SETUP.md)"
echo "2. Run: npm run dev:https"
echo "3. Visit: https://localhost:3000"
```

Make it executable:

```bash
chmod +x https-dev.sh
./https-dev.sh
```

---

## Summary

For **development/testing**: Use **mkcert** (Option 1)
For **quick local HTTPS**: Use **Caddy** (Option 2)
For **production**: Use **Cloud deployment** (Option 5) or **Nginx + Let's Encrypt** (Option 3)

All options ensure the BFF pattern's secure cookies work correctly with the `secure` flag enabled! 🔒
