# ⚖️ 밸런스 게임 (Balance Game)

> 한경 부트캠프 **4차 8시간 해커톤** — Java Spring Boot · Thymeleaf 기반 웹 서비스 구축 및 클라우드 배포 챌린지

사용자가 양자택일 **밸런스 게임 질문을 등록**하고, 다른 사용자들이 **투표**하면 결과가 양쪽 퍼센트 바로 표시되는 웹 서비스입니다. **투표해야 결과가 공개되는 블라인드 방식**으로 참여를 유도합니다.

- 🌐 **배포 URL**: https://carb-ones-rejected-percentage.trycloudflare.com  *(GCP VM + Cloudflare Tunnel, HTTPS)*
- 💻 **GitHub**: https://github.com/jeongws032700-source/balance-game
- 🧪 **데모 계정**: `demo1` ~ `demo8` / 비밀번호 `1234`

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **수행 주제** | 밸런스 게임 — 회원 인증 기반 양자택일 투표 CRUD 웹 서비스 |
| **사용 기술** | Java 17, Spring Boot 3.5.6, Spring Security, Thymeleaf, HTML/CSS, MariaDB, GCP(Compute Engine), Cloudflare(Tunnel/SSL) |
| **DB 접근** | Spring JDBC (`JdbcTemplate` + `RowMapper`) — **직접 SQL 작성, JPA 미사용** |
| **빌드/배포** | Gradle (Wrapper) / GCP VM + systemd 상시 가동 + cloudflared 터널 |

### 주요 기능 (CRUD)
- **Create**: 로그인 사용자의 질문 등록
- **Read**: 질문 목록(검색·정렬) / 상세 / 결과 집계 (비로그인도 조회 가능)
- **Update**: 작성자 본인의 질문 수정
- **Delete**: 작성자 본인의 질문 삭제 (연관 투표 함께 삭제)
- **투표**: 한 질문당 1인 1표, 투표 후 결과(퍼센트 바) 공개 (블라인드)
- **마이페이지**: 내가 만든 질문 / 내가 투표한 질문

---

## 2. 스프링 아키텍처 및 서비스 구조

### 2-1. 계층 구조 설계 개요

스프링 표준 **레이어드 아키텍처**를 준수하며, 요청은 단방향으로 흐릅니다.

```
[브라우저/Thymeleaf]
        │  (HTTP 요청 / 폼 데이터)
        ▼
   Controller  ── 요청 매핑, 입력 검증(@Valid), 화면 반환. 비즈니스 로직 없음
        │  (DTO 전달)
        ▼
    Service    ── 비즈니스 로직, 트랜잭션(@Transactional), 권한 검증, 결과 집계
        │  (도메인 객체)
        ▼
   Repository  ── JdbcTemplate으로 SQL 실행, ResultSet → 도메인 매핑(RowMapper)
        │
        ▼
     MariaDB
```

각 계층의 책임을 명확히 분리했습니다.
- **Controller**: URL ↔ 메서드 매핑, `@Valid` 입력 검증, `Model`/리다이렉트로 화면 제어. (`AuthController`, `QuestionController`)
- **Service**: 핵심 로직과 `@Transactional` 원자성, **작성자 권한 검증**, 투표 집계. (`UserService`, `QuestionService`, `VoteService`, `CustomUserDetailsService`)
- **Repository**: `JdbcTemplate` 기반 **직접 SQL** 실행 및 `RowMapper` 매핑. (`UserRepository`, `QuestionRepository`, `VoteRepository`)
- **Domain / DTO**: 테이블 대응 POJO(`User`/`Question`/`Vote`)와 화면·입력용 객체(`SignupForm`/`QuestionForm`/`QuestionResult`) 분리.

```
com.balance.balancegame
├── BalanceGameApplication.java   # 진입점
├── config/SecurityConfig.java    # Security 설정(인가 규칙·폼 로그인·BCrypt)
├── controller/                   # AuthController, QuestionController
├── service/                      # User/Question/Vote/CustomUserDetails Service
├── repository/                   # User/Question/Vote Repository (JdbcTemplate)
├── domain/                       # User, Question, Vote (POJO)
└── dto/                          # SignupForm, QuestionForm, QuestionResult
```

### 2-2. 주요 URL 및 권한 라우팅 경로

