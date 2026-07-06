"use client";

import { useEffect, useMemo, useRef, useState } from "react";

interface Props {
  value: string; // ISO yyyy-mm-dd
  onChange: (value: string) => void;
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

function toLocalIso(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${dd}`;
}

function parseIso(iso: string): Date | null {
  if (!iso) return null;
  const [y, m, d] = iso.split("-").map(Number);
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d);
}

function formatKorean(iso: string): string {
  if (!iso) return "";
  const [y, m, d] = iso.split("-");
  return `${y}년 ${Number(m)}월 ${Number(d)}일`;
}

function sameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

/**
 * 네이티브 달력을 쓰지 않는 커스텀 날짜 선택기.
 * 예쁜 트리거 + 팝오버 달력(월 이동 / 오늘·선택 강조) + 빠른 선택 칩.
 */
export function DateField({ value, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const [view, setView] = useState<Date>(() => parseIso(value) ?? new Date());
  const wrapRef = useRef<HTMLDivElement>(null);

  const today = new Date();
  const selected = useMemo(() => parseIso(value), [value]);

  // 팝오버가 열릴 때 선택값(없으면 오늘)이 보이도록 뷰 이동
  useEffect(() => {
    if (open) setView(parseIso(value) ?? new Date());
  }, [open, value]);

  // 바깥 클릭 / ESC 로 닫기
  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const grid = useMemo(() => {
    const year = view.getFullYear();
    const month = view.getMonth();
    const startWeekday = new Date(year, month, 1).getDay();
    const start = new Date(year, month, 1 - startWeekday);
    return Array.from({ length: 42 }, (_, i) => {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      return d;
    });
  }, [view]);

  function pick(d: Date) {
    onChange(toLocalIso(d));
    setOpen(false);
  }

  function shiftMonth(delta: number) {
    setView((v) => new Date(v.getFullYear(), v.getMonth() + delta, 1));
  }

  return (
    <div className="date-picker" ref={wrapRef}>
      <button
        type="button"
        className={`date-trigger ${value ? "has" : ""}`}
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
      >
        <span className="date-ico">📅</span>
        <span className="date-text">
          {value ? formatKorean(value) : "날짜를 선택하세요"}
        </span>
        {value ? (
          <span
            className="date-clear"
            role="button"
            aria-label="날짜 지우기"
            onClick={(e) => {
              e.stopPropagation();
              onChange("");
            }}
          >
            ✕
          </span>
        ) : (
          <span className={`date-caret ${open ? "open" : ""}`}>▾</span>
        )}
      </button>

      <div className="preset-row">
        <button type="button" className="preset" onClick={() => pick(new Date())}>
          오늘
        </button>
        <button
          type="button"
          className="preset"
          onClick={() => pick(new Date(today.getFullYear(), today.getMonth() + 1, 0))}
        >
          이번 달 말일
        </button>
        <button
          type="button"
          className="preset"
          onClick={() => pick(new Date(today.getFullYear(), today.getMonth(), 0))}
        >
          지난달 말일
        </button>
      </div>

      {open && (
        <div className="cal-pop" role="dialog" aria-label="날짜 선택">
          <div className="cal-head">
            <span className="cal-title">
              {view.getFullYear()}년 {view.getMonth() + 1}월
            </span>
            <div className="cal-nav">
              <button type="button" aria-label="이전 달" onClick={() => shiftMonth(-1)}>
                ‹
              </button>
              <button type="button" aria-label="다음 달" onClick={() => shiftMonth(1)}>
                ›
              </button>
            </div>
          </div>

          <div className="cal-grid">
            {WEEKDAYS.map((w, i) => (
              <div
                key={w}
                className={`cal-wd ${i === 0 ? "sun" : i === 6 ? "sat" : ""}`}
              >
                {w}
              </div>
            ))}
            {grid.map((d) => {
              const out = d.getMonth() !== view.getMonth();
              const wd = d.getDay();
              const classes = [
                "cal-cell",
                out ? "out" : "",
                wd === 0 ? "sun" : wd === 6 ? "sat" : "",
                sameDay(d, today) ? "today" : "",
                selected && sameDay(d, selected) ? "sel" : "",
              ]
                .filter(Boolean)
                .join(" ");
              return (
                <button
                  key={toLocalIso(d)}
                  type="button"
                  className={classes}
                  onClick={() => pick(d)}
                >
                  {d.getDate()}
                </button>
              );
            })}
          </div>

          <div className="cal-foot">
            <button
              type="button"
              className="clear"
              onClick={() => {
                onChange("");
                setOpen(false);
              }}
            >
              삭제
            </button>
            <button type="button" onClick={() => pick(new Date())}>
              오늘
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
