const A4_WIDTH_MM = 210;
const A4_HEIGHT_MM = 297;

type ShareNavigator = Navigator & {
  canShare?: (data: ShareData) => boolean;
  share?: (data: ShareData) => Promise<void>;
};

export type PdfSaveResult = "shared" | "downloaded" | "opened" | "cancelled";

export function safePdfFilename(value: string, fallback = "LIFT"): string {
  const name = value
    .trim()
    .replace(/[\\/:*?"<>|]/g, "_")
    .replace(/\s+/g, "_")
    .slice(0, 80);
  return name || fallback;
}

export function isAppleMobileBrowser(): boolean {
  if (typeof navigator === "undefined") return false;
  const ua = navigator.userAgent;
  return /iPad|iPhone|iPod/i.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

export function openPdfFallbackWindow(): Window | null {
  // 모바일에서만 사전 탭을 연다. Blob 생성(비동기) 뒤에는 팝업이 막히므로,
  // 클릭 제스처가 살아있는 동안 미리 빈 탭을 확보해 두는 용도다.
  if (typeof navigator === "undefined") return null;
  if (!isAppleMobileBrowser() && !/Android/i.test(navigator.userAgent)) return null;

  const win = window.open("", "_blank");
  if (!win) return null;

  try {
    win.document.title = "PDF 준비 중";
    win.document.body.innerHTML =
      '<div style="font-family:-apple-system,BlinkMacSystemFont,sans-serif;padding:24px;line-height:1.5;color:#172033">' +
      '<strong>PDF 파일을 준비하고 있어요.</strong><br />잠시 후 이 탭에서 열립니다.' +
      "</div>";
  } catch {
    // Some WebViews block access to the newly opened document. Navigation still works later.
  }

  return win;
}

export async function createPdfBlobFromElement(element: HTMLElement): Promise<Blob> {
  if (document.fonts) {
    await document.fonts.ready;
  }

  await new Promise((resolve) => window.requestAnimationFrame(resolve));

  const [{ default: html2canvas }, { jsPDF }] = await Promise.all([
    import("html2canvas"),
    import("jspdf"),
  ]);
  const canvas = await html2canvas(element, {
    backgroundColor: "#ffffff",
    logging: false,
    scale: 2,
    useCORS: true,
    windowWidth: element.scrollWidth,
  });

  if (canvas.width === 0 || canvas.height === 0) {
    throw new Error("PDF export target is empty.");
  }

  const pdf = new jsPDF({
    orientation: "portrait",
    unit: "mm",
    format: "a4",
    compress: true,
  });
  const pageHeightPx = Math.floor((canvas.width * A4_HEIGHT_MM) / A4_WIDTH_MM);
  let offsetY = 0;
  let pageIndex = 0;

  while (offsetY < canvas.height) {
    const sliceHeight = Math.min(pageHeightPx, canvas.height - offsetY);
    const pageCanvas = document.createElement("canvas");
    pageCanvas.width = canvas.width;
    pageCanvas.height = sliceHeight;

    const ctx = pageCanvas.getContext("2d");
    if (!ctx) {
      throw new Error("Could not create PDF page canvas.");
    }

    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, pageCanvas.width, pageCanvas.height);
    ctx.drawImage(
      canvas,
      0,
      offsetY,
      canvas.width,
      sliceHeight,
      0,
      0,
      canvas.width,
      sliceHeight,
    );

    if (pageIndex > 0) {
      pdf.addPage();
    }

    const pageHeightMm = (sliceHeight * A4_WIDTH_MM) / canvas.width;
    pdf.addImage(
      pageCanvas.toDataURL("image/jpeg", 0.92),
      "JPEG",
      0,
      0,
      A4_WIDTH_MM,
      pageHeightMm,
      undefined,
      "FAST",
    );

    offsetY += sliceHeight;
    pageIndex += 1;
  }

  return pdf.output("blob");
}

function isMobileBrowser(): boolean {
  if (typeof navigator === "undefined") return false;
  return isAppleMobileBrowser() || /Android/i.test(navigator.userAgent);
}

/**
 * PDF Blob을 기기에 저장한다. 플랫폼별로 가장 확실한 경로를 고른다.
 *
 * - iOS Safari · Android Chrome: 파일 공유(Web Share)가 가능하면 "공유 시트"를 띄운다.
 *   여기서 "파일에 저장"(iOS) / "다운로드"(Android)로 실제 저장이 가능하다.
 *   단, 공유 시트는 사용자 제스처(클릭) 안에서 호출돼야 하므로, 호출부(PDF 페이지)는
 *   Blob을 미리 만들어 두고 클릭 시 곧바로 이 함수를 부른다.
 * - 공유가 불가능하거나 실패한 모바일: 미리 열어둔 탭 또는 새 탭에 Blob을 띄워
 *   기본 PDF 뷰어에서 저장하도록 한다.
 * - 데스크톱: download 속성 앵커로 즉시 내려받는다.
 */
export async function savePdfBlob(
  blob: Blob,
  filename: string,
  fallbackWindow: Window | null = null,
): Promise<PdfSaveResult> {
  const normalizedFilename = filename.endsWith(".pdf") ? filename : `${filename}.pdf`;
  const file = new File([blob], normalizedFilename, { type: "application/pdf" });
  const shareNavigator = navigator as ShareNavigator;
  const mobile = isMobileBrowser();

  // 모바일에서 파일 공유가 지원되면 우선 사용한다(iOS 저장/에어드롭, Android 저장 등).
  if (mobile && shareNavigator.share && canShareFile(shareNavigator, file)) {
    try {
      await shareNavigator.share({
        files: [file],
        title: normalizedFilename,
      });
      closeFallbackWindow(fallbackWindow);
      return "shared";
    } catch (error) {
      if (isShareCancelled(error)) {
        closeFallbackWindow(fallbackWindow);
        return "cancelled";
      }
      // 공유가 거부되면(대개 사용자 제스처 만료) 아래 탭 열기/다운로드로 넘어간다.
    }
  }

  const url = URL.createObjectURL(blob);

  // iOS 등에서 미리 열어둔 탭이 있으면 그 탭에서 PDF를 연다(뷰어에서 저장 가능).
  if (fallbackWindow && !fallbackWindow.closed) {
    fallbackWindow.location.href = url;
    window.setTimeout(() => URL.revokeObjectURL(url), 60000);
    return "opened";
  }

  // 공유·다운로드가 모두 애매한 모바일: 새 탭에 PDF를 띄운다.
  if (mobile) {
    const opened = window.open(url, "_blank");
    if (!opened) {
      window.location.href = url;
    }
    window.setTimeout(() => URL.revokeObjectURL(url), 60000);
    return "opened";
  }

  // 데스크톱(및 다운로드가 확실한 환경): 앵커 다운로드.
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = normalizedFilename;
  anchor.rel = "noopener";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  return "downloaded";
}

export function createPdfObjectUrl(blob: Blob): string {
  return URL.createObjectURL(blob);
}

export function revokePdfObjectUrl(url: string): void {
  URL.revokeObjectURL(url);
}

export async function sharePdfBlob(blob: Blob, filename: string): Promise<boolean> {
  const normalizedFilename = filename.endsWith(".pdf") ? filename : `${filename}.pdf`;
  const file = new File([blob], normalizedFilename, { type: "application/pdf" });
  const shareNavigator = navigator as ShareNavigator;

  if (!shareNavigator.share || !canShareFile(shareNavigator, file)) {
    return false;
  }

  await shareNavigator.share({
    files: [file],
    title: normalizedFilename,
  });
  return true;
}

function canShareFile(navigatorApi: ShareNavigator, file: File): boolean {
  try {
    return Boolean(navigatorApi.canShare?.({ files: [file] }));
  } catch {
    return false;
  }
}

function closeFallbackWindow(win: Window | null) {
  if (!win || win.closed) return;
  try {
    win.close();
  } catch {
    // Ignore: the share sheet already handled the file.
  }
}

function isShareCancelled(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}
