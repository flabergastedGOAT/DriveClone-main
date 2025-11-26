/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Proxy API requests to Java backend
  async rewrites() {
    return [
      {
        source: '/api/java/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ];
  },
}

module.exports = nextConfig

