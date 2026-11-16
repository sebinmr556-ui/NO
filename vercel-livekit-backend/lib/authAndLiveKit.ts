import "server-only";
import { NextRequest, NextResponse } from "next/server";
import { AccessToken, RoomServiceClient, VideoGrant } from "livekit-server-sdk";
import type { Auth } from "firebase-admin/auth";
import crypto from "crypto";

export const DEFAULT_FIREBASE_PROJECT_ID = "project-7eddf9da-6480-4ac9-a2d";

export const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Firebase-Uid",
};

export function handleCorsOptions(): NextResponse {
  return new NextResponse(null, { status: 204, headers: CORS_HEADERS });
}

export function getFirebaseProjectId(): string {
  return (process.env.FIREBASE_PROJECT_ID || DEFAULT_FIREBASE_PROJECT_ID).trim();
}

/**
 * Dynamically imports `firebase-admin/app` and `firebase-admin/auth` on the server only.
 * Avoids top-level CommonJS `require('jose')` (`ERR_REQUIRE_ESM`) during module initialization.
 */
async function getFirebaseAdminAuth(): Promise<Auth | null> {
  const projectId = getFirebaseProjectId();
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL?.trim();
  const privateKey = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n")?.trim();

  if (!projectId || !clientEmail || !privateKey) {
    return null;
  }

  try {
    const appModule = await import("firebase-admin/app");
    const authModule = await import("firebase-admin/auth");
    const existingApps = appModule.getApps();
    const app =
      existingApps.length > 0
        ? existingApps[0]
        : appModule.initializeApp({
            credential: appModule.cert({
              projectId,
              clientEmail,
              privateKey,
            }),
          });
    return authModule.getAuth(app);
  } catch (err) {
    console.warn("firebase-admin dynamic import fallback to Google x509 public certs:", err);
    return null;
  }
}

let cachedGoogleCerts: { certs: Record<string, string>; expiresAt: number } | null = null;

async function fetchGooglePublicCerts(): Promise<Record<string, string>> {
  if (process.env.FIREBASE_LOCAL_TEST_CERTS_JSON) {
    try {
      return JSON.parse(process.env.FIREBASE_LOCAL_TEST_CERTS_JSON) as Record<string, string>;
    } catch {
      // ignore invalid test certs JSON
    }
  }
  const now = Date.now();
  if (cachedGoogleCerts && cachedGoogleCerts.expiresAt > now) {
    return cachedGoogleCerts.certs;
  }
  const resp = await fetch(
    "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com",
    { cache: "no-store" }
  );
  if (!resp.ok) {
    throw new Error(`Unable to fetch Google public x509 certs: HTTP ${resp.status}`);
  }
  const certs = (await resp.json()) as Record<string, string>;
  cachedGoogleCerts = {
    certs,
    expiresAt: now + 50 * 60 * 1000,
  };
  return certs;
}

function decodeBase64UrlJson<T>(segment: string): T {
  const normalized = segment.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
  return JSON.parse(Buffer.from(padded, "base64").toString("utf8")) as T;
}

export async function verifyFirebaseJwtWithGooglePublicCerts(
  idToken: string,
  expectedProjectId: string
): Promise<{ uid: string; name?: string; email?: string }> {
  const parts = idToken.split(".");
  if (parts.length !== 3) {
    throw new Error("Malformed Firebase ID token");
  }

  const header = decodeBase64UrlJson<{ alg?: string; kid?: string }>(parts[0]);
  const payload = decodeBase64UrlJson<{
    aud?: string;
    iss?: string;
    sub?: string;
    user_id?: string;
    name?: string;
    email?: string;
    exp?: number;
    iat?: number;
  }>(parts[1]);

  if (header.alg !== "RS256" || !header.kid) {
    throw new Error("Unsupported Firebase JWT algorithm or missing kid");
  }

  const nowSec = Math.floor(Date.now() / 1000);
  if (!payload.exp || payload.exp < nowSec - 30) {
    throw new Error("Firebase ID token has expired");
  }
  if (payload.aud !== expectedProjectId) {
    throw new Error(
      `Invalid Firebase token audience: expected ${expectedProjectId}, got ${payload.aud}`
    );
  }
  if (payload.iss !== `https://securetoken.google.com/${expectedProjectId}`) {
    throw new Error(`Invalid Firebase token issuer: ${payload.iss}`);
  }
  const uid = (payload.sub || payload.user_id || "").trim();
  if (!uid) {
    throw new Error("Firebase ID token missing sub/uid");
  }

  const certs = await fetchGooglePublicCerts();
  const certPem = certs[header.kid];
  if (!certPem) {
    throw new Error(`Unknown Google signing key kid: ${header.kid}`);
  }

  const verifier = crypto.createVerify("RSA-SHA256");
  verifier.update(`${parts[0]}.${parts[1]}`);
  verifier.end();

  const sigNormalized = parts[2].replace(/-/g, "+").replace(/_/g, "/");
  const sigBuffer = Buffer.from(sigNormalized, "base64");
  const isValid = verifier.verify(certPem, sigBuffer);
  if (!isValid) {
    throw new Error("Firebase ID token signature verification failed");
  }

  return {
    uid,
    name: payload.name,
    email: payload.email,
  };
}

