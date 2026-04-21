# Sevin.dev Blog Platform 요구사항 명세 v1.1.0

- 버전: v1.1.0 (v1.0.1 기반 API 엔드포인트 및 미결 사항 확정)
- 최초 작성: 2026-02-20 / 업데이트: 2026-04-21

---

## 1. 시스템 아키텍처

### 도메인 / 라우팅

| 경로 | 서비스 |
|---|---|
| `/portfolio` | Portfolio FE (Vercel) |
| `/blog` | Blog FE (Vercel) |
| `/api/blog/*` | Blog BE (EC2) |

- 단일 도메인 `sevin.dev`, Path 기반 라우팅
- Blog FE / BE 분리 배포
- Blog BE는 API 전용 서버 (`/blog` context path)
- Reverse Proxy 또는 Edge Routing으로 경로 통합
- Analytics 이벤트 수집은 Blog BE에서 처리

---

## 2. 역할 / 권한

- Role: `ADMIN` 단일 Role. 미인증 사용자는 Anonymous 처리
- Public 리소스: `permitAll`
- ADMIN 리소스: `/api/blog/admin/*` 경로, Role 검증 필수
- 관리자 화면: UI 노출 없음, URL 직접 접근 방식만 허용

---

## 3. 인증 (Authentication)

### 방식

- ID/PW + JWT (ADMIN 계정 1개 운영)
- 비밀번호: bcrypt 해시 저장

### JWT 정책

| 토큰 | 만료 |
|---|---|
| Access Token | 15분 |
| Refresh Token | 7일 |

- 저장: HttpOnly Secure Cookie (클라이언트 JS 접근 불가, HTTPS 전용)
- Refresh Token Rotation: 재발급 시 기존 토큰 무효화, 신규 발급

### 인증 API

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/blog/admin/auth/login` | 로그인, Access/Refresh Token 발급 |
| POST | `/api/blog/admin/auth/logout` | 로그아웃, 쿠키 삭제 |
| POST | `/api/blog/admin/auth/refresh` | Access Token 재발급 |
| GET | `/api/blog/admin/auth/me` | 현재 사용자 정보 |

---

## 4. 게시글 (Post)

### 필드

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| uuid | UUID | 외부 노출용 |
| title | String | 필수 |
| slug | String | 필수, 유니크 |
| content | Text | Markdown |
| status | Enum | 필수 |
| excerpt | String | 선택, 미입력 시 content 앞 200자 자동 생성 |
| thumbnailUrl | String | 선택 |
| publishedAt | DateTime | PUBLISHED 전이 시 자동 기록 |
| scheduledAt | DateTime | SCHEDULED 상태일 때만 유효 |
| createdAt | DateTime | 자동 |
| updatedAt | DateTime | 자동 |
| deletedAt | DateTime | Soft delete |

### 상태 (Status)

```
DRAFT → PUBLISHED
DRAFT → SCHEDULED
DRAFT → PRIVATE
PRIVATE → PUBLISHED
SCHEDULED → PUBLISHED  (스케줄러 자동 전이)
ANY → DELETED
```

- Public 조회: `PUBLISHED` 상태만
- ADMIN: 모든 상태 조회 가능
- Soft Delete 전용 (물리 삭제 없음)

### 게시글 API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/blog/posts` | Public | 목록 (PUBLISHED, cursor 페이지네이션) |
| GET | `/api/blog/posts/{slug}` | Public | 상세 조회 |
| GET | `/api/blog/search?q=&tag=&category=&cursor=` | Public | 검색 (제목+내용, 태그/카테고리 필터) |
| GET | `/api/blog/admin/posts` | ADMIN | 전체 목록 (모든 상태) |
| POST | `/api/blog/admin/posts` | ADMIN | 게시글 생성 |
| PUT | `/api/blog/admin/posts/{id}` | ADMIN | 게시글 수정 |
| PATCH | `/api/blog/admin/posts/{id}/status` | ADMIN | 상태 변경 |
| DELETE | `/api/blog/admin/posts/{id}` | ADMIN | Soft delete |

**페이지네이션**: Cursor 기반 (`?cursor=<lastId>&size=10`)
**검색 범위**: 제목 + 내용 Full-text (PostgreSQL `tsvector`), 태그/카테고리 필터 가능

---

## 5. 태그 (Tag)

