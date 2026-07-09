"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { isLoggedIn } from "@/lib/auth";
import { resolveReportNavigationPath } from "@/lib/reportProgress";

export default function RootPage() {
  const router = useRouter();

  useEffect(() => {
    if (!isLoggedIn()) {
      router.replace("/login");
      return;
    }

    api
      .getLatestReportRoute()
      .then((target) => {
        router.replace(resolveReportNavigationPath(target));
      })
      .catch(() => router.replace("/login"));
  }, [router]);

  return <div className="center-state">이동 중…</div>;
}
