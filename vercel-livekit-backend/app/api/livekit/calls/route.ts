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

  const targetUserId = (typeof body.targetUserId === "string" ? body.targetUserId : "").trim();
  const callId = (typeof body.callId === "string" ? body.callId : "").trim();
  const requestedRoomName = (typeof body.roomName === "string" ? body.roomName : "").trim();

  const sortedPair = [verifiedUser.uid, targetUserId || "peer"].sort().join("_");
  const rawRoom = requestedRoomName || (callId ? `call_${callId}` : `call_${sortedPair}`);
  const roomName = rawRoom.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 128);

  const participantName = (
    (typeof body.participantName === "string" ? body.participantName : "") ||
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
      canPublish: true,
      canSubscribe: true,
      canPublishData: true,
      metadata: JSON.stringify({
        uid: verifiedUser.uid,
        username: participantName,
        callType: typeof body.callType === "string" ? body.callType : "audio",
        targetUserId,
      }),
    });

    return NextResponse.json(tokenPayload, { status: 200, headers: CORS_HEADERS });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Failed to create 1-on-1 call token";
    return NextResponse.json({ error: message }, { status: 500, headers: CORS_HEADERS });
  }
}