- Post는 0개 이상의 Tag 보유 (M:N)
- Tag name 유니크, Post 저장 시 없으면 자동 생성

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/blog/tags` | Public | 태그 목록 (게시글 수 포함) |
| DELETE | `/api/blog/admin/tags/{id}` | ADMIN | 태그 삭제 |

---

## 6. 카테고리 (Category)

- Post는 0..1 Category 보유 (단일 레벨, 계층 없음)
- slug 유니크

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/blog/categories` | Public | 카테고리 목록 (게시글 수 포함) |
| POST | `/api/blog/admin/categories` | ADMIN | 생성 |
| PUT | `/api/blog/admin/categories/{id}` | ADMIN | 수정 |
| DELETE | `/api/blog/admin/categories/{id}` | ADMIN | 삭제 |

---

## 7. 댓글 (Comment)

### 필드

| 필드 | 비고 |
|---|---|
| id | PK |
| postId | FK |
| nickname | 필수 |
| password | bcrypt 해시, 필수 |
| content | 필수 |
| status | ACTIVE / DELETED |
| createdAt | 자동 |

- 익명 댓글 (로그인 불필요)
- 대댓글 미지원 (단일 레벨)
- 수정/삭제: password 검증 기반, 실패 시 403

### 댓글 API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/blog/posts/{slug}/comments` | Public | 댓글 목록 (ACTIVE만) |
| POST | `/api/blog/posts/{slug}/comments` | Public | 댓글 작성 |
| PUT | `/api/blog/posts/{slug}/comments/{id}` | Public | 수정 (password 검증) |
| DELETE | `/api/blog/posts/{slug}/comments/{id}` | Public | Soft delete (password 검증) |
| DELETE | `/api/blog/admin/comments/{id}` | ADMIN | 강제 삭제 |

### 스팸 방지

- Rate Limit: 댓글 작성 3회/분 (IP 기준)
- 금칙어 필터: 선택 적용

---

## 8. Analytics

### 방문자 식별

- `anonId`: 최초 방문 시 서버 발급, 쿠키 저장 (HttpOnly)
- UV 중복 처리: 동일 anonId + 동일 날짜 = 1 UV

### 수집 이벤트

| 이벤트 | 필수 |
|---|---|
| PAGE_VIEW | 필수 |
| POST_VIEW | 필수 |
| SEARCH | 선택 |

### 집계 지표

- Daily Metrics, Monthly Metrics 집계 (스케줄러)
- 지표: PV, UV, DAU, MAU, 게시글별 조회수, 태그별 조회수

### Analytics API

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/blog/analytics/events` | Public | 이벤트 수집 |
| GET | `/api/blog/admin/analytics/dashboard` | ADMIN | Today/Total 대시보드 |
| GET | `/api/blog/admin/analytics/posts` | ADMIN | 게시글별 조회수 |
| GET | `/api/blog/admin/analytics/dau` | ADMIN | DAU 추이 |
| GET | `/api/blog/admin/analytics/mau` | ADMIN | MAU 추이 |

---

## 9. 이미지 업로드

- ADMIN 전용
- 저장소: AWS S3
- 파일 크기 제한: 10MB
- 지원 형식: `png`, `jpg`, `jpeg`, `gif`
- 미지원 형식: 415 반환
- 응답: 업로드된 S3 URL 반환

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/blog/admin/upload/image` | ADMIN | 이미지 업로드 |

---

## 10. 에디터

- 관리자 전용 마크다운 에디터 (FE 영역)
- Preview, 자동 저장 (동일 Draft 레코드 업데이트 방식), 이미지 업로드

---

## 11. 예약 발행

- `SCHEDULED` 상태 Post를 서버 스케줄러(1분 주기)가 감지하여 `PUBLISHED` 전이
- Idempotent 처리 (중복 실행 안전)

---

## 12. SEO (FE 영역)

- SSR 또는 SSG
- OG 태그, sitemap (발행 시 갱신 트리거), canonical URL

---

## 13. 표준 응답 포맷

```json
// 성공
{ "success": true, "data": { ... } }

// 목록
{ "success": true, "data": [...], "cursor": "xxx", "hasNext": true }

// 실패
{ "success": false, "error": { "code": "POST_NOT_FOUND", "message": "..." } }
```

---

## 14. 보안

- 관리자 API: 인증 필수
- 로그인 Rate Limit: 5회/분 (IP 기준)
- 댓글 Rate Limit: 3회/분 (IP 기준)
- HTTPS 필수, 토큰 HttpOnly Cookie

---

## 15. 운영

- Health Check: `GET /api/blog/health`
- 표준 에러 응답 포맷 (13번 참조)
- 기본 로깅 (요청/응답, 에러)
- Analytics 로그 보관: 최대 1년

---

## 16. CI/CD

- Blog FE: Vercel 자동 배포
- Blog BE: GitHub Actions (`main` 머지 시 EC2 배포)
- 환경 변수: `.env.local` / `.env.production` 분리
