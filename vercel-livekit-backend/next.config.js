/** @type {import('next').NextConfig} */
const nextConfig = {
  serverExternalPackages: ['firebase-admin'],
  experimental: {
    serverComponentsExternalPackages: ['firebase-admin'],
  },
};

module.exports = nextConfig;
