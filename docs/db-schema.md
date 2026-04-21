# DB Schema 명세

## ERD 개요

```
admin_users ──< refresh_tokens
posts >──< tags (post_tags)
posts >── categories
posts ──< comments
posts ──< analytics_events
images (독립)
analytics_daily / analytics_monthly (집계)
```

---

## 테이블 정의

### admin_users
```sql
id            BIGSERIAL PRIMARY KEY
username      VARCHAR(50) NOT NULL UNIQUE
password      VARCHAR(255) NOT NULL  -- bcrypt
created_at    TIMESTAMP NOT NULL DEFAULT now()
updated_at    TIMESTAMP NOT NULL DEFAULT now()
```

### refresh_tokens
```sql
id            BIGSERIAL PRIMARY KEY
admin_id      BIGINT NOT NULL REFERENCES admin_users(id)
token_hash    VARCHAR(255) NOT NULL UNIQUE  -- SHA-256 해시 저장
expires_at    TIMESTAMP NOT NULL
created_at    TIMESTAMP NOT NULL DEFAULT now()
```
> Rotation 정책: 재발급 시 기존 레코드 삭제 후 신규 삽입

---

### posts
```sql
id             BIGSERIAL PRIMARY KEY
uuid           UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()
title          VARCHAR(500) NOT NULL
slug           VARCHAR(500) NOT NULL UNIQUE
content        TEXT NOT NULL           -- Markdown 원문
status         VARCHAR(20) NOT NULL    -- DRAFT|PUBLISHED|SCHEDULED|PRIVATE|DELETED
excerpt        VARCHAR(500)            -- NULL이면 content 앞 200자 자동
thumbnail_url  VARCHAR(2000)
published_at   TIMESTAMP
scheduled_at   TIMESTAMP
search_vector  TSVECTOR                -- Full-text 검색용 (title+content, 트리거로 갱신)
created_at     TIMESTAMP NOT NULL DEFAULT now()
updated_at     TIMESTAMP NOT NULL DEFAULT now()
deleted_at     TIMESTAMP
```

**인덱스**
```sql
CREATE INDEX idx_posts_slug ON posts(slug);
CREATE INDEX idx_posts_status ON posts(status);
CREATE INDEX idx_posts_published_at ON posts(published_at DESC);
CREATE INDEX idx_posts_scheduled_at ON posts(scheduled_at) WHERE status = 'SCHEDULED';
CREATE INDEX idx_posts_search ON posts USING GIN(search_vector);
```

**트리거** (search_vector 자동 갱신)
```sql
CREATE TRIGGER posts_search_vector_update
  BEFORE INSERT OR UPDATE ON posts
  FOR EACH ROW EXECUTE FUNCTION
    tsvector_update_trigger(search_vector, 'pg_catalog.korean', title, content);
```
> 한국어 형태소 분석기(pgroonga 또는 pg_bigm) 미설치 시 기본 simple 사용

---

### categories
```sql
id          BIGSERIAL PRIMARY KEY
name        VARCHAR(100) NOT NULL UNIQUE
slug        VARCHAR(100) NOT NULL UNIQUE
created_at  TIMESTAMP NOT NULL DEFAULT now()
updated_at  TIMESTAMP NOT NULL DEFAULT now()
```

### tags
```sql
id          BIGSERIAL PRIMARY KEY
name        VARCHAR(100) NOT NULL UNIQUE
slug        VARCHAR(100) NOT NULL UNIQUE
created_at  TIMESTAMP NOT NULL DEFAULT now()
```

### post_tags (junction)
```sql
post_id  BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE
tag_id   BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE
PRIMARY KEY (post_id, tag_id)
```

### post_categories
```sql
post_id      BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE PRIMARY KEY
category_id  BIGINT NOT NULL REFERENCES categories(id)
```

---

