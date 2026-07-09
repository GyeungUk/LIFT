"use client";

import { Suspense, use, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AuthGuard } from "@/components/AuthGuard";
import { ReportPdfDocument } from "@/components/ReportPdfDocument";
import { api, ApiError } from "@/lib/api";
import {
  createPdfBlobFromElement,
  openPdfFallbackWindow,
  safePdfFilename,
  savePdfBlob,
} from "@/lib/pdfExport";
import type { ReportDetail } from "@/lib/types";

function parseWage(value: string | null): number | null {
  if (!value) return null;
  const amount = Number(value.replace(/[^\d]/g, ""));
  return Number.isFinite(amount) && amount > 0 ? amount : null;
}

/**
 * 카카오톡·네이버 등 인앱 브라우저(WebView)에서는 window.print()가 조용히 무시돼
 * 모바일에서 "PDF 저장이 됐다 안 됐다" 하는 원인이 된다. 감지해서 외부 브라우저로 안내한다.
 */
type InAppBrowser = "kakaotalk" | "naver" | "other";

function detectInAppBrowser(): InAppBrowser | null {
  if (typeof navigator === "undefined") return null;
  const ua = navigator.userAgent;
  if (/KAKAOTALK/i.test(ua)) return "kakaotalk";
  if (/NAVER\(inapp/i.test(ua)) return "naver";
  if (/Instagram|FBAN|FBAV|FB_IAB|Line\/|DaumApps|everytimeApp|; wv\)/i.test(ua)) return "other";
  return null;
}

