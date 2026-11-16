/** @type {import('next').NextConfig} */
const nextConfig = {
  serverExternalPackages: ["firebase-admin", "jwks-rsa", "jose", "livekit-server-sdk"],
};

export default nextConfig;
