# 게시글 CRUD 설계 (Issue #15)

## 개요

ADMIN 전용 게시글 CRUD API 구현. 공개 목록/상세 조회는 #17에서 별도 처리.

---

## 도메인 모델

### Post 엔티티

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | `BIGINT` | PK, auto increment | 식별자 |
| `title` | `VARCHAR(255)` | NOT NULL | 제목 |
| `slug` | `VARCHAR(255)` | NOT NULL, UNIQUE | URL 식별자 |
| `content` | `TEXT` | NOT NULL | Markdown 본문 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'DRAFT' | 게시 상태 |
| `created_at` | `TIMESTAMP` | NOT NULL | 생성 시각 |
| `updated_at` | `TIMESTAMP` | NOT NULL | 수정 시각 |

### PostStatus (enum)

```java
public enum PostStatus {
    DRAFT,
    PUBLISHED,
    DELETED
}
```

- soft delete: `DELETE /admin/posts/{id}` → `status = DELETED`
- `DELETED` 상태 게시글은 조회 시 `POST_NOT_FOUND`와 동일하게 처리

---

## Slug 생성 규칙

- 제목에서 자동 생성 (생성 시 1회, 수정 시 변경 없음)
- 공백 → `-`
- 영문 → 소문자화
- 특수문자 제거 (`[^a-z0-9가-힣-]` 패턴 사용)
- 한글 제목 → 한글 그대로 (`봄날-산책`)
- 영문 제목 → 영문 소문자 (`spring-boot-getting-started`)
- 중복 시 `-2`, `-3`, ... suffix 추가

---

## API

### 공통

- Base path: `/blog/admin/posts`
- 모든 엔드포인트: `ROLE_ADMIN` 필요 (기존 `JwtAuthenticationFilter` 활용)
- 응답 포맷: 기존 `ApiResponse<T>` 공통 래퍼 사용

### 엔드포인트

#### POST `/admin/posts` — 게시글 생성

**Request**
```json
{
  "title": "Spring Boot 시작하기",
  "content": "# 본문\n마크다운 내용...",
  "status": "DRAFT"
}
```
- `status` 생략 시 `DRAFT`

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Spring Boot 시작하기",
    "slug": "spring-boot-시작하기",
    "content": "# 본문\n마크다운 내용...",
    "status": "DRAFT",
    "createdAt": "2026-04-21T10:00:00",
    "updatedAt": "2026-04-21T10:00:00"
  }
}
```

#### GET `/admin/posts` — 목록 조회

**Query Parameters**
- `status` (optional): `DRAFT` | `PUBLISHED` | `DELETED`
- `page` (default: 0), `size` (default: 10)

**Response** `200 OK` — `Page<PostResponse>`

#### GET `/admin/posts/{id}` — 단건 조회

**Response** `200 OK` — `PostResponse`  
`DELETED` 상태 포함하여 조회 가능 (ADMIN이므로)

#### PUT `/admin/posts/{id}` — 수정

**Request**
```json
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "status": "PUBLISHED"
}
```
- slug는 수정되지 않음

**Response** `200 OK` — `PostResponse`

#### DELETE `/admin/posts/{id}` — soft delete

**Response** `200 OK`
```json
{ "success": true, "data": null }
```

---

## 레이어 구조

```
controller/
  PostController.java
  dto/
    CreatePostRequest.java
    UpdatePostRequest.java
    PostResponse.java

service/
  PostService.java

domain/
  Post.java          (Entity)
  PostStatus.java    (Enum)
  PostRepository.java
```

---

## 예외 처리

모든 에러 코드는 기존 `ErrorCode` enum에 상수 추가:

| 에러 코드 | HTTP | 메시지 |
|---|---|---|
| `POST_NOT_FOUND` | 404 | 게시글을 찾을 수 없습니다 |

- 존재하지 않는 id 조회 → `POST_NOT_FOUND`
- ADMIN 목록 조회 시 `DELETED` 상태도 포함 (필터링은 `status` 파라미터로)

---

## DB 마이그레이션

`V2__create_post_table.sql`

```sql
CREATE TABLE post (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255)        NOT NULL,
    slug       VARCHAR(255)        NOT NULL UNIQUE,
    content    TEXT                NOT NULL,
    status     VARCHAR(20)         NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_post_status ON post (status);
CREATE INDEX idx_post_slug   ON post (slug);
```

---

## 테스트 전략

| 테스트 | 유형 |
|---|---|
| `PostControllerTest` | `@WebMvcTest` — 각 엔드포인트 성공/실패 케이스 |
| `PostServiceTest` | `@ExtendWith(MockitoExtension.class)` — slug 생성 로직, 중복 처리, soft delete |

---

## 상수 관리 원칙

- 게시 상태 → `PostStatus` enum
- 에러 코드 → `ErrorCode` enum (기존 패턴 유지)
- 매직 스트링/숫자 사용 금지
