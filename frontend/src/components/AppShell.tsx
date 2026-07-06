"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { clearToken } from "@/lib/auth";

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

  function handleLogout() {
    clearToken();
    router.replace("/login");
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
