"use client";

export interface Option<T extends string> {
  value: T;
  label: string;
  desc?: string;
  icon?: string;
}

interface Props<T extends string> {
  options: Option<T>[];
  value: T | null;
  onChange: (value: T) => void;
  variant?: "card" | "chip";
  columns?: number;
}

/**
 * 밋밋한 <select>를 대체하는 커스텀 선택 컴포넌트.
 * - card: 배지 + 라벨
 * - chip: 한 줄에 여러 개(세그먼트 형태)
 */
export function OptionSelector<T extends string>({
  options,
  value,
  onChange,
  variant = "card",
  columns = 1,
}: Props<T>) {
  const variantClass = variant === "card" ? "opt-card" : "opt-chip";

  return (
    <div className={`opt-grid cols-${columns}`}>
      {options.map((opt) => {
        const selected = opt.value === value;
        return (
          <button
            key={opt.value}
            type="button"
            className={`opt ${variantClass} ${selected ? "sel" : ""}`}
            data-option={opt.value}
            aria-pressed={selected}
            onClick={() => onChange(opt.value)}
          >
            {variant === "card" ? (
              <>
                {opt.icon && <span className="opt-ico">{opt.icon}</span>}
                <span className="opt-copy">
                  <span className="opt-label">{opt.label}</span>
                </span>
              </>
            ) : (
              <span className="opt-label">{opt.label}</span>
            )}
          </button>
        );
      })}
    </div>
  );
}
