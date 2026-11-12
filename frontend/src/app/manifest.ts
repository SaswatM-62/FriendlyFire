import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "FriendlyFire",
    short_name: "FriendlyFire",
    description: "Trivia built from your friends' own VALORANT matches.",
    start_url: "/",
    display: "standalone",
    background_color: "#16121F",
    theme_color: "#16121F",
    icons: [
      { src: "/brand/ff-logo-192.png", sizes: "192x192", type: "image/png" },
      { src: "/brand/ff-logo-512.png", sizes: "512x512", type: "image/png" },
    ],
  };
}
