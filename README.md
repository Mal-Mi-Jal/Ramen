# 🍜 RamenLog - 진짜 라멘 덕후들을 위한 맛집 인증 앱

> "먹어본 사람만 리뷰할 수 있다" — GPS + 체류시간 기반 방문 인증 시스템

<br>

## 📖 프로젝트 소개

RamenLog는 라멘 맛집을 직접 방문한 사람만 리뷰를 남길 수 있는 신뢰 기반 라멘 맛집 추천 앱입니다.  
허위 리뷰 없이, 실제로 줄 서서 먹어본 사람들의 솔직한 후기만 모입니다.

<br>

## ✨ 주요 기능

### 👤 회원 관리
- 이메일 기반 회원가입 / 로그인
- 프로필 관리 및 내 리뷰 히스토리

### 📍 방문 인증 시스템

```
[ 라멘집 반경 50m 진입 감지 ]
           ↓
  [ 체류 시간 측정 시작 ]
           ↓
  [ 20분 이상 체류 확인 ]
           ↓
     [ 리뷰 작성 가능! ]
```

| 인증 조건 | 상세 |
|-----------|------|
| 📡 위치 기반 | 라멘집 반경 50m 이내 GPS 감지 |
| ⏱️ 체류 시간 | 최소 20분 이상 머물러야 인증 완료 |
| 🔮 향후 계획 | 영수증 OCR 인증, 카드 결제 연동 등 보다 확실한 방문 인증 수단으로 업그레이드 예정 |

> 💡 현재 GPS + 체류시간 방식은 1차 구현입니다. 단순히 근처를 지나가거나 잠깐 들른 경우를 걸러내기 위해 20분 체류 조건을 적용했습니다. 추후 영수증 인증 등 보다 확실한 수단으로 개선할 예정입니다.

<br>

### ⭐ 리뷰 & 커뮤니티

- 별점 (1~5점) 및 텍스트 리뷰 작성
- 재방문 의사 표시 (또 갈게요 / 글쎄요 / 안 갈 것 같아요)
- 사진 업로드 (방문 인증된 유저만 가능)
- 리뷰에 댓글 달기
- 리뷰 추천(좋아요) 기능

### 🗺️ 지도 & 검색 (카카오맵 API)

- 현재 위치 기반 주변 라멘집 탐색
- 지도 위에서 라멘집 위치 확인
- 가게명 / 지역명으로 키워드 검색
- 지도 마커 클릭 시 간단 정보 미리보기

<br>

## 🛠️ 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Kotlin |
| **지도 API** | Kakao Maps SDK |
| **위치 서비스** | Android Location API (GPS) |
| **인증** | 이메일 / 소셜 로그인 (예정) |

<br>

## 🗂️ 프로젝트 구조 (예정)

```
RamenLog/
├── app/
│   ├── src/main/
│   │   ├── java/com/ramenlog/
│   │   │   ├── auth/          # 로그인 / 회원가입
│   │   │   ├── map/           # 카카오맵 연동
│   │   │   ├── review/        # 리뷰 작성 & 목록
│   │   │   ├── verification/  # GPS 방문 인증 로직
│   │   │   └── community/     # 댓글 & 추천
│   │   └── res/
└── README.md
```

<br>

## DB 모델링

<img width="2146" height="1151" alt="Untitled" src="https://github.com/user-attachments/assets/bff7b6ff-9d2d-493d-b56a-ee82c9b8e8dc" />

<br>

## 🚀 개발 로드맵

- [x] 프로젝트 기획 및 설계
- [ ] 회원가입 / 로그인 구현
- [ ] 카카오맵 API 연동
- [ ] GPS 방문 인증 (50m + 20분 체류)
- [ ] 리뷰 작성 / 별점 / 사진 업로드
- [ ] 댓글 & 추천 기능
- [ ] 재방문 의사 표시
- [ ] 영수증 인증 시스템 (고도화)

<br>

# 📡 RamenLog API 명세서

## 공통 사항

**Base URL**
```
https://api.ramenlog.com/v1
```

