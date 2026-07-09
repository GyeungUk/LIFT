import React from "react";

/**
 * AI 답변용 초경량 마크다운 렌더러.
 * OpenAI 답변이 쓰는 문법만 처리한다: **굵게**, `- ` / `* ` 목록, [텍스트](링크)·자동 URL, 줄바꿈.
 * 외부 의존성 없이(번들·CSP 부담 없음) 안전하게 렌더한다. 사용자가 입력한 메시지에는 쓰지 않는다.
 */

// **굵게**, [라벨](URL), 그리고 http(s):// 자동 링크를 인라인으로 변환한다.
function renderInline(text: string, keyPrefix: string): React.ReactNode[] {
  const nodes: React.ReactNode[] = [];
  const regex =
    /\*\*([^*]+)\*\*|\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)|(https?:\/\/[^\s)]+)/g;
  let last = 0;
  let match: RegExpExecArray | null;
  let i = 0;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > last) {
      nodes.push(text.slice(last, match.index));
    }
    if (match[1] !== undefined) {
      nodes.push(<strong key={`${keyPrefix}-b${i}`}>{match[1]}</strong>);
    } else if (match[2] !== undefined && match[3] !== undefined) {
      nodes.push(
        <a key={`${keyPrefix}-l${i}`} href={match[3]} target="_blank" rel="noreferrer">
          {match[2]}
        </a>,
      );
    } else if (match[4] !== undefined) {
      nodes.push(
        <a key={`${keyPrefix}-u${i}`} href={match[4]} target="_blank" rel="noreferrer">
          {match[4]}
        </a>,
      );
    }
    last = regex.lastIndex;
    i += 1;
  }
  if (last < text.length) {
    nodes.push(text.slice(last));
  }
  return nodes;
}

export function ChatMarkdown({ text }: { text: string }) {
  const lines = text.split("\n");
  const blocks: React.ReactNode[] = [];
  let listItems: string[] = [];
  let key = 0;

  const flushList = () => {
    if (listItems.length > 0) {
      const items = listItems;
      const listKey = key;
      blocks.push(
        <ul key={`ul${key++}`} className="md-list">
          {items.map((item, idx) => (
            <li key={idx}>{renderInline(item, `li${listKey}-${idx}`)}</li>
          ))}
        </ul>,
      );
      listItems = [];
    }
  };

  for (const rawLine of lines) {
    const line = rawLine.replace(/\s+$/, "");
    const bullet = line.match(/^\s*[-*]\s+(.*)$/);
    if (bullet) {
      listItems.push(bullet[1]);
      continue;
    }
    flushList();
    if (line.trim() === "") {
      continue;
    }
    blocks.push(
      <p key={`p${key++}`} className="md-p">
        {renderInline(line, `p${key}`)}
      </p>,
    );
  }
  flushList();

  return <>{blocks}</>;
}
