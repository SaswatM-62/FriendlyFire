import { ImageResponse } from "next/og";

export const alt = "FriendlyFire: how well do you know your squad?";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

/** The link preview card (FF-514): the crosshair and the tagline on the dark stage. */
export default function OpenGraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          alignItems: "center",
          gap: 64,
          padding: "0 96px",
          background: "radial-gradient(120% 70% at 50% 100%, #3a2a5c 0%, #16121F 60%)",
          color: "#F4F1FF",
        }}
      >
        <svg width="280" height="280" viewBox="0 0 512 512">
          <rect width="512" height="512" rx="112" fill="#16121F" />
          <g fill="none" stroke="#3DFF8A" strokeWidth="22" strokeLinecap="round">
            <circle cx="256" cy="256" r="166" />
            <path d="M256 44V120M256 392V468M44 256H120M392 256H468" />
          </g>
          <path d="M155 193H247V224H187V247H235V276H187V327H155Z M280 193H372V224H312V247H360V276H312V327H280Z" fill="#FF8A2E" />
        </svg>
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          <div style={{ display: "flex", fontSize: 76, fontWeight: 800, letterSpacing: 2 }}>
            <span style={{ color: "#3DFF8A" }}>Friendly</span>
            <span style={{ color: "#FF8A2E" }}>Fire</span>
          </div>
          <div style={{ fontSize: 40, lineHeight: 1.25, maxWidth: 660 }}>How well do you know your squad?</div>
          <div style={{ fontSize: 28, color: "#B9AFD6", maxWidth: 660 }}>Trivia built from your friends&apos; VALORANT matches.</div>
        </div>
      </div>
    ),
    size,
  );
}