**인증 방식**
- JWT Bearer Token 사용
- 인증이 필요한 API는 요청 헤더에 토큰 포함

```
Authorization: Bearer {access_token}
```

**공통 응답 형식**
```json
{
  "success": true,
  "data": { },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

**공통 에러 응답**
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

**HTTP 상태 코드**

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복 요청 (예: 이미 추천한 리뷰) |
| 500 | 서버 오류 |

---

## 1. 인증 (Auth)

### 회원가입
```
POST /auth/signup
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password1234!",
  "nickname": "라멘마스터"
}
```

**Response `201`**
```json
{
  "success": true,
  "data": {
    "user_id": "uuid",
    "email": "user@example.com",
    "nickname": "라멘마스터"
  },
  "message": "회원가입이 완료되었습니다."
}
```

---

### 로그인
```
POST /auth/login
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password1234!"
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGci...",
    "refresh_token": "eyJhbGci...",
    "expires_in": 3600
  }
}
```

---

### 토큰 재발급
```
POST /auth/refresh
```

**Request Body**
```json
{
  "refresh_token": "eyJhbGci..."
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGci...",
    "expires_in": 3600
  }
}
```

---

### 로그아웃
```
POST /auth/logout
```
🔒 인증 필요

**Response `200`**
```json
{
  "success": true,
  "message": "로그아웃 되었습니다."
}
```

---

## 2. 사용자 (Users)

### 내 프로필 조회
```
GET /users/me
```
🔒 인증 필요

**Response `200`**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "nickname": "라멘마스터",
    "profile_image_url": "https://...",
    "created_at": "2025-01-01T00:00:00Z"
  }
}
```

---

### 프로필 수정
```
PATCH /users/me
```
🔒 인증 필요

**Request Body**
```json
{
  "nickname": "신라멘마스터",
  "profile_image_url": "https://..."
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "nickname": "신라멘마스터",
    "profile_image_url": "https://..."
  }
}
```

---

## 3. 라멘집 (Restaurants)

### 주변 라멘집 검색 (카카오맵 API 연동)
```
GET /restaurants/nearby
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| latitude | float | ✅ | 현재 위도 |
| longitude | float | ✅ | 현재 경도 |
| radius | int | ❌ | 검색 반경(m), 기본값 1000 |
| keyword | string | ❌ | 검색 키워드 |

**Response `200`**
```json
{
  "success": true,
  "data": {
    "restaurants": [
      {
        "id": "uuid",
        "kakao_place_id": "12345678",
        "name": "라멘 홍대점",
        "address": "서울 마포구 ...",
        "latitude": 37.5563,
        "longitude": 126.9236,
        "phone": "02-1234-5678",
        "distance_meters": 320,
        "review_count": 42,
        "average_rating": 4.3
      }
    ]
  }
}
```

---

### 라멘집 상세 조회
```
GET /restaurants/{restaurant_id}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "kakao_place_id": "12345678",
    "name": "라멘 홍대점",
    "address": "서울 마포구 ...",
    "latitude": 37.5563,
    "longitude": 126.9236,
    "phone": "02-1234-5678",
    "review_count": 42,
    "average_rating": 4.3,
    "revisit_rate": 0.85
  }
}
```

---

## 4. 방문 인증 (Visit Verification)

> 리뷰 작성 전 반드시 방문 인증을 완료해야 합니다.
> 라멘집 반경 **50m** 이내 진입 후 **20분 이상** 체류 시 인증이 완료됩니다.

### 방문 인증 시작 (진입 감지)
```
POST /verifications/enter
```
🔒 인증 필요

**Request Body**
```json
{
  "restaurant_id": "uuid",
  "latitude": 37.5563,
  "longitude": 126.9236
}
```

**Response `201`**
```json
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "status": "pending",
    "entered_at": "2025-01-01T12:00:00Z",
    "required_stay_seconds": 1200
  },
  "message": "방문 인증이 시작되었습니다. 20분 이상 체류해주세요."
}
```

**에러 케이스**

| 코드 | 설명 |
|------|------|
| `OUT_OF_RANGE` | 라멘집 반경 50m 밖에 있음 |
| `ALREADY_PENDING` | 이미 진행 중인 인증 존재 |

---

### 방문 인증 완료 확인
```
POST /verifications/{verification_id}/verify
```
🔒 인증 필요

**Request Body**
```json
{
  "latitude": 37.5563,
  "longitude": 126.9236
}
```

**Response `200` — 인증 성공**
```json
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "status": "verified",
    "stay_duration_seconds": 1380,
    "verified_at": "2025-01-01T12:23:00Z"
  },
  "message": "방문 인증이 완료되었습니다! 리뷰를 남겨주세요."
}
```

**Response `200` — 인증 실패 (체류 시간 부족)**
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_STAY_TIME",
    "message": "아직 체류 시간이 부족합니다.",
    "data": {
      "current_stay_seconds": 540,
      "required_stay_seconds": 1200,
      "remaining_seconds": 660
    }
  }
}
```

