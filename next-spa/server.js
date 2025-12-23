const { createServer } = require('https');
const { parse } = require('url');
const next = require('next');
const fs = require('fs');
const path = require('path');

const dev = process.env.NODE_ENV !== 'production';
const hostname = 'localhost';
const port = parseInt(process.env.PORT || '3000', 10);

const app = next({ dev, hostname, port });
const handle = app.getRequestHandler();

// Check if certificates exist
const certPath = path.join(__dirname, 'localhost+2.pem');
const keyPath = path.join(__dirname, 'localhost+2-key.pem');

if (!fs.existsSync(certPath) || !fs.existsSync(keyPath)) {
  console.error('❌ SSL certificates not found!');
  console.error('');
  console.error('Please generate certificates first:');
  console.error('  1. Install mkcert: brew install mkcert (macOS) or see HTTPS_SETUP.md');
  console.error('  2. Run: mkcert -install');
  console.error('  3. Run: mkcert localhost 127.0.0.1 ::1');
  console.error('');
  console.error('Or see HTTPS_SETUP.md for other options.');
  process.exit(1);
}

const httpsOptions = {
  key: fs.readFileSync(keyPath),
  cert: fs.readFileSync(certPath),
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
  })
    .once('error', (err) => {
      console.error('Server error:', err);
      process.exit(1);
    })
    .listen(port, (err) => {
      if (err) throw err;
      console.log('');
      console.log(`✓ HTTPS Server ready`);
      console.log(`  Local:            https://${hostname}:${port}`);
      console.log(`  Network:          https://127.0.0.1:${port}`);
      console.log('');
      console.log('🔒 Secure cookies enabled (httpOnly + secure flags)');
      console.log('');
    });
});
