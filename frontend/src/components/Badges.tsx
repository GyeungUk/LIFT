import type { EligibilityLevel, PriorityLevel } from "@/lib/types";
import {
  eligibilityLabel,
  eligibilityTone,
  priorityLabel,
  priorityTone,
} from "@/lib/labels";

export function EligibilityBadge({ level }: { level: EligibilityLevel }) {
  return <span className={`badge ${eligibilityTone(level)}`}>{eligibilityLabel[level]}</span>;
}

export function PriorityBadge({ level }: { level: PriorityLevel }) {
  return <span className={`badge ${priorityTone(level)}`}>{priorityLabel[level]}</span>;
}
