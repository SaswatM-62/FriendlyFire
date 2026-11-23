"use client";

import { use } from "react";
import { SignedIn } from "@/components/common/SignedIn";
import { QuizDetail } from "@/components/history/QuizDetail";

export function QuizDetailRoute({ params }: { params: Promise<{ quizId: string }> }) {
  const { quizId } = use(params);
  return <SignedIn what="this quiz">{(me) => <QuizDetail quizId={quizId} me={me} />}</SignedIn>;
}