---

### 내 방문 인증 내역 조회
```
GET /verifications/me
```
🔒 인증 필요

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| status | string | ❌ | pending / verified / failed |
| page | int | ❌ | 페이지 번호, 기본값 1 |
| size | int | ❌ | 페이지 크기, 기본값 20 |

---
### 진행 중인 방문 인증 조회
```
GET /verifications/current

🔒 인증 필요
Response 200
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "restaurant_id": "uuid",
    "status": "PENDING",
    "remaining_seconds": 735,
    "entered_at": "2025-01-01T12:00:00Z"
  }
}

진행 중인 인증이 없으면
{
  "success": true,
  "data": null
}

또는

{
  "success": false,
  "code": "NO_ACTIVE_VERIFICATION",
  "message": "진행 중인 방문 인증이 없습니다."
}

```
## 5. 리뷰 (Reviews)

### 리뷰 작성
```
POST /reviews
```
🔒 인증 필요

> `verification_id`는 `status: verified` 상태의 인증만 허용됩니다.

**Request Body**
```json
{
  "restaurant_id": "uuid",
  "verification_id": "uuid",
  "rating": 5,
  "content": "국물이 진하고 면이 쫄깃해요. 차슈도 두툼하고 맛있었습니다!",
  "revisit_intention": true
}
```

**Response `201`**
```json
{
  "success": true,
  "data": {
    "review_id": "uuid",
    "restaurant_id": "uuid",
    "rating": 5,
    "content": "국물이 진하고...",
    "revisit_intention": true,
    "created_at": "2025-01-01T12:30:00Z"
  }
}
```

**에러 케이스**

| 코드 | 설명 |
|------|------|
| `VERIFICATION_REQUIRED` | 방문 인증이 완료되지 않음 |
| `ALREADY_REVIEWED` | 해당 인증으로 이미 리뷰 작성함 |
| `INVALID_RATING` | 별점은 1~5 사이여야 함 |

---

### 리뷰 사진 업로드
```
POST /reviews/{review_id}/photos
```
🔒 인증 필요 (본인 리뷰만 가능)

**Request** — `multipart/form-data`

| 필드 | 타입 | 설명 |
|------|------|------|
| photos | file[] | 이미지 파일 (jpg, png, webp) |

**Response `201`**
```json
{
  "success": true,
  "data": {
    "photos": [
      {
        "id": "uuid",
        "image_url": "https://...",
        "order_index": 0
      }
    ]
  }
}
```

---

