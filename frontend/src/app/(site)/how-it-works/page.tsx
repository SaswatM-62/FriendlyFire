import type { Metadata } from "next";
import { HowItWorks } from "@/components/home/HowItWorks";

export const metadata: Metadata = {
  title: "How it works",
  description: "Trivia about your squad, built from your own VALORANT matches: how a quiz works, how scoring works and what data we use.",
};

export default function HowItWorksPage() {
  return <HowItWorks />;
}
