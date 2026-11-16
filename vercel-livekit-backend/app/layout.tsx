import React from "react";

export const metadata = {
  title: "NOICE LIVE — LiveKit Token Service",
  description: "Secure LiveKit token backend for NOICE LIVE voice calls, video calls, and group voice rooms.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body style={{ margin: 0, fontFamily: "system-ui, sans-serif", background: "#050507", color: "#F9F9FB" }}>
        {children}
      </body>
    </html>
  );
}