### 라멘집 리뷰 목록 조회
```
GET /restaurants/{restaurant_id}/reviews
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| sort | string | ❌ | latest / rating_desc / likes (기본: latest) |
| page | int | ❌ | 기본값 1 |
| size | int | ❌ | 기본값 20 |

**Response `200`**
```json
{
  "success": true,
  "data": {
    "reviews": [
      {
        "id": "uuid",
        "user": {
          "id": "uuid",
          "nickname": "라멘마스터",
          "profile_image_url": "https://..."
        },
        "rating": 5,
        "content": "국물이 진하고...",
        "revisit_intention": true,
        "like_count": 12,
        "photos": [
          { "id": "uuid", "image_url": "https://...", "order_index": 0 }
        ],
        "comment_count": 3,
        "created_at": "2025-01-01T12:30:00Z"
      }
    ],
    "pagination": {
      "total": 42,
      "page": 1,
      "size": 20,
      "total_pages": 3
    }
  }
}
```

---

### 리뷰 상세 조회
```
GET /reviews/{review_id}
```

---

### 리뷰 수정
```
PATCH /reviews/{review_id}
```
🔒 인증 필요 (본인 리뷰만 가능)

**Request Body**
```json
{
  "rating": 4,
  "content": "두 번째 방문했는데 첫 번째보다는 조금 아쉬웠어요.",
  "revisit_intention": true
}
```

---

### 리뷰 삭제
```
DELETE /reviews/{review_id}
```
🔒 인증 필요 (본인 리뷰만 가능)

**Response `200`**
```json
{
  "success": true,
  "message": "리뷰가 삭제되었습니다."
}
```

---

### 리뷰 추천
```
POST /reviews/{review_id}/likes
```
🔒 인증 필요

**Response `201`**
```json
{
  "success": true,
  "data": {
    "review_id": "uuid",
    "like_count": 13
  }
}

```

**에러 케이스**

| 코드 | 설명 |
|------|------|
| `ALREADY_LIKED` | 이미 추천한 리뷰 (409) |
| `CANNOT_LIKE_OWN` | 본인 리뷰 추천 불가 |

---

### 리뷰 추천 취소
```
DELETE /reviews/{review_id}/likes
```
🔒 인증 필요

---

## 6. 댓글 (Comments)

### 댓글 작성
```
POST /reviews/{review_id}/comments
```
🔒 인증 필요

**Request Body**
```json
{
  "content": "저도 다녀왔는데 정말 맛있었어요!"
}
```

**Response `201`**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "user": {
      "id": "uuid",
      "nickname": "라멘덕후",
      "profile_image_url": "https://..."
    },
    "content": "저도 다녀왔는데 정말 맛있었어요!",
    "created_at": "2025-01-01T13:00:00Z"
  }
}
```

---

### 댓글 목록 조회
```
GET /reviews/{review_id}/comments
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| page | int | ❌ | 기본값 1 |
| size | int | ❌ | 기본값 30 |

---

### 댓글 수정
```
PATCH /reviews/{review_id}/comments/{comment_id}
```
🔒 인증 필요 (본인 댓글만 가능)

**Request Body**
```json
{
  "content": "수정된 댓글 내용입니다."
}
```

---

### 댓글 삭제
```
DELETE /reviews/{review_id}/comments/{comment_id}
```
🔒 인증 필요 (본인 댓글만 가능)

---

## 7. 방문 기록 (Visit History)

### 내 방문 기록 조회
```
GET /users/me/visit-history
```
🔒 인증 필요

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| page | int | ❌ | 기본값 1 |
| size | int | ❌ | 기본값 20 |

**Response `200`**
```json
{
  "success": true,
  "data": {
    "visit_history": [
      {
        "restaurant": {
          "id": "uuid",
          "name": "이치란 라멘 홍대점",
          "address": "서울 마포구 ..."
        },
        "visit_count": 3,
        "last_visited_at": "2025-01-01T12:30:00Z"
      }
    ],
    "pagination": {
      "total": 15,
      "page": 1,
      "size": 20,
      "total_pages": 1
    }
  }
}
```

---

### 특정 라멘집 방문 기록 조회
```
GET /restaurants/{restaurant_id}/visit-history/me
```
🔒 인증 필요

**Response `200`**
```json
{
  "success": true,
  "data": {
    "restaurant_id": "uuid",
    "visit_count": 3,
    "last_visited_at": "2025-01-01T12:30:00Z",
    "first_visited_at": "2024-06-01T11:00:00Z"
  }
}
```

## 📌 참고 & 라이선스

- [카카오맵 API 문서](https://apis.map.kakao.com/android/)
- 이 프로젝트는 개인 학습 및 포트폴리오 목적으로 제작되었습니다.
