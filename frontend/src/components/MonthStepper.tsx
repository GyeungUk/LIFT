"use client";

interface Props {
  value: number | null;
  onChange: (value: number | null) => void;
  presets?: number[];
  max?: number;
}

/**
 * 고용보험 가입 개월 수 입력. +/- 스테퍼 + 자주 쓰는 값 프리셋 칩.
 */
export function MonthStepper({
  value,
  onChange,
  presets = [6, 12, 24, 36],
  max = 600,
}: Props) {
  const current = value ?? 0;

  function set(next: number) {
    onChange(Math.max(0, Math.min(max, next)));
  }

  return (
    <div>
      <div className="stepper">
        <button type="button" onClick={() => set(current - 1)} disabled={current <= 0} aria-label="1개월 감소">
          −
        </button>
        <div className="val">
          <span className="num">{value ?? "-"}</span>
          <span className="unit">개월</span>
        </div>
        <button type="button" onClick={() => set(current + 1)} aria-label="1개월 증가">
          +
        </button>
      </div>
      <div className="preset-row">
        {presets.map((p) => (
          <button
            key={p}
            type="button"
            className={`preset ${value === p ? "on" : ""}`}
            onClick={() => onChange(p)}
          >
            {p >= 12 && p % 12 === 0 ? `${p / 12}년` : `${p}개월`}
          </button>
        ))}
        <button
          type="button"
          className={`preset ${value === 0 ? "on" : ""}`}
          onClick={() => onChange(0)}
        >
          잘 모름
        </button>
      </div>
    </div>
  );
}