export async function verifyRequestFirebaseUser(
  req: NextRequest
): Promise<{ uid: string; name?: string; email?: string }> {
  const authHeader = req.headers.get("authorization") || req.headers.get("Authorization") || "";
  if (!authHeader.startsWith("Bearer ")) {
    throw new Error("Missing or invalid Authorization Bearer token");
  }
  const idToken = authHeader.slice("Bearer ".length).trim();
  if (!idToken) {
    throw new Error("Empty Firebase ID token");
  }

  const adminAuth = await getFirebaseAdminAuth();
  if (adminAuth) {
    try {
      const decoded = await adminAuth.verifyIdToken(idToken);
      return {
        uid: decoded.uid,
        name: (decoded.name as string | undefined) || decoded.email,
        email: decoded.email,
      };
    } catch (err: unknown) {
      const code = (err as { code?: string })?.code || "";
      const message = err instanceof Error ? err.message : String(err);
      // Only fall back to Google x509 cert verification if an ESM/module loader error occurred;
      // rethrow real Firebase Auth token validation errors (expired, revoked, invalid argument).
      const isModuleLoaderError =
        code === "ERR_REQUIRE_ESM" ||
        code === "MODULE_NOT_FOUND" ||
        message.includes("ERR_REQUIRE_ESM") ||
        message.includes("Failed to load external module");
      if (!isModuleLoaderError) {
        throw err;
      }
      console.warn("Falling back to Google x509 verification due to module loader error:", message);
    }
  }

  return verifyFirebaseJwtWithGooglePublicCerts(idToken, getFirebaseProjectId());
}

export function getLiveKitConfig(): {
  livekitUrl: string;
  apiKey: string;
  apiSecret: string;
} {
  const livekitUrl = (process.env.LIVEKIT_URL || process.env.NEXT_PUBLIC_LIVEKIT_URL || "").trim();
  const apiKey = (process.env.LIVEKIT_API_KEY || "").trim();
  const apiSecret = (process.env.LIVEKIT_API_SECRET || "").trim();

  if (!livekitUrl || !apiKey || !apiSecret) {
    throw new Error(
      "Server LiveKit configuration missing (LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET)"
    );
  }
  return { livekitUrl, apiKey, apiSecret };
}

export function getLiveKitRoomServiceClient(): RoomServiceClient {
  const { livekitUrl, apiKey, apiSecret } = getLiveKitConfig();
  const httpUrl = livekitUrl.replace(/^wss:\/\//i, "https://").replace(/^ws:\/\//i, "http://");
  return new RoomServiceClient(httpUrl, apiKey, apiSecret);
}

export async function createSignedLiveKitToken(params: {
  roomName: string;
  identity: string;
  participantName: string;
  canPublish?: boolean;
  canSubscribe?: boolean;
  canPublishData?: boolean;
  metadata?: string;
}): Promise<{
  wsUrl: string;
  token: string;
  serverUrl: string;
  url: string;
  roomName: string;
  identity: string;
  participantName: string;
  expiresInSeconds: number;
}> {
  const { livekitUrl, apiKey, apiSecret } = getLiveKitConfig();
  const ttlSeconds = 3600;

  const at = new AccessToken(apiKey, apiSecret, {
    identity: params.identity,
    name: params.participantName,
    ttl: ttlSeconds,
    metadata: params.metadata,
  });

  const grant: VideoGrant = {
    room: params.roomName,
    roomJoin: true,
    canPublish: params.canPublish ?? true,
    canSubscribe: params.canSubscribe ?? true,
    canPublishData: params.canPublishData ?? true,
  };
  at.addGrant(grant);

  const jwt = await at.toJwt();
  return {
    wsUrl: livekitUrl,
    token: jwt,
    serverUrl: livekitUrl,
    url: livekitUrl,
    roomName: params.roomName,
    identity: params.identity,
    participantName: params.participantName,
    expiresInSeconds: ttlSeconds,
  };
}
