import { NextResponse } from "next/server";
import { CORS_HEADERS, DEFAULT_FIREBASE_PROJECT_ID, handleCorsOptions } from "../../../lib/authAndLiveKit";

export async function OPTIONS() {
  return handleCorsOptions();
}

export async function GET() {
  const hasLiveKitUrl = Boolean((process.env.LIVEKIT_URL || process.env.NEXT_PUBLIC_LIVEKIT_URL || "").trim());
  const hasLiveKitKey = Boolean((process.env.LIVEKIT_API_KEY || "").trim());
  const hasLiveKitSecret = Boolean((process.env.LIVEKIT_API_SECRET || "").trim());
  const firebaseProjectId = (process.env.FIREBASE_PROJECT_ID || DEFAULT_FIREBASE_PROJECT_ID).trim();
  const hasFirebaseClientEmail = Boolean((process.env.FIREBASE_CLIENT_EMAIL || "").trim());
  const hasFirebasePrivateKey = Boolean((process.env.FIREBASE_PRIVATE_KEY || "").trim());

  const ok = hasLiveKitUrl && hasLiveKitKey && hasLiveKitSecret && Boolean(firebaseProjectId);

  return NextResponse.json(
    {
      ok,
      service: "noice-live-livekit",
      firebaseVerificationMode:
        hasFirebaseClientEmail && hasFirebasePrivateKey
          ? "firebase-admin-service-account"
          : "google-public-x509-jwks",
      config: {
        LIVEKIT_URL: hasLiveKitUrl,
        LIVEKIT_API_KEY: hasLiveKitKey,
        LIVEKIT_API_SECRET: hasLiveKitSecret,
        FIREBASE_PROJECT_ID: Boolean(firebaseProjectId),
        FIREBASE_CLIENT_EMAIL: hasFirebaseClientEmail,
        FIREBASE_PRIVATE_KEY: hasFirebasePrivateKey,
      },
    },
    {
      status: ok ? 200 : 503,
      headers: CORS_HEADERS,
    }
  );
}