| URL | 메서드 | 접근 권한 | 설명 |
|-----|--------|-----------|------|
| `/`, `/questions` | GET | 누구나 | 질문 목록(검색·정렬) |
| `/questions/{id}` | GET | 누구나 | 질문 상세·결과 (투표 후 퍼센트 공개) |
| `/login`, `/signup` | GET/POST | 누구나 | 로그인 / 회원가입 |
| `/css/**` | GET | 누구나 | 정적 리소스 |
| `/questions/new` | GET | **인증 필요** | 질문 등록 폼 |
| `/questions` | POST | **인증 필요** | 질문 등록(Create) |
| `/questions/{id}/vote` | POST | **인증 필요** | 투표 |
| `/questions/{id}/edit` | GET/POST | **인증 + 작성자 본인** | 질문 수정(Update) |
| `/questions/{id}/delete` | POST | **인증 + 작성자 본인** | 질문 삭제(Delete) |
| `/mypage` | GET | **인증 필요** | 내 질문 / 내 투표 |

> 작성자 본인 확인은 Service 계층(`QuestionService.getOwnedQuestion`)에서 `질문.userId == 로그인 사용자.id`를 검증하여, 타인의 질문 수정/삭제를 차단합니다.

---

## 3. Spring Security 인증/인가 설정

`SecurityConfig`의 `SecurityFilterChain` 빈으로 구성했습니다.

- **폼 로그인**: 커스텀 로그인 페이지(`/login`), 로그인 성공 시 `/questions`로 이동, 실패 시 `?error`.
- **로그아웃**: POST `/logout`, 성공 시 `/questions`로 이동.
- **세션 관리**: Spring Security 기본 세션 방식(`JSESSIONID` 쿠키 기반). 로그인 시 세션 고정 보호(session fixation)로 세션 ID 재발급.
- **비밀번호 암호화**: `BCryptPasswordEncoder` 빈으로 가입 시 단방향 해싱 저장.
- **DB 기반 인증**: `CustomUserDetailsService`가 `users` 테이블에서 사용자를 조회하여 인증 처리, `role` 컬럼을 권한(authority)으로 부여.
- **인가 규칙**: 목록/상세/회원가입/로그인/정적 리소스는 `permitAll`, 등록·투표·수정·삭제·마이페이지는 `authenticated`.

```java
http
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/questions/new", "/questions/*/edit", "/mypage").authenticated()
      .requestMatchers(HttpMethod.POST, "/questions", "/questions/*/vote",
              "/questions/*/edit", "/questions/*/delete").authenticated()
      .requestMatchers("/", "/questions", "/questions/*",
              "/signup", "/login", "/css/**", "/js/**").permitAll()
      .anyRequest().authenticated())
  .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/questions", true).permitAll())
  .logout(l -> l.logoutSuccessUrl("/questions").permitAll());
```

> **권한(Role) 체계**: `users.role`에 `ROLE_USER` / `ROLE_ADMIN`을 저장하고 `CustomUserDetailsService`가 이를 권한으로 부여하도록 배선되어 있습니다. 현재 가입 사용자는 모두 `ROLE_USER`이며, 인가는 "인증 여부 + 작성자 본인 여부" 기준으로 동작합니다. (`ROLE_ADMIN` 전용 관리 화면은 확장 지점으로 예약)

화면 측에서는 `thymeleaf-extras-springsecurity6`의 `sec:authorize`로 로그인 상태에 따라 내비게이션(로그인/로그아웃/마이페이지)을 분기합니다.

---

## 4. 데이터베이스 및 SQL 활용

### 4-1. 사용 테이블 (MariaDB)

앱 기동 시 `schema.sql`이 자동 실행되어 테이블을 생성합니다(`CREATE TABLE IF NOT EXISTS`).

```sql
users     (id PK, username UNIQUE, password, role, created_at)
questions (id PK, title, option_a, option_b, user_id → users.id, created_at)
votes     (id PK, question_id → questions.id, user_id → users.id, choice('A'/'B'), created_at,
           UNIQUE(question_id, user_id))   -- 1인 1표 보장
```

`data.sql`로 데모 데이터(질문 20개 + 투표)를 `INSERT IGNORE`로 주입하여 재기동 시에도 중복이 쌓이지 않습니다.

### 4-2. 주요 SQL (계층을 거친 CRUD)

