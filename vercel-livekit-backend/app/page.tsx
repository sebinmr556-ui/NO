import React from "react";

export default function HomePage() {
  return (
    <main style={{ maxWidth: 720, margin: "0 auto", padding: "48px 24px" }}>
      <h1 style={{ fontSize: 24, fontWeight: 700, marginBottom: 8 }}>
        NOICE LIVE — LiveKit Token Service
      </h1>
      <p style={{ color: "#9E9EB0", fontSize: 14, marginBottom: 24 }}>
        Secure Node.js Serverless LiveKit token & room management backend authenticated via Firebase Admin SDK.
      </p>
      <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 12 }}>
        <li><code>GET /api/health</code> — Service & environment health check</li>
        <li><code>POST /api/livekit/rooms</code> — Create a group voice room & issue host LiveKit token</li>
        <li><code>GET /api/livekit/rooms</code> — List active joinable group voice rooms</li>
        <li><code>GET /api/livekit/rooms/:roomName</code> — Room details & participant list</li>
        <li><code>POST /api/livekit/rooms/:roomName/token</code> — Join an existing group voice room</li>
        <li><code>DELETE /api/livekit/rooms/:roomName</code> — Host closes room</li>
        <li><code>POST /api/livekit/token</code> — Issue room-scoped LiveKit token</li>
        <li><code>POST /api/livekit/calls</code> — Start or answer a 1-to-1 voice or video call</li>
      </ul>
    </main>
  );
}
