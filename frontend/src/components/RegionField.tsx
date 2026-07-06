"use client";

interface Props {
  sido: string;
  sigungu: string;
  onSidoChange: (value: string) => void;
  onSigunguChange: (value: string) => void;
}

const PROVINCES = [
  "서울특별시",
  "부산광역시",
  "대구광역시",
  "인천광역시",
  "광주광역시",
  "대전광역시",
  "울산광역시",
  "세종특별자치시",
  "경기도",
  "강원특별자치도",
  "충청북도",
  "충청남도",
  "전북특별자치도",
  "전라남도",
  "경상북도",
  "경상남도",
  "제주특별자치도",
];

/**
 * 거주 지역 입력. 시/도는 커스텀 스타일 셀렉트, 시/군/구는 아이콘 입력.
 */
export function RegionField({ sido, sigungu, onSidoChange, onSigunguChange }: Props) {
  return (
    <div className="region-row">
      <div className="field-icon-wrap select-wrap">
        <span className="lead-ico">📍</span>
        <select
          className={`select-field ${sido ? "" : "placeholder"}`}
          value={sido}
          onChange={(e) => onSidoChange(e.target.value)}
        >
          <option value="">시 / 도 선택</option>
          {PROVINCES.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
        <span className="select-caret">▾</span>
      </div>

      <div className="field-icon-wrap">
        <span className="lead-ico">🏘️</span>
        <input
          type="text"
          className="text-input with-icon"
          placeholder="시 / 군 / 구"
          value={sigungu}
          onChange={(e) => onSigunguChange(e.target.value)}
        />
      </div>
    </div>
  );
}
