# Blog Backend

개인 블로그 플랫폼 백엔드 서버

🌐 **사이트**: [https://sevin.dev/blog](https://sevin.dev/blog)

## 🧪 테스트 섹션

이 섹션은 CodeRabbit 테스트를 위한 변경사항입니다.

## 📋 프로젝트 개요

Spring Boot 기반의 개인 블로그 플랫폼 백엔드 서버로, 포트폴리오와 블로그를 하나의 도메인으로 통합하는 슈퍼앱의 백엔드를 담당합니다.

## 🛠 기술 스택

- **Framework**: Spring Boot
- **Language**: Java
- **Database**: PostgreSQL
- **Build Tool**: Gradle
- **Container**: Docker
- **Deployment**: AWS EC2

## 🔄 자동 버전 관리 시스템

이 프로젝트는 커밋할 때마다 자동으로 버전과 README를 업데이트합니다.

### 버전 증가 규칙

- **feat**: 마이너 버전 증가 (1.0.0 → 1.1.0)
- **fix, refactor, perf, style, docs, chore, test**: 패치 버전 증가 (1.0.0 → 1.0.1)
- **첫 배포**: 0.0.1 → 1.0.0으로 자동 설정

### 사용 방법

1. **Git Hook 설정** (최초 1회만 실행)
   ```bash
   ./gradlew setupHook
   ```

2. **커밋 시 자동 업데이트**
   - 커밋 메시지를 컨벤션에 맞게 작성 (예: `feat: 새로운 기능 추가`)
   - 커밋 후 자동으로:
     - `build.gradle`의 버전이 업데이트됩니다
     - `README.md`에 최근 커밋 내역이 추가됩니다
     - `CHANGELOG.md`에 전체 기록이 추가됩니다

3. **수동 업데이트** (필요시)
   ```bash
   ./gradlew updateReadme
   ```

### 커밋 메시지 컨벤션

- `feat: 새로운 기능 추가`
- `fix: 버그 수정`
- `refactor: 코드 리팩토링`
- `perf: 성능 개선`
- `style: 스타일 변경`
- `docs: 문서 수정`
- `chore: 기타 작업`

---

## 📅 최근 변경사항

<!-- 최근 5개 버전만 표시됩니다. 전체 기록은 CHANGELOG.md를 참고하세요. -->

---

전체 변경사항은 [CHANGELOG.md](./CHANGELOG.md)를 참고하세요.
