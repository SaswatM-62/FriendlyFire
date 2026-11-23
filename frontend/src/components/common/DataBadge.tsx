import { Icon } from "@/components/ui/Icon";
import type { DataStatus } from "@/lib/api-types";
import { formatEstimate } from "@/lib/format";

/** A player's match-data status as a tag: Synced, Syncing · ~3 min, Not linked... */
export function DataBadge({ status, estimatedSeconds }: { status: DataStatus; estimatedSeconds?: number | null }) {
  switch (status) {
    case "SYNCED":
      return (
        <span className="tag tag--mint">
          <Icon name="check" size={12} />
          Synced
        </span>
      );
    case "SYNCING":
      return (
        <span className="tag tag--yellow">
          <Icon name="clock" size={12} />
          Syncing · {formatEstimate(estimatedSeconds)}
        </span>
      );
    case "NOT_SYNCED":
      return (
        <span className="tag tag--yellow">
          <Icon name="sync" size={12} />
          Needs a refresh
        </span>
      );
    case "FAILED":
      return (
        <span className="tag tag--red">
          <Icon name="alert" size={12} />
          Sync failed
        </span>
      );
    case "NOT_LINKED":
      return (
        <span className="tag tag--red">Not linked
        </span>
      );
  }
}
