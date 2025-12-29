/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  allowedDevOrigins: ['http://localhost:3000', 'http://10.0.0.11:3000'],
  devIndicators: false
}

module.exports = nextConfig
