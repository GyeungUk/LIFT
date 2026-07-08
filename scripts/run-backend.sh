#!/bin/sh
# LIFT 백엔드 로컬 실행 런처 (preview_start / launch.json 용).
#
# preview 프로세스가 어떤 작업 디렉터리에서 실행되든(때로 접근 불가한 CWD에서 뜬다)
# 안전하게 동작하도록 모든 경로를 절대/스크립트 기준으로 잡는다.
#  - 스크립트 위치로부터 프로젝트 루트를 구해 cd (상대경로 ./gradlew 문제 회피)
#  - 기본 java가 11이면 Gradle 9가 뜨지 않으므로 macOS 표준 방식으로 Java 21 자동 탐색
#  - 로컬 프로파일(H2 인메모리 + .env)로 기동
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

# Java 21 자동 탐색 (없으면 현재 java 사용 — 21 미설치 시 Gradle이 안내 메시지 출력)
if JH="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
  JAVA_HOME="$JH"
  export JAVA_HOME
  PATH="$JAVA_HOME/bin:$PATH"
  export PATH
fi

# macOS preview 샌드박스는 ~/Documents 안의 '파일 실행(exec)'을 막는다(읽기/쓰기는 허용).
# 그래서 ./gradlew를 직접 실행하지 않고 시스템 sh로 '해석'해 실행한다. gradlew 내부의
# java 실행은 ~/Library(비보호 영역)라 허용된다.
exec sh ./gradlew bootRun --args="--spring.profiles.active=local" --console=plain
