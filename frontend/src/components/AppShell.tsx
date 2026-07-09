"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { clearToken } from "@/lib/auth";
import { rememberReportProgress, resolveReportNavigationPath } from "@/lib/reportProgress";

export function AppShell({
  children,
  showLogout = false,
  wide = false,
}: {
  children: React.ReactNode;
  showLogout?: boolean;
  wide?: boolean;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const [reportNavigating, setReportNavigating] = useState(false);

  useEffect(() => {
    rememberReportProgress(pathname, window.location.search.replace(/^\?/, ""));
  }, [pathname]);

  function handleLogout() {
    clearToken();
    router.replace("/login");
  }

  async function handleReportNavigation() {
    setReportNavigating(true);
    try {
      const target = await api.getLatestReportRoute();
      router.push(resolveReportNavigationPath(target));
    } catch {
      router.push(resolveReportNavigationPath(null));
    } finally {
      setReportNavigating(false);
    }
  }

  function isActive(path: string) {
    return pathname === path || pathname.startsWith(`${path}/`);
  }

  const brandHref = showLogout ? "/" : "/login";

  return (
    <div className={`app-shell ${wide ? "wide" : ""}`}>
      <div className="topbar">
        <Link href={brandHref} className="brand" style={{ textDecoration: "none" }}>
          LIFT
        </Link>
        {showLogout && (
          <div className="top-actions">
            <Link href="/my" className={`top-link ${isActive("/my") ? "active" : ""}`}>
              내 정보
            </Link>
            <button
              type="button"
              className={`top-link top-link-button ${
                isActive("/onboarding/life-event") ||
                isActive("/assessment/new") ||
                isActive("/report") ||
                isActive("/checkout")
                  ? "active"
                  : ""
              }`}
              disabled={reportNavigating}
              onClick={handleReportNavigation}
            >
              {reportNavigating ? "이동 중" : "보고서"}
            </button>
            <Link
              href="/community"
              className={`top-link ${isActive("/community") ? "active" : ""}`}
            >
              커뮤니티
            </Link>
            <Link href="/chat" className={`top-link ${isActive("/chat") ? "active" : ""}`}>
              AI챗봇
            </Link>
            <button type="button" className="link-btn" onClick={handleLogout}>
              로그아웃
            </button>
          </div>
        )}
      </div>
      {children}
    </div>
  );
}
