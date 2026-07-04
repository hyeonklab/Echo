# Echo — echo-web 에이전트 가이드

이 문서는 `echo-web/`(Next.js App Router) 작업 시 에이전트가 따라야 할 Echo 프로젝트 규칙이다. README 전체를 대체하지 않는다.

## 모노레포 구조

| 경로 | 역할 |
|------|------|
| `echo-web/` | Next.js App Router + TypeScript 프론트엔드 (이 문서의 범위) |
| `echo-server/` | Spring Boot 3 + Java 21 REST/OAuth2/JWT 백엔드 |
| `docker-compose.yml` | PostgreSQL (루트) |

- 프론트엔드 변경은 `echo-web/` 안에서만 한다. 백엔드 수정이 필요하면 `echo-server/`를 별도로 다룬다.
- 백엔드 코딩 규칙(참고): guard clause, 함수 Javadoc, SP 추가 시 `USP_` 접두 + `SELECT`/`INSERT` 등 동사 접미.

## 디렉터리 규칙 (echo-web)

```
app/          # App Router 페이지 (page.tsx, layout.tsx)
components/   # 재사용 UI — index.ts barrel export
lib/          # auth, stomp 등 클라이언트/공용 유틸
public/       # 정적 자산
```

- 경로 alias: `@/` → 프로젝트 루트 (`tsconfig.json`).
- 클라이언트 훅/상태가 필요하면 `"use client"`를 파일 최상단에 선언한다.
- 인터랙티브 UI는 `components/`에, API/토큰 로직은 `lib/`에 둔다.

## 인증 흐름

```
/login → GET {API_URL}/oauth2/authorization/{google|naver}
       → OAuth2 (Spring) → /auth/callback?code=... → POST /api/auth/exchange
       → setTokens() → localStorage
       → GET /api/auth/me (Bearer JWT) → 사용자 표시
```

| 단계 | 파일/함수 |
|------|-----------|
| 로그인 링크 | `app/login/page.tsx` → `getOAuthLoginUrl()` |
| OAuth 콜백 | `app/auth/callback/page.tsx` → `setTokens()` |
| 토큰 저장/조회 | `lib/auth.ts` — `accessToken`, `refreshToken` 키 |
| 현재 사용자 | `fetchCurrentUser(token)` → `GET /api/auth/me` |
| 로그아웃 | `clearTokens()` (클라이언트만; 서버 세션 없음) |
| 홈 인증 UI | `components/HomeAuth.tsx` |

- 토큰은 **localStorage**에 저장한다. HttpOnly 쿠키가 아니다.
- `getAccessToken()`은 SSR에서 `null`을 반환한다 — 클라이언트 컴포넌트/`useEffect`에서만 사용.
- 토큰 만료 시 `POST /api/auth/refresh`로 갱신 가능(아직 프론트 자동 갱신 미구현). 새 기능 추가 시 `lib/auth.ts`에 통합한다.
- OAuth 실패 시 서버가 `/login?error=...`로 리다이렉트한다.

## 환경 변수

`.env.local.example`을 복사해 `.env.local` 생성:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

- `NEXT_PUBLIC_*`만 브라우저에서 접근 가능.
- API 기본값: `http://localhost:8080` (`lib/auth.ts` fallback).
- STOMP URL(예정): `NEXT_PUBLIC_STOMP_URL` — 기본 `http://localhost:8080/ws` (`lib/stomp.ts`).

## API 규칙

- Base URL: `process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"`.
- 인증 API 호출: `Authorization: Bearer ${accessToken}` 헤더.
- 주요 엔드포인트:
  - `GET /api/auth/me` — 현재 사용자 (`AuthUser` 타입)
  - `POST /api/auth/exchange` — `{ code }` body (OAuth 일회용 교환 코드)
  - `POST /api/auth/refresh` — `{ refreshToken }` body
  - `GET /oauth2/authorization/{google|naver}` — OAuth 시작 (리다이렉트)
- `fetch` 시 사용자 정보 조회는 `cache: "no-store"` 사용(기존 패턴 유지).

## STOMP / 채팅 (예정 — 요청 없이 구현 금지)

- `lib/stomp.ts` — STOMP 브로커 URL 상수만 존재 (stub).
- `app/chat/page.tsx` — 플레이스홀더 UI.
- `echo-server/.../websocket/` — 서버 측 stub.
- **사용자가 명시적으로 요청하기 전까지** STOMP 클라이언트, WebSocket 연결, 채팅 UI/상태관리를 구현하지 않는다.

## 코드 품질 (ESLint + SonarLint + SpotBugs/PMD)

| 도구 | 대상 | 실행 |
|------|------|------|
| **ESLint** | echo-web (TS/React) | `npm run lint` |
| **Extension Pack for Java** | echo-server IDE 진단 | Cursor 확장 설치 |
| **SonarLint** | echo-web + echo-server | Cursor 확장 |
| **SpotBugs / PMD** | echo-server (Maven) | `./mvnw verify -Pquality` |

- `echo-web/eslint.config.mjs` — ESLint flat config.
- `sonar-project.properties` — SonarLint 분석 범위.
- `echo-server/config/spotbugs-exclude.xml` — SpotBugs Lombok false positive 필터.
- `./mvnw spotbugs:check pmd:check` — quality 프로필 없이 단독 실행 가능.
- `./mvnw verify -Pquality` — test + SpotBugs + PMD.

## TypeScript / React 컨vention

- **언어**: UI 문구·레이블은 한국어 우선 (`lang="ko"` in layout). 코드·식별자·주석은 영어.
- **타입**: `lib/auth.ts`의 `AuthUser` 등 기존 타입 재사용. API 응답 shape 변경 시 타입과 fetch 함께 수정.
- **함수 주석**: export 함수·컴포넌트에 JSDoc(`/** ... */`) — 기존 파일 패턴 따름.
- **스타일**: Tailwind CSS, zinc 팔레트 + dark mode (`dark:`). 카드형 `rounded-2xl border` 레이아웃 유지.
- **페이지**: Server Component 기본; `useSearchParams`/`useRouter`/상태 필요 시 Client Component 분리 (`AuthCallbackContent` 패턴).
- **import**: `@/lib/auth`, `@/components/...` alias 사용.
- **최소 diff**: 요청 범위 밖 리팩터링·README/CLAUDE.md 생성 금지.

## 로컬 실행 (참고)

```bash
# echo-web
npm run dev          # http://localhost:3000

# echo-server (별도 터미널)
./mvnw spring-boot:run   # http://localhost:8080

# DB
docker compose up -d     # 루트 Echo/
```

<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->
