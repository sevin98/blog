# API 명세

Base URL: `https://sevin.dev/api/blog`
Content-Type: `application/json`

## 표준 응답 포맷

```json
// 단건 성공
{ "success": true, "data": { } }

// 목록 성공 (cursor 페이지네이션)
{ "success": true, "data": [ ], "cursor": "123", "hasNext": true }

// 실패
{ "success": false, "error": { "code": "POST_NOT_FOUND", "message": "게시글을 찾을 수 없습니다." } }
```

### 에러 코드 목록

| Code | HTTP | 설명 |
|---|---|---|
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `FORBIDDEN` | 403 | 권한 없음 (댓글 비밀번호 불일치 포함) |
| `POST_NOT_FOUND` | 404 | 게시글 없음 |
| `SLUG_DUPLICATE` | 409 | slug 중복 |
| `INVALID_STATUS_TRANSITION` | 422 | 허용되지 않는 상태 전이 |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | 지원하지 않는 파일 형식 |
| `FILE_TOO_LARGE` | 413 | 파일 크기 초과 (10MB) |
| `RATE_LIMIT_EXCEEDED` | 429 | 요청 횟수 초과 |
| `VALIDATION_ERROR` | 400 | 입력값 검증 실패 |

---

## Auth API (`/admin/auth`)

### POST `/admin/auth/login`
```json
// Request
{ "username": "admin", "password": "password" }

// Response 200 — Set-Cookie: accessToken, refreshToken (HttpOnly Secure)
{ "success": true, "data": { "username": "admin" } }
```

### POST `/admin/auth/logout`
```json
// Response 200 — 쿠키 삭제
{ "success": true, "data": null }
```

### POST `/admin/auth/refresh`
```json
// Request — refreshToken 쿠키 자동 전송
// Response 200 — 새 accessToken 쿠키 발급, refreshToken Rotation
{ "success": true, "data": null }
```

### GET `/admin/auth/me`
```json
// Response 200
{ "success": true, "data": { "username": "admin" } }
```

---

## Post API

### GET `/posts?cursor=&size=10&category=&tag=`
```json
{
  "success": true,
  "data": [
    {
      "id": 1, "slug": "my-first-post", "title": "첫 번째 글",
      "excerpt": "글 요약...", "thumbnailUrl": "https://...",
      "category": { "id": 1, "name": "개발", "slug": "dev" },
      "tags": [{ "id": 1, "name": "Spring", "slug": "spring" }],
      "publishedAt": "2026-04-21T09:00:00Z",
      "readingTime": 5
    }
  ],
  "cursor": "1", "hasNext": false
}
```

### GET `/posts/{slug}`
```json
{
  "success": true,
  "data": {
    "id": 1, "uuid": "...", "slug": "my-first-post", "title": "첫 번째 글",
    "content": "# 마크다운 내용...", "excerpt": "...",
    "thumbnailUrl": "https://...", "viewCount": 42,
    "category": { "id": 1, "name": "개발", "slug": "dev" },
    "tags": [{ "id": 1, "name": "Spring", "slug": "spring" }],
    "series": { "id": 1, "title": "Spring 입문", "slug": "spring-intro", "position": 2, "total": 5 },
    "prev": { "slug": "prev-post", "title": "이전 글" },
    "next": { "slug": "next-post", "title": "다음 글" },
    "publishedAt": "2026-04-21T09:00:00Z",
    "readingTime": 5
  }
}
```
> `series`, `prev`, `next`: 해당 없으면 null

### GET `/search?q=&cursor=&size=10&tag=&category=`
```json
{
  "success": true,
  "data": [ /* Post 목록 (excerpt에 검색어 하이라이트 포함) */ ],
  "cursor": "5", "hasNext": true
}
```

### POST `/admin/posts` (ADMIN)
```json
// Request
{
  "title": "제목", "slug": "my-slug", "content": "# 마크다운",
  "status": "DRAFT", "excerpt": "요약",
  "thumbnailUrl": "https://...",
  "categoryId": 1, "tagIds": [1, 2], "seriesId": 1,
  "scheduledAt": null
}
// Response 201
{ "success": true, "data": { "id": 1, "slug": "my-slug" } }
```

### PUT `/admin/posts/{id}` (ADMIN)
> Request 필드 동일. Response 200.

### PATCH `/admin/posts/{id}/status` (ADMIN)
```json
// Request
{ "status": "PUBLISHED" }
// Response 200
{ "success": true, "data": { "id": 1, "status": "PUBLISHED", "publishedAt": "2026-04-21T09:00:00Z" } }
```

