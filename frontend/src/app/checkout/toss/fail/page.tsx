"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AppShell } from "@/components/AppShell";
import { AuthGuard } from "@/components/AuthGuard";

function TossFailInner() {
  const router = useRouter();
  const params = useSearchParams();
  const reportId = Number(params.get("reportId"));
  const code = params.get("code");
  const message = params.get("message");

  return (
    <>
      <p className="step-hint">STEP 4 · 토스 테스트 결제</p>
      <h1 className="page-title">결제가 완료되지 않았어요</h1>
      <p className="page-sub">
        {message || "결제창이 취소되었거나 테스트 결제 인증에 실패했습니다."}
      </p>
      {code && <div className="error-box">토스 오류 코드: {code}</div>}

      <div className="sticky-cta" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
        <button type="button" className="btn" onClick={() => router.replace(`/checkout?reportId=${reportId}`)}>
          결제 다시 시도
        </button>
        <button type="button" className="btn secondary" onClick={() => router.replace(`/report/${reportId}/preview`)}>
          미리보기로 돌아가기
        </button>
      </div>
    </>
  );
}

export default function TossFailPage() {
  return (
    <AppShell showLogout>
      <AuthGuard>
        <Suspense fallback={<div className="center-state">불러오는 중…</div>}>
          <TossFailInner />
        </Suspense>
      </AuthGuard>
    </AppShell>
  );
}
