import { NextRequest, NextResponse } from "next/server";
import {
  CORS_HEADERS,
  createSignedLiveKitToken,
  getLiveKitRoomServiceClient,
  handleCorsOptions,
  verifyRequestFirebaseUser,
} from "../../../../lib/authAndLiveKit";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function OPTIONS() {
  return handleCorsOptions();
}

export async function GET(req: NextRequest) {
  try {
    await verifyRequestFirebaseUser(req);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Unauthorized";
    return NextResponse.json(
      { error: `Invalid or expired Firebase ID token: ${message}` },
      { status: 401, headers: CORS_HEADERS }
    );
  }

  try {
    const roomService = getLiveKitRoomServiceClient();
    const rooms = await roomService.listRooms();
    return NextResponse.json(
      {
        rooms: rooms.map((r) => ({
          name: r.name,
          sid: r.sid,
          numParticipants: r.numParticipants,
          maxParticipants: r.maxParticipants,
          creationTime: Number(r.creationTime),
        })),
      },
      { status: 200, headers: CORS_HEADERS }
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Failed to list LiveKit rooms";
    return NextResponse.json({ error: message }, { status: 500, headers: CORS_HEADERS });
  }
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
    (typeof body.name === "string" ? body.name : "") ||
    `room_${verifiedUser.uid}_${Date.now()}`;
  const roomName = rawRoom.trim().replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 128);

  const participantName = (
    (typeof body.participantName === "string" ? body.participantName : "") ||
    verifiedUser.name ||
    verifiedUser.uid
  )
    .trim()
    .slice(0, 60);

  try {
    try {
      const roomService = getLiveKitRoomServiceClient();
      await roomService.createRoom({
        name: roomName,
        emptyTimeout: 600,
        maxParticipants: typeof body.maxParticipants === "number" ? body.maxParticipants : 50,
      });
    } catch {
      // Room is also auto-created on first participant join when roomJoin=true
    }

    const tokenPayload = await createSignedLiveKitToken({
      roomName,
      identity: verifiedUser.uid,
      participantName,
      metadata: JSON.stringify({
        uid: verifiedUser.uid,
        username: participantName,
        isPrivate: Boolean(body.isPrivate),
      }),
    });

    return NextResponse.json(tokenPayload, { status: 200, headers: CORS_HEADERS });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Failed to create LiveKit room";
    return NextResponse.json({ error: message }, { status: 500, headers: CORS_HEADERS });
  }
}