### DELETE `/admin/posts/{id}` (ADMIN)
```json
// Response 200
{ "success": true, "data": null }
```

---

## Tag API

### GET `/tags`
```json
{ "success": true, "data": [{ "id": 1, "name": "Spring", "slug": "spring", "postCount": 12 }] }
```

### DELETE `/admin/tags/{id}` (ADMIN)
> 연결된 post_tags 함께 삭제 (CASCADE)

---

## Category API

### GET `/categories`
```json
{ "success": true, "data": [{ "id": 1, "name": "개발", "slug": "dev", "postCount": 20 }] }
```

### POST `/admin/categories` (ADMIN)
```json
// Request
{ "name": "개발", "slug": "dev" }
```

---

## Series API

### GET `/series`
```json
{
  "success": true,
  "data": [{
    "id": 1, "title": "Spring 입문", "slug": "spring-intro",
    "description": "...", "postCount": 5
  }]
}
```

### GET `/series/{slug}`
```json
{
  "success": true,
  "data": {
    "id": 1, "title": "Spring 입문", "slug": "spring-intro", "description": "...",
    "posts": [
      { "position": 1, "slug": "spring-intro-1", "title": "1편: 시작하기" },
      { "position": 2, "slug": "spring-intro-2", "title": "2편: DI 이해" }
    ]
  }
}
```

### POST/PUT/DELETE `/admin/series`, `/admin/series/{id}` (ADMIN)
> 시리즈 CRUD + 게시글 순서 배치

---

## Comment API

### GET `/posts/{slug}/comments?cursor=&size=20`
```json
{
  "success": true,
  "data": [{
    "id": 1, "nickname": "독자", "content": "좋은 글이에요!",
    "createdAt": "2026-04-21T10:00:00Z"
  }],
  "cursor": "1", "hasNext": false
}
```

### POST `/posts/{slug}/comments`
```json
// Request
{ "nickname": "독자", "password": "1234", "content": "좋은 글이에요!" }
// Response 201
{ "success": true, "data": { "id": 1 } }
```

### PUT `/posts/{slug}/comments/{id}`
```json
// Request
{ "password": "1234", "content": "수정된 내용" }
```

### DELETE `/posts/{slug}/comments/{id}`
```json
// Request
{ "password": "1234" }
```

---

## Reaction API

### GET `/posts/{slug}/reactions`
```json
{ "success": true, "data": { "👍": 5, "❤️": 3, "🔥": 1 } }
```

### POST `/posts/{slug}/reactions`
```json
// Request — anonId 쿠키 자동 전송
{ "emoji": "👍" }
// Response 200 (토글: 없으면 추가, 있으면 취소)
{ "success": true, "data": { "added": true } }
```
> 지원 emoji: 👍 ❤️ 🔥 🤔 (고정 목록)

---

## Email Subscription API

### POST `/subscriptions`
```json
// Request
{ "email": "reader@example.com" }
// Response 201 — 인증 이메일 발송
{ "success": true, "data": { "message": "인증 메일을 발송했습니다." } }
```

### GET `/subscriptions/verify?token=`
```json
// Response 200
{ "success": true, "data": { "message": "구독이 완료되었습니다." } }
```

### DELETE `/subscriptions/unsubscribe?token=`
> 구독 해지

---

## Analytics API

### POST `/analytics/events`
```json
// Request — anonId 쿠키 자동 전송
{ "type": "POST_VIEW", "postId": 1, "path": "/blog/my-first-post" }
// Response 200
{ "success": true, "data": null }
```

### GET `/admin/analytics/dashboard` (ADMIN)
```json
{
  "success": true,
  "data": {
    "today": { "pv": 120, "uv": 45 },
    "total": { "pv": 50000, "uv": 12000 },
    "topPosts": [{ "slug": "...", "title": "...", "viewCount": 300 }]
  }
}
```

### GET `/admin/analytics/dau?from=2026-04-01&to=2026-04-21` (ADMIN)
```json
{ "success": true, "data": [{ "date": "2026-04-21", "pv": 120, "uv": 45 }] }
```

---

## Image Upload API

### POST `/admin/upload/image` (ADMIN)
```
Content-Type: multipart/form-data
Body: file (png|jpg|jpeg|gif, max 10MB)
```
```json
// Response 201
{ "success": true, "data": { "url": "https://s3.amazonaws.com/..." } }
```

---

## 운영 API

### GET `/health`
```json
{ "status": "UP", "db": "UP" }
```
