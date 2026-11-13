import type { Metadata } from "next";
import { Suspense } from "react";
import { QuizDetailRoute } from "@/components/history/QuizDetailRoute";
import { PageSkeleton } from "@/components/ui/States";

export const metadata: Metadata = { title: "Quiz results" };

export default function QuizPage({ params }: PageProps<"/history/[quizId]">) {
  return (
    <>
      <Suspense fallback={<PageSkeleton />}>
        <QuizDetailRoute params={params} />
      </Suspense>
    </>
  );
}
