import type { LatestReportRoute } from "./types";

const REPORT_PROGRESS_KEY = "lift.reportProgress";

type StoredReportProgress = {
  path: string;
  savedAt: string;
};

export function rememberReportProgress(pathname: string, query = ""): void {
  if (typeof window === "undefined") return;
  const path = reportProgressPath(pathname, query);
  if (!path) return;

  window.localStorage.setItem(
    REPORT_PROGRESS_KEY,
    JSON.stringify({ path, savedAt: new Date().toISOString() } satisfies StoredReportProgress),
  );
}

export function clearReportProgress(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(REPORT_PROGRESS_KEY);
}

export function resolveReportNavigationPath(target?: LatestReportRoute | null): string {
  if (target?.available && target.reportId && target.paymentStatus === "PAID") {
    return `/report/${target.reportId}`;
  }

  const stored = readStoredReportProgress();
  if (stored) {
    return stored.path;
  }

  if (target?.available && target.reportId) {
    return `/checkout?reportId=${target.reportId}`;
  }

  return "/onboarding/life-event";
}

function readStoredReportProgress(): StoredReportProgress | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(REPORT_PROGRESS_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as Partial<StoredReportProgress>;
    if (typeof parsed.path === "string" && parsed.path.startsWith("/")) {
      return { path: parsed.path, savedAt: parsed.savedAt ?? "" };
    }
  } catch {
    window.localStorage.removeItem(REPORT_PROGRESS_KEY);
  }
  return null;
}

function reportProgressPath(pathname: string, query: string): string | null {
  if (pathname === "/onboarding/life-event") return pathname;
  if (pathname === "/assessment/new") return pathname;
  if (/^\/report\/[^/]+\/preview$/.test(pathname)) return pathname;
  if (pathname === "/checkout") return query ? `${pathname}?${query}` : pathname;
  if (/^\/report\/[^/]+$/.test(pathname)) return pathname;
  return null;
}