```sql
-- [Create] 질문 등록 (KeyHolder로 생성 PK 반환)
INSERT INTO questions (title, option_a, option_b, user_id) VALUES (?, ?, ?, ?);

-- [Read] 목록: 제목 검색(LIKE) + 인기순(투표 수) / 최신순 정렬
SELECT q.* FROM questions q
WHERE q.title LIKE ?
ORDER BY (SELECT COUNT(*) FROM votes v WHERE v.question_id = q.id) DESC, q.id DESC;

-- [Read] 결과 집계: 전체 질문의 선택지별 득표를 한 번에 (N+1 방지)
SELECT question_id, choice, COUNT(*) AS cnt FROM votes GROUP BY question_id, choice;

-- [Update] 질문 수정 (작성자 검증 후)
UPDATE questions SET title = ?, option_a = ?, option_b = ? WHERE id = ?;

-- [Delete] 질문 삭제 (FK 때문에 자식 투표 먼저 삭제)
DELETE FROM votes     WHERE question_id = ?;
DELETE FROM questions WHERE id = ?;

-- [투표] 1인 1표: UNIQUE(question_id, user_id) 제약 + 사전 조회 검증
INSERT INTO votes (question_id, user_id, choice) VALUES (?, ?, ?);
```

모든 쿼리는 Repository 계층에서 `JdbcTemplate`으로 실행되며, 결과는 `RowMapper`로 도메인 객체에 매핑됩니다.

---

## 5. 트러블슈팅 (문제 해결 기록)

### ① Spring Security 적용 후 CSS(정적 리소스) 차단
- **문제**: Security 활성화 후 비로그인 상태에서 `/css/style.css`가 차단되어 화면 스타일이 깨짐.
- **해결**: 인가 규칙에 `requestMatchers("/css/**").permitAll()`을 추가해 정적 리소스를 인증 없이 허용.

### ② 투표 결과 집계 N+1 쿼리
- **문제**: 목록에서 질문마다 `A 득표 / B 득표 / 내 투표`를 개별 조회 → 질문 N개에 쿼리가 `3N+1`개 발생.
- **해결**: `GROUP BY question_id, choice`로 **전체 득표를 1쿼리**, 내 투표를 1쿼리로 모아 Map으로 조립. 질문 수와 무관하게 **최대 3쿼리**로 감소.

### ③ 질문 삭제 시 외래키(FK) 제약 위반
- **문제**: `questions`를 삭제하려 하자 `votes`가 참조 중이어서 FK 제약 위반 오류.
- **해결**: Service의 삭제 로직을 `@Transactional`로 묶고, **자식(votes) 먼저 삭제 → 질문 삭제** 순서로 처리.

### ④ DB 접속정보 하드코딩 (배포 보안)
- **문제**: `application.properties`에 DB 비밀번호가 평문으로 노출 → 저장소 공개 시 위험.
- **해결**: `${DB_PASSWORD:기본값}` 형태로 **환경변수 분리**. 운영(GCP)에서는 systemd `EnvironmentFile`(권한 600)로 주입하여 코드/저장소에 비밀번호를 남기지 않음.

### ⑤ GCP 배포 후 애플리케이션 상시 가동 / HTTPS
- **문제**: SSH 세션 종료·프로세스 종료 시 앱이 함께 꺼지고, VM에 SSL 인증서 설정이 번거로움.
- **해결**:
  - 앱을 **systemd 서비스**로 등록(`Restart=always` + `enable`) → 크래시·재부팅에도 자동 복구·자동 기동.
  - **cloudflared 터널**을 systemd 서비스로 상시 실행 → Cloudflare 엣지가 **HTTPS를 자동 처리**(VM에 인증서 불필요), 8080 인바운드도 열 필요 없음(아웃바운드 터널).
  - 앱은 `127.0.0.1:8080`에만 바인딩하고 터널만 접근하도록 하여 외부 직접 노출을 차단.

---

## ▶️ 실행 방법 (로컬)

```bash
# 1) DB 준비 (MariaDB)
CREATE DATABASE balancegame CHARACTER SET utf8mb4;

# 2) 접속 정보 (환경변수 또는 application.properties 기본값)
#    DB_URL / DB_USERNAME / DB_PASSWORD

# 3) 실행
./gradlew bootRun        # http://localhost:8080
```

첫 기동 시 `schema.sql`로 테이블이, `data.sql`로 데모 질문 20개 + 투표가 자동 생성됩니다.
배포 절차(GCP VM + systemd + cloudflared)는 [`deploy/DEPLOY.md`](deploy/DEPLOY.md)를 참고하세요.
