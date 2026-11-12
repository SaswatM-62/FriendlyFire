import { Chakra_Petch, Inter, Orbitron, Russo_One } from "next/font/google";

// The four Neon Arcade typefaces (FF-501), self-hosted by next/font. Each one is a CSS
// variable that src/styles/tokens.css builds the --font-* tokens from.
const orbitron = Orbitron({ subsets: ["latin"], weight: ["700", "800", "900"], variable: "--font-orbitron" });
const russo = Russo_One({ subsets: ["latin"], weight: "400", variable: "--font-russo" });
const chakra = Chakra_Petch({ subsets: ["latin"], weight: ["500", "600", "700"], variable: "--font-chakra" });
const inter = Inter({ subsets: ["latin"], weight: ["400", "500", "600", "700"], variable: "--font-inter" });

export const fontVariables = [orbitron.variable, russo.variable, chakra.variable, inter.variable].join(" ");
