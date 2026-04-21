# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew clean build          # 빌드 (테스트 포함)
./gradlew build -x test        # 빌드 (테스트 제외)
./gradlew test                 # 전체 테스트
./gradlew test --tests "sevin.dev.blog.BlogApplicationTests"  # 단일 테스트
docker compose up              # 로컬 실행 (PostgreSQL 포함)
./gradlew setupHook            # Git Hook 초기 설정 (자동 버전 관리, 최초 1회)
```

## Architecture

Spring Boot 4.0.2 + Java 17, context path `/blog`, WAR 패키징.

| 문서 | 내용 |
|---|---|
| [`docs/requirements.md`](docs/requirements.md) | 기능 요구사항 명세 |
| [`docs/api-spec.md`](docs/api-spec.md) | API 엔드포인트 + Request/Response 상세 |
| [`docs/db-schema.md`](docs/db-schema.md) | 테이블 정의 및 인덱스 |
| [`docs/roadmap.md`](docs/roadmap.md) | 기능 로드맵 및 개발 우선순위 |

### 프로파일

| 프로파일 | 환경변수 파일 | DB ddl-auto |
|---|---|---|
| `local` | `.env.local` | update |
| `production` | `.env.production` | validate |

`DotEnvConfig`가 기동 시 `.env.{profile}` 로드 → 시스템 프로퍼티 등록 (시스템 환경변수 우선).

### 주요 환경변수

```
DB_URL, DB_USERNAME, DB_PASSWORD
```

### 브랜치 전략

```
feat/<기능명> → develop → main
```
- 모든 기능 작업은 `feat/` 브랜치에서 시작
- `develop`으로 PR 머지
- 배포는 `main` 머지 시 GitHub Actions 트리거

### 로컬 DB (compose.yaml)

`localhost:5432`, DB: `blog_local`, user/password: `postgres`

### 자동 버전 관리

`scripts/update-readme.gradle` → post-commit hook:
- `feat:` → 마이너 버전 증가
- 나머지 타입 → 패치 버전 증가
- `build.gradle`, `README.md`, `CHANGELOG.md` 자동 업데이트
