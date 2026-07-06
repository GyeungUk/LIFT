"use client";

import { useEffect, useMemo, useRef, useState } from "react";

type Step = "form" | "code" | "consent" | "loading";

const CARRIERS = ["SKT", "KT", "LG U+", "알뜰폰"] as const;
type Carrier = (typeof CARRIERS)[number];

const CONSENTS = [
  { key: "privacy", label: "개인정보 수집·이용 동의", required: true },
  { key: "unique", label: "고유식별정보 처리 동의", required: true },
  { key: "terms", label: "서비스 이용약관 동의", required: true },
  { key: "provide", label: "공공 마이데이터 제공 요구 동의", required: true },
] as const;

function formatPhone(raw: string): string {
  const d = raw.replace(/\D/g, "").slice(0, 11);
  if (d.length < 4) return d;
  if (d.length < 8) return `${d.slice(0, 3)}-${d.slice(3)}`;
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`;
}

export function IdentityVerifyModal({
  onClose,
  onComplete,
}: {
  onClose: () => void;
  onComplete: (name: string) => void;
}) {
  const [step, setStep] = useState<Step>("form");

  const [carrier, setCarrier] = useState<Carrier | null>(null);
  const [name, setName] = useState("");
  const [birth, setBirth] = useState("");
  const [phone, setPhone] = useState("");

  const [code, setCode] = useState("");
  const [seconds, setSeconds] = useState(180);

  const [agreed, setAgreed] = useState<Record<string, boolean>>({});

  const codeInputRef = useRef<HTMLInputElement>(null);

  const phoneDigits = phone.replace(/\D/g, "");
  const formValid =
    carrier !== null &&
    name.trim().length >= 2 &&
    /^\d{6}$/.test(birth) &&
    phoneDigits.length === 11;

  const codeValid = /^\d{6}$/.test(code);
  const allRequiredAgreed = CONSENTS.every((c) => agreed[c.key]);
  const allAgreed = useMemo(
    () => CONSENTS.every((c) => agreed[c.key]),
    [agreed],
  );

  // 인증번호 단계 카운트다운
  useEffect(() => {
    if (step !== "code") return;
    if (seconds <= 0) return;
    const t = setInterval(() => setSeconds((s) => (s > 0 ? s - 1 : 0)), 1000);
    return () => clearInterval(t);
  }, [step, seconds]);

  useEffect(() => {
    if (step === "code") setTimeout(() => codeInputRef.current?.focus(), 250);
  }, [step]);

  // 배경 스크롤 잠금
  useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, []);

  function requestCode() {
    if (!formValid) return;
    setSeconds(180);
    setCode("");
    setStep("code");
  }

  function confirmCode() {
    if (!codeValid) return;
    setStep("consent");
  }

  function toggleAll(v: boolean) {
    const next: Record<string, boolean> = {};
    for (const c of CONSENTS) next[c.key] = v;
    setAgreed(next);
  }

  function submit() {
    if (!allRequiredAgreed) return;
    setStep("loading");
    setTimeout(() => onComplete(name.trim()), 1600);
  }

  const mmss = `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(
    seconds % 60,
  ).padStart(2, "0")}`;

  const stepIndex = step === "form" ? 0 : step === "code" ? 1 : 2;

  return (
    <div className="idv-overlay" onClick={onClose}>
      <div
        className="idv-sheet"
        role="dialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="idv-grip" />

        <div className="idv-head">
          <div className="idv-brand">
            <span className="idv-shield">🪪</span>
            <div>
              <div className="idv-brand-title">본인인증</div>
              <div className="idv-brand-sub">간편인증으로 안전하게 확인해요</div>
            </div>
          </div>
          <button
            type="button"
            className="idv-close"
            onClick={onClose}
            aria-label="닫기"
          >
            ✕
          </button>
        </div>

        <div className="idv-demo">
          🧪 데모용 모의 본인인증입니다. 실제 인증·정보 전송은 이루어지지 않아요.
        </div>

        <div className="idv-steps">
          {["정보 입력", "인증번호", "정보 제공 동의"].map((label, i) => (
            <div
              key={label}
              className={`idv-step ${i === stepIndex ? "on" : ""} ${
                i < stepIndex ? "done" : ""
              }`}
            >
              <span className="idv-step-dot">{i < stepIndex ? "✓" : i + 1}</span>
              <span className="idv-step-label">{label}</span>
            </div>
          ))}
        </div>

        {step === "form" && (
          <div className="idv-body">
            <label className="idv-label">통신사</label>
            <div className="idv-carriers">
              {CARRIERS.map((c) => (
                <button
                  key={c}
                  type="button"
                  className={`idv-carrier ${carrier === c ? "sel" : ""}`}
                  onClick={() => setCarrier(c)}
                >
                  {c}
                </button>
              ))}
            </div>

            <label className="idv-label">이름</label>
            <input
              className="idv-input"
              placeholder="홍길동"
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />

            <label className="idv-label">생년월일 6자리</label>
            <input
              className="idv-input"
              placeholder="예) 000101"
              inputMode="numeric"
              value={birth}
              onChange={(e) =>
                setBirth(e.target.value.replace(/\D/g, "").slice(0, 6))
              }
            />

            <label className="idv-label">휴대폰 번호</label>
            <input
              className="idv-input"
              placeholder="010-0000-0000"
              inputMode="numeric"
              value={phone}
              onChange={(e) => setPhone(formatPhone(e.target.value))}
            />

            <button
              type="button"
              className="idv-cta"
              disabled={!formValid}
              onClick={requestCode}
            >
              인증번호 요청
            </button>
          </div>
        )}

        {step === "code" && (
          <div className="idv-body">
            <div className="idv-sent">
              <b>{formatPhone(phone)}</b> 로<br />
              인증번호를 보냈어요.
            </div>

            <div className="idv-code-wrap">
              <input
                ref={codeInputRef}
                className="idv-code"
                placeholder="000000"
                inputMode="numeric"
                maxLength={6}
                value={code}
                onChange={(e) =>
                  setCode(e.target.value.replace(/\D/g, "").slice(0, 6))
                }
              />
              <span className={`idv-timer ${seconds <= 30 ? "warn" : ""}`}>
                {mmss}
              </span>
            </div>

            <div className="idv-hint">
              데모에서는 <b>아무 숫자 6자리</b>나 입력하면 인증돼요.
            </div>

            <button
              type="button"
              className="idv-resend"
              onClick={() => setSeconds(180)}
            >
              인증번호 다시 받기
            </button>

            <button
              type="button"
              className="idv-cta"
              disabled={!codeValid}
              onClick={confirmCode}
            >
              확인
            </button>
          </div>
        )}

        {step === "consent" && (
          <div className="idv-body">
            <button
              type="button"
              className={`idv-all ${allAgreed ? "on" : ""}`}
              onClick={() => toggleAll(!allAgreed)}
            >
              <span className="idv-check">{allAgreed ? "✓" : ""}</span>
              전체 동의하기
            </button>

            <div className="idv-consent-list">
              {CONSENTS.map((c) => (
                <button
                  key={c.key}
                  type="button"
                  className="idv-consent"
                  onClick={() =>
                    setAgreed((p) => ({ ...p, [c.key]: !p[c.key] }))
                  }
                >
                  <span className={`idv-check sm ${agreed[c.key] ? "on" : ""}`}>
                    {agreed[c.key] ? "✓" : ""}
                  </span>
                  <span className="idv-consent-label">
                    <span className="idv-req">[필수]</span> {c.label}
                  </span>
                </button>
              ))}
            </div>

            <button
              type="button"
              className="idv-cta"
              disabled={!allRequiredAgreed}
              onClick={submit}
            >
              동의하고 서류 조회하기
            </button>
          </div>
        )}

        {step === "loading" && (
          <div className="idv-body idv-loading">
            <div className="idv-spinner" />
            <div className="idv-loading-title">
              {name.trim()}님의 정보를 확인하고 있어요
            </div>
            <div className="idv-loading-sub">
              공공기관에서 발급 가능한 서류를 안전하게 조회하는 중…
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
