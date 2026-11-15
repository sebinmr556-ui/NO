import { NextRequest, NextResponse } from "next/server";
import {
  CORS_HEADERS,
  createSignedLiveKitToken,
  handleCorsOptions,
  verifyRequestFirebaseUser,
} from "../../../../lib/authAndLiveKit";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function OPTIONS() {
  return handleCorsOptions();
}

export async function POST(req: NextRequest) {
  let verifiedUser: { uid: string; name?: string; email?: string };
  try {
    verifiedUser = await verifyRequestFirebaseUser(req);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Unauthorized";
    return NextResponse.json(
      { error: `Invalid or expired Firebase ID token: ${message}` },
      { status: 401, headers: CORS_HEADERS }
    );
  }

  let body: Record<string, unknown> = {};
  try {
    body = (await req.json()) as Record<string, unknown>;
  } catch {
    body = {};
  }

  const rawRoom =
    (typeof body.roomName === "string" ? body.roomName : "") ||
    (typeof body.room === "string" ? body.room : "") ||
    (typeof body.roomId === "string" ? body.roomId : "");
  const roomName = rawRoom.trim().replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 128);

  if (!roomName) {
    return NextResponse.json(
      { error: "roomName is required" },
      { status: 400, headers: CORS_HEADERS }
    );
  }

  const requestedIdentity = typeof body.identity === "string" ? body.identity.trim() : "";
  if (requestedIdentity && requestedIdentity !== verifiedUser.uid) {
    return NextResponse.json(
      { error: "Requested identity does not match authenticated Firebase UID" },
      { status: 403, headers: CORS_HEADERS }
    );
  }

  const participantName = (
    (typeof body.participantName === "string" ? body.participantName : "") ||
    (typeof body.username === "string" ? body.username : "") ||
    verifiedUser.name ||
    verifiedUser.email ||
    verifiedUser.uid
  )
    .trim()
    .slice(0, 60);

  try {
    const tokenPayload = await createSignedLiveKitToken({
      roomName,
      identity: verifiedUser.uid,
      participantName,
      canPublish: typeof body.canPublish === "boolean" ? body.canPublish : true,
      canSubscribe: typeof body.canSubscribe === "boolean" ? body.canSubscribe : true,
      canPublishData: typeof body.canPublishData === "boolean" ? body.canPublishData : true,
      metadata: JSON.stringify({
        uid: verifiedUser.uid,
        username: participantName,
      }),
    });
    return NextResponse.json(tokenPayload, { status: 200, headers: CORS_HEADERS });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Failed to generate LiveKit token";
    return NextResponse.json({ error: message }, { status: 500, headers: CORS_HEADERS });
  }
}

export async function GET(req: NextRequest) {
  let verifiedUser: { uid: string; name?: string; email?: string };
  try {
    verifiedUser = await verifyRequestFirebaseUser(req);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Unauthorized";
    return NextResponse.json(
      { error: `Invalid or expired Firebase ID token: ${message}` },
      { status: 401, headers: CORS_HEADERS }
    );
  }

  const { searchParams } = new URL(req.url);
  const rawRoom =
    searchParams.get("roomName") || searchParams.get("room") || searchParams.get("roomId") || "";
  const roomName = rawRoom.trim().replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 128);

  if (!roomName) {
    return NextResponse.json(
      { error: "roomName query parameter is required" },
      { status: 400, headers: CORS_HEADERS }
    );
  }

  const participantName = (
    searchParams.get("participantName") ||
    searchParams.get("username") ||
    verifiedUser.name ||
    verifiedUser.uid
  )
    .trim()
    .slice(0, 60);

  try {
    const tokenPayload = await createSignedLiveKitToken({
      roomName,
      identity: verifiedUser.uid,
      participantName,
    });
    return NextResponse.json(tokenPayload, { status: 200, headers: CORS_HEADERS });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Failed to generate LiveKit token";
    return NextResponse.json({ error: message }, { status: 500, headers: CORS_HEADERS });
  }
}
