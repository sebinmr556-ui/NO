import { NextRequest, NextResponse } from "next/server";
import {
  CORS_HEADERS,
  getLiveKitRoomServiceClient,
  handleCorsOptions,
  verifyRequestFirebaseUser,
} from "../../../../../lib/authAndLiveKit";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function OPTIONS() {
  return handleCorsOptions();
}

export async function DELETE(
  req: NextRequest,
  context: { params: Promise<{ roomName: string }> }
) {
  try {
    await verifyRequestFirebaseUser(req);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Unauthorized";
    return NextResponse.json(
      { error: `Invalid or expired Firebase ID token: ${message}` },
      { status: 401, headers: CORS_HEADERS }
    );
  }

  const resolvedParams = await context.params;
  const roomName = (resolvedParams.roomName || "")
    .trim()
    .replace(/[^a-zA-Z0-9_-]/g, "_")
    .slice(0, 128);

  if (!roomName) {
    return NextResponse.json(
      { error: "roomName is required" },
      { status: 400, headers: CORS_HEADERS }
    );
  }

  try {
    const roomService = getLiveKitRoomServiceClient();
    await roomService.deleteRoom(roomName);
    return NextResponse.json({ deleted: true, roomName }, { status: 200, headers: CORS_HEADERS });
  } catch {
    return NextResponse.json({ deleted: true, roomName }, { status: 200, headers: CORS_HEADERS });
  }
}
