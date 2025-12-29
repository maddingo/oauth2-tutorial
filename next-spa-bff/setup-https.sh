#!/bin/bash

echo "🔒 Setting up HTTPS for local development..."
echo ""

# Check if mkcert is installed
if ! command -v mkcert &> /dev/null; then
    echo "❌ mkcert not found!"
    echo ""
    echo "Please install mkcert first:"
    echo ""
    echo "  macOS:"
    echo "    brew install mkcert"
    echo "    brew install nss  # for Firefox"
    echo ""
    echo "  Linux (Debian/Ubuntu):"
    echo "    sudo apt install libnss3-tools"
    echo "    wget https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/mkcert-v1.4.4-linux-amd64"
    echo "    chmod +x mkcert-v1.4.4-linux-amd64"
    echo "    sudo mv mkcert-v1.4.4-linux-amd64 /usr/local/bin/mkcert"
    echo ""
    echo "  Windows (with Chocolatey):"
    echo "    choco install mkcert"
    echo ""
    exit 1
fi

echo "✓ mkcert found"

# Install local CA
echo ""
echo "Installing local Certificate Authority..."
mkcert -install

if [ $? -ne 0 ]; then
    echo "❌ Failed to install CA"
    exit 1
fi

echo "✓ Local CA installed"

# Generate certificates
echo ""
echo "Generating SSL certificates for localhost..."
mkcert localhost 127.0.0.1 ::1

if [ $? -ne 0 ]; then
    echo "❌ Failed to generate certificates"
    exit 1
fi

echo "✓ Certificates generated:"
echo "  - localhost+2.pem (certificate)"
echo "  - localhost+2-key.pem (private key)"

# Create .gitignore entry if it doesn't exist
if [ ! -f .gitignore ]; then
    touch .gitignore
fi

if ! grep -q "localhost+2" .gitignore; then
    echo "" >> .gitignore
    echo "# SSL certificates (local development)" >> .gitignore
    echo "localhost+2.pem" >> .gitignore
    echo "localhost+2-key.pem" >> .gitignore
    echo "*.pem" >> .gitignore
    echo "" >> .gitignore
    echo "✓ Updated .gitignore"
fi

echo ""
echo "✅ HTTPS setup complete!"
echo ""
echo "Next steps:"
echo "  1. Start the HTTPS dev server:"
echo "     npm run dev:https"
echo ""
echo "  2. Visit: https://localhost:3000"
echo ""
echo "  3. Update authorization server to allow:"
echo "     https://localhost:3000/api/auth/callback"
echo ""
echo "For more options, see HTTPS_SETUP.md"
echo ""
