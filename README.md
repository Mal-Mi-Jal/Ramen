# 🍜 RamenLog — 방문 인증 기반 라멘 리뷰 서비스 (백엔드)

> "먹어본 사람만 리뷰할 수 있다" — GPS 거리 + 체류 시간으로 방문을 인증해야 리뷰를 쓸 수 있는 라멘 맛집 서비스의 **Spring Boot 백엔드**입니다.

- 프론트엔드: [Mal-Mi-Jal/Ramen-frontend](https://github.com/Mal-Mi-Jal/Ramen-frontend) (서비스 전체 소개 포함)
- 포트폴리오: [mal-mi-jal.github.io](https://mal-mi-jal.github.io)
- 기간: 2025 – 2026 · 1인 개발 (백엔드·프론트엔드·DB 설계·배포)

> ℹ️ 백엔드 서버(Railway)는 무료 이용 기간이 끝나 현재 꺼져 있습니다. 프론트엔드 화면만 확인할 수 있습니다.

<br>

## 📖 왜 만들었나

라멘 맛집 정보는 인스타그램 공지와 출처가 불분명한 리뷰에 흩어져 있습니다. RamenLog는 **실제로 방문한 사람만** 리뷰를 남길 수 있게 해서 리뷰의 신뢰도를 높이는 것을 목표로 했습니다. 그래서 단순 CRUD 게시판이 아니라 **위치 기반 도메인 로직(방문 인증)** 을 핵심으로 설계했습니다.

<br>

## 🛠️ 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Kotlin 2.2 (JDK 21) |
| Framework | Spring Boot 4, Spring Data JPA, Spring Security, Validation |
| 인증 | JWT (JJWT) Access / Refresh Token, BCrypt |
| DB | PostgreSQL (Supabase) |
| 외부 API | Kakao Local API (WebClient) |
| 테스트 | JUnit 5, Mockito |
| 배포 | Docker 멀티스테이지 빌드, Railway (프론트엔드는 Vercel) |

<br>

## 🏗️ 아키텍처

```
[브라우저 (Vercel, Vanilla JS)]
        │  REST API + JWT (Authorization: Bearer)
        ▼
[Spring Boot 백엔드 (Railway)] ──── Kakao Local API (라멘집 검색)
        │  JPA
        ▼
[PostgreSQL (Supabase)]          리뷰 사진: 프론트가 Supabase Storage에 직접 업로드, 서버는 URL만 저장
```

**패키지 구조** (`src/main/kotlin/com/ramenlog/ramen/`)

```
api/
├── auth/            # 로그인·회원가입 컨트롤러, security/ (JwtProvider, JwtAuthenticationFilter, SecurityConfig)
├── shop/            # 라멘집 검색·상세 컨트롤러
├── verification/    # 방문 인증 컨트롤러
├── review/          # 리뷰·댓글·추천 컨트롤러
├── user/ favorite/ visit/
├── application/     # 서비스 계층 (도메인별 *Service, VerificationScheduler)
└── common/          # GlobalExceptionHandler, 도메인 예외
domain/              # 엔티티 + JPA Repository (user, restaurant, visit, review, favorite)
util/LocationUtils   # 하버사인 거리 계산
```

<br>

## ✨ 핵심 설계

### 1. 방문 인증 (GPS 거리 + 체류 시간)

```
POST /verifications/enter (위도·경도)
   │  하버사인 거리 ≤ 50m ?  ── 아니면 OUT_OF_RANGE
   ▼
 PENDING (진입 시각 저장)
   │
   ├─ POST /verifications/{id}/verify  : 위치를 다시 확인 + 20분 경과 → VERIFIED
   └─ VerificationScheduler (@Scheduled, 5초마다) : 20분 지난 PENDING → VERIFIED + 방문 기록 누적
   ▼
 VERIFIED → 리뷰 작성 가능
```

- **하버사인 공식**: 위도가 높을수록 경도 1°의 길이가 줄어들어 평면 계산은 오차가 생깁니다. 구 위의 최단 거리로 계산했습니다 (`LocationUtils.calculateDistance`).
- **체류 시간은 서버 기준**: 진입 시각을 서버가 기록하고 그 기준으로 계산해서, 클라이언트 타이머를 조작해도 체류 시간이 줄지 않습니다.
- **기준값 외부화**: 반경 50m, 체류 1200초를 `application.yml`에서 환경 변수로 바꿀 수 있습니다 (`VERIFICATION_RADIUS_METERS`, `VERIFICATION_REQUIRED_STAY_SECONDS`).

### 2. 라멘집만 골라내는 카카오 검색

- 카카오 로컬 API는 "라면"으로 검색하면 분식집·굿즈샵까지 섞여 나옵니다. 실제 응답의 카테고리를 확인해서 **카테고리 경로의 마지막 분류가 정확히 "일본식라면"** 이고 **가게 이름에 검색어가 포함된** 곳만 남깁니다.
- 검색어를 확장(`○○`, `○○ 라멘`, `○○ 일본라면`)해 조회하고, 마지막 페이지(`is_end`)면 더 부르지 않습니다.
- 찾은 가게는 DB에 저장해서, **주변 라멘집 조회는 외부 API 없이** DB만으로 처리합니다.

### 3. 인증 · 보안

- Spring Security + JWT 필터(`OncePerRequestFilter`)로 세션 없이(stateless) 인증합니다. 조회 API만 공개하고 나머지는 인증이 필요합니다.
- Refresh Token은 DB에 저장하고 재발급할 때마다 교체합니다. 비밀번호는 BCrypt로 저장합니다.
- DB 접속 정보, JWT 비밀키, 카카오 키는 모두 환경 변수로 받고, 비밀값에는 기본값을 두지 않아 빠지면 시작부터 실패합니다.

### 4. 전역 예외 처리

- 서비스는 `OutOfRangeException`, `AlreadyLikedException` 같은 도메인 예외(12종)를 던지고, `@RestControllerAdvice`(`GlobalExceptionHandler`)가 HTTP 상태 코드와 에러 코드로 변환합니다.

<br>

## 🧪 테스트

JUnit 5 + Mockito로 서비스 계층 단위 테스트 **11개** (스프링·DB 없이 약 1초).

- `VerificationServiceTest` (8개): 반경 **안(약 40m)·밖(약 60m)**, 체류 시간 **기준 1초 전후**의 경계값 테스트, 완료 요청 시 위치 재확인
- `AuthServiceTest` (3개): 로그인 성공, 비밀번호 오류, 없는 계정

> 경계값 테스트를 추가하다가, 기존 테스트(거리 0m만 검사)가 놓친 **설정값 누락 버그**(완료 단계에 하드코딩된 반경이 남아 있었음)를 찾아 고쳤습니다.

```bash
./gradlew test --tests "*ServiceTest"
```

(`RamenApplicationTests`는 전체 컨텍스트를 띄우는 테스트라 DB가 필요합니다.)

<br>

## 🚀 실행 방법

필요한 것: JDK 21, PostgreSQL, 카카오 REST API 키

```bash
export DB_URL=jdbc:postgresql://localhost:5432/ramenlog
export DB_USERNAME=...
export DB_PASSWORD=...
export JWT_SECRET=...          # HS256용 32바이트 이상
export KAKAO_API_KEY=...       # 카카오 REST API 키
./gradlew bootRun
```

Docker:

```bash
docker build -t ramenlog .
docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -e JWT_SECRET=... -e KAKAO_API_KEY=... ramenlog
```

<br>

## 📡 API

| 기능 | 엔드포인트 |
|---|---|
| 인증 | `POST /auth/signup`, `/auth/login`, `/auth/refresh`, `/auth/logout` |
| 라멘집 | `GET /restaurants/nearby`, `/restaurants/search`, `/restaurants/{id}`, `/restaurants/{id}/reviews`, `POST /restaurants/{id}/favorite` |
| 방문 인증 | `POST /verifications/enter`, `POST /verifications/{id}/verify`, `GET /verifications/me`, `/verifications/current` |
| 리뷰 | `POST /reviews`, `GET·PATCH·DELETE /reviews/{id}`, `POST·DELETE /reviews/{id}/likes` |
| 댓글 | `POST·GET /reviews/{id}/comments`, `PATCH·DELETE /reviews/{id}/comments/{commentId}` |
| 사용자 | `GET·PATCH /users/me`, `GET /users/me/reviews`, `/me/visit-history`, `/me/favorites`, `/me/pending-review` |

구현 전에 먼저 작성한 상세 명세는 [docs/api-spec.md](docs/api-spec.md)에 있습니다. (구현하면서 달라진 부분을 문서 맨 위에 정리해 두었습니다.)

<br>

## ⚠️ 알려진 한계와 개선 계획

직접 코드를 다시 점검하면서 찾은 문제들입니다.

| 문제 | 현재 동작 | 개선 방향 |
|---|---|---|
| 스케줄러 자동 완료가 위치를 확인하지 않음 | 진입 후 바로 떠나도 20분이 지나면 VERIFIED | 주기적 위치 확인 또는 완료 시점 재검증 필수화, 장기적으로 영수증 OCR 인증 |
| 진입 거리를 클라이언트가 보낸 좌표로 계산 | GPS 좌표를 조작하면 반경 검사를 통과할 수 있음 | 위치 이력 패턴 검사, 영수증 인증 |
| 여러 예외가 `IllegalArgumentException` | 권한 없음·없는 리소스·중복이 모두 400 | `ErrorCode` enum + 공통 예외로 403/404/409 구분, `Exception` 처리기(500) 추가 |
| 주변 라멘집 조회의 N+1 | `findAll()`로 가게 전체를 읽고, 상위 30곳마다 리뷰 수·평점을 따로 조회(최대 61쿼리) | 위경도 범위 조건 + 인덱스, `IN (...) GROUP BY` 집계 쿼리 한 번 |
| Access / Refresh 토큰 구분 없음 | 두 토큰이 만료 시간만 다르고 필터가 종류를 확인하지 않음 | `type` 클레임을 넣고 필터는 Access만 허용, 로그아웃 시 블랙리스트(Redis) |
| `@Transactional` 내부 호출 | `startVerification`이 같은 클래스의 트랜잭션 메서드를 호출해 트랜잭션이 적용되지 않음 | 바깥 메서드에 트랜잭션 적용 또는 별도 빈으로 분리 |
| 운영 설정 미분리 | 공통 설정에 `ddl-auto: update`, `show-sql: true` | `application-prod.yml`(validate, SQL 로그 끔) + Flyway |

<br>

## 📌 참고

- [카카오 로컬 API](https://developers.kakao.com/docs/latest/ko/local/dev-guide)
- 개인 학습 및 포트폴리오 목적으로 제작했습니다.