### series
```sql
id           BIGSERIAL PRIMARY KEY
title        VARCHAR(500) NOT NULL
slug         VARCHAR(500) NOT NULL UNIQUE
description  TEXT
created_at   TIMESTAMP NOT NULL DEFAULT now()
updated_at   TIMESTAMP NOT NULL DEFAULT now()
```

### series_posts (순서 있는 다대다)
```sql
series_id   BIGINT NOT NULL REFERENCES series(id) ON DELETE CASCADE
post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE
position    INT NOT NULL  -- 시리즈 내 순서
PRIMARY KEY (series_id, post_id)
```

---

### comments
```sql
id          BIGSERIAL PRIMARY KEY
post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE
nickname    VARCHAR(50) NOT NULL
password    VARCHAR(255) NOT NULL  -- bcrypt
content     TEXT NOT NULL
status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'  -- ACTIVE|DELETED
ip          INET                   -- Rate limit용 (로깅 목적)
created_at  TIMESTAMP NOT NULL DEFAULT now()
updated_at  TIMESTAMP NOT NULL DEFAULT now()
```

---

### reactions
```sql
id          BIGSERIAL PRIMARY KEY
post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE
anon_id     VARCHAR(100) NOT NULL  -- 익명 식별자
emoji       VARCHAR(10) NOT NULL   -- 👍 ❤️ 🔥 등
created_at  TIMESTAMP NOT NULL DEFAULT now()
UNIQUE (post_id, anon_id, emoji)   -- 중복 방지
```

---

### email_subscriptions
```sql
id              BIGSERIAL PRIMARY KEY
email           VARCHAR(255) NOT NULL UNIQUE
verified        BOOLEAN NOT NULL DEFAULT false
verify_token    VARCHAR(100)            -- 이메일 인증 토큰
subscribed_at   TIMESTAMP
unsubscribed_at TIMESTAMP
created_at      TIMESTAMP NOT NULL DEFAULT now()
```

---

### images
```sql
id            BIGSERIAL PRIMARY KEY
original_name VARCHAR(255) NOT NULL
s3_key        VARCHAR(1000) NOT NULL UNIQUE
s3_url        VARCHAR(2000) NOT NULL
file_size     BIGINT NOT NULL   -- bytes
content_type  VARCHAR(100) NOT NULL
created_at    TIMESTAMP NOT NULL DEFAULT now()
```

---

### analytics_events
```sql
id          BIGSERIAL PRIMARY KEY
type        VARCHAR(50) NOT NULL   -- PAGE_VIEW|POST_VIEW|SEARCH
anon_id     VARCHAR(100)
post_id     BIGINT REFERENCES posts(id) ON DELETE SET NULL
path        VARCHAR(2000)
referrer    VARCHAR(2000)
ip          INET
user_agent  TEXT
created_at  TIMESTAMP NOT NULL DEFAULT now()
```

**파티셔닝** (데이터 증가 대비)
```sql
-- 월별 파티션 권장 (추후 적용)
PARTITION BY RANGE (created_at)
```

**보관 정책**: 1년 후 삭제 (스케줄러)

### analytics_daily
```sql
id      BIGSERIAL PRIMARY KEY
date    DATE NOT NULL UNIQUE
pv      BIGINT NOT NULL DEFAULT 0
uv      BIGINT NOT NULL DEFAULT 0
```

### analytics_monthly
```sql
id    BIGSERIAL PRIMARY KEY
year  INT NOT NULL
month INT NOT NULL
pv    BIGINT NOT NULL DEFAULT 0
uv    BIGINT NOT NULL DEFAULT 0
UNIQUE (year, month)
```

### post_stats (게시글별 집계, denormalized)
```sql
post_id    BIGINT PRIMARY KEY REFERENCES posts(id) ON DELETE CASCADE
view_count BIGINT NOT NULL DEFAULT 0
updated_at TIMESTAMP NOT NULL DEFAULT now()
```
> analytics_events에서 집계하기 무거우므로 별도 테이블로 관리
