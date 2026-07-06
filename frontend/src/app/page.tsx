"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { isLoggedIn } from "@/lib/auth";

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
        if (!target.available || !target.reportId) {
          router.replace("/onboarding/life-event");
          return;
        }

        router.replace(
          target.paymentStatus === "PAID"
            ? `/report/${target.reportId}`
            : `/checkout?reportId=${target.reportId}`,
        );
      })
      .catch(() => router.replace("/login"));
  }, [router]);

  return <div className="center-state">이동 중…</div>;
}
