"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AppShell } from "@/components/AppShell";
import { AuthGuard } from "@/components/AuthGuard";
import { api, ApiError } from "@/lib/api";

function TossSuccessInner() {
  const router = useRouter();
  const params = useSearchParams();
  const reportId = Number(params.get("reportId"));
  const paymentKey = params.get("paymentKey");
  const orderId = params.get("orderId");
  const amount = Number(params.get("amount"));
  const [error, setError] = useState<string | null>(null);
  const confirmingRef = useRef(false);

  useEffect(() => {
    if (confirmingRef.current) return;

    if (!reportId || !paymentKey || !orderId || !amount) {
      setError("토스 결제 승인에 필요한 값이 부족합니다.");
      return;
    }

    confirmingRef.current = true;
    api
      .confirmTossPayment(reportId, { paymentKey, orderId, amount })
      .then(() => router.replace(`/report/${reportId}`))
      .catch((e) => {
        confirmingRef.current = false;
        setError(e instanceof ApiError ? e.message : "토스 결제 승인 중 문제가 발생했어요.");
      });
  }, [amount, orderId, paymentKey, reportId, router]);

  return (
    <>
      <p className="step-hint">STEP 4 · 토스 테스트 결제</p>
      <h1 className="page-title">결제 승인 확인 중</h1>
      <p className="page-sub">토스 결제 인증 결과를 서버에서 승인하고 있어요.</p>

      {error ? (
        <>
          <div className="error-box">{error}</div>
          <div className="sticky-cta" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <button type="button" className="btn" onClick={() => router.replace(`/checkout?reportId=${reportId}`)}>
              결제 다시 시도
            </button>
            <button type="button" className="btn secondary" onClick={() => router.replace(`/report/${reportId}/preview`)}>
              미리보기로 돌아가기
            </button>
          </div>
        </>
      ) : (
        <div className="center-state">승인 요청 중…</div>
      )}
    </>
  );
}

export default function TossSuccessPage() {
  return (
    <AppShell showLogout>
      <AuthGuard>
        <Suspense fallback={<div className="center-state">불러오는 중…</div>}>
          <TossSuccessInner />
        </Suspense>
      </AuthGuard>
    </AppShell>
  );
}
