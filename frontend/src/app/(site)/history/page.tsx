import type { Metadata } from "next";
import Link from "next/link";
import { PageHead } from "@/components/common/PageHead";
import { HistoryRoute } from "@/components/history/HistoryRoute";
import { Icon } from "@/components/ui/Icon";

export const metadata: Metadata = { title: "History" };

export default function HistoryPage() {
  return (
    <div className="hist">
      <PageHead
        above={
          <Link className="row small muted" href="/profile" style={{ textDecoration: "none", gap: 6 }}>
            <Icon name="arrowLeft" size={16} />
            Profile
          </Link>
        }
        title="History"
        subtitle="Your quizzes, newest first."
      />
      <HistoryRoute />
    </div>
  );
}