function openInExternalBrowser(inApp: InAppBrowser) {
  const url = window.location.href;
  if (inApp === "kakaotalk") {
    // 카카오톡 인앱 브라우저 공식 스킴: 기본 브라우저로 현재 페이지를 연다.
    window.location.href = `kakaotalk://web/openExternal?url=${encodeURIComponent(url)}`;
    return;
  }
  if (/android/i.test(navigator.userAgent)) {
    // 안드로이드 인앱 → 크롬 intent로 탈출 시도
    const stripped = url.replace(/^https?:\/\//, "");
    window.location.href = `intent://${stripped}#Intent;scheme=https;package=com.android.chrome;end`;
    return;
  }
  // iOS 등 스킴 지원이 없는 경우: 주소 복사 안내용으로 클립보드에 담아둔다.
  void navigator.clipboard?.writeText(url);
  alert("주소가 복사되었어요. Safari/Chrome 주소창에 붙여넣어 열어주세요.");
}

function ReportPdfInner({ reportId }: { reportId: number }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const exportRef = useRef<HTMLDivElement>(null);
  const [report, setReport] = useState<ReportDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [preparing, setPreparing] = useState(false);
  const [inAppBrowser, setInAppBrowser] = useState<InAppBrowser | null>(null);
  // 미리 만들어 둔 PDF Blob. 사용자가 "저장"을 누르는 즉시(제스처 안에서) 공유/저장하려면
  // Blob이 이미 준비돼 있어야 iOS 공유 시트가 안정적으로 뜬다.
  const pdfBlobRef = useRef<Blob | null>(null);
  const monthlyAverageWage = parseWage(searchParams.get("monthlyAverageWage"));

  useEffect(() => {
    setInAppBrowser(detectInAppBrowser());
  }, []);

  // 리포트가 준비되면 백그라운드에서 PDF Blob을 미리 만들어 둔다.
  useEffect(() => {
    if (!report || inAppBrowser) return;
    let cancelled = false;
    pdfBlobRef.current = null;
    setPreparing(true);
    const frame = window.requestAnimationFrame(async () => {
      const target = exportRef.current?.querySelector<HTMLElement>(".pdf-page");
      if (!target) {
        if (!cancelled) setPreparing(false);
        return;
      }
      try {
        const blob = await createPdfBlobFromElement(target);
        if (!cancelled) pdfBlobRef.current = blob;
      } catch {
        // 사전 생성 실패는 조용히 무시하고, 저장 시 즉석 생성으로 재시도한다.
      } finally {
        if (!cancelled) setPreparing(false);
      }
    });
    return () => {
      cancelled = true;
      window.cancelAnimationFrame(frame);
    };
  }, [report, inAppBrowser]);

  useEffect(() => {
    let mounted = true;
    setError(null);
    setReport(null);
    api
      .getPdfReport(
        reportId,
        monthlyAverageWage == null ? {} : { monthlyAverageWage },
      )
      .then((data) => {
        if (mounted) setReport(data);
      })
      .catch((e) => {
        if (!mounted) return;
        setError(e instanceof ApiError ? e.message : "PDF 리포트를 준비하지 못했어요.");
      });

    return () => {
      mounted = false;
    };
  }, [monthlyAverageWage, reportId]);

  async function handleSavePdf() {
    if (inAppBrowser) {
      openInExternalBrowser(inAppBrowser);
      return;
    }

    if (!report) {
      setError("PDF로 저장할 리포트를 찾지 못했어요. 다시 시도해 주세요.");
      return;
    }

    const filename = `${safePdfFilename(`LIFT_${report.summaryTitle}`, "LIFT_report")}.pdf`;
    const ready = pdfBlobRef.current;

    setSaving(true);
    setError(null);
    // Blob이 이미 준비돼 있으면 사전 탭 없이 클릭 제스처 안에서 바로 공유/저장한다.
    // 아직 준비 전이면 비동기 생성 동안 제스처가 만료되므로 사전 탭을 열어 둔다.
    const fallbackWindow = ready ? null : openPdfFallbackWindow();
    try {
      let blob = ready;
      if (!blob) {
        const target = exportRef.current?.querySelector<HTMLElement>(".pdf-page");
        if (!target) {
          throw new Error("PDF export target missing.");
        }
        blob = await createPdfBlobFromElement(target);
        pdfBlobRef.current = blob;
      }
      await savePdfBlob(blob, filename, fallbackWindow);
    } catch {
      fallbackWindow?.close();
      setError("PDF 파일 저장에 실패했어요. Safari나 Chrome에서 다시 시도해 주세요.");
    } finally {
      setSaving(false);
    }
  }

  if (error) {
    return (
      <main className="pdf-preview-page">
        <div className="error-box">{error}</div>
        <button type="button" className="btn secondary" onClick={() => router.back()}>
          이전으로
        </button>
      </main>
    );
  }

  if (!report) {
    return <div className="center-state">PDF 리포트를 준비하는 중…</div>;
  }

  return (
    <main className="pdf-preview-page">
      {inAppBrowser && (
        <div className="warning-box no-print" style={{ marginBottom: 10 }}>
          {inAppBrowser === "kakaotalk"
            ? "카카오톡 안 브라우저에서는 PDF 저장이 지원되지 않아요. 아래 버튼을 누르면 기본 브라우저로 열어드려요."
            : "앱 안 브라우저에서는 PDF 저장이 지원되지 않아요. Chrome이나 Safari로 열어주세요."}
        </div>
      )}
      <div className="pdf-preview-toolbar no-print">
        <button type="button" className="btn secondary" onClick={() => router.back()}>
          이전으로
        </button>
        <button
          type="button"
          className="btn"
          disabled={saving || (preparing && !inAppBrowser)}
          onClick={handleSavePdf}
        >
          {saving
            ? "PDF 저장 중…"
            : inAppBrowser
              ? "브라우저로 열어 PDF 저장"
              : preparing
                ? "PDF 준비 중…"
                : "PDF 파일 저장"}
        </button>
      </div>
      <div className="pdf-preview-shell">
        <ReportPdfDocument report={report} />
      </div>
      <div ref={exportRef} className="pdf-export-root" aria-hidden="true">
        <ReportPdfDocument report={report} />
      </div>
    </main>
  );
}

export default function ReportPdfPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  return (
    <AuthGuard>
      <Suspense fallback={<div className="center-state">PDF 리포트를 준비하는 중…</div>}>
        <ReportPdfInner reportId={Number(id)} />
      </Suspense>
    </AuthGuard>
  );
}
