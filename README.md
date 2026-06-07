# ⚖️ 밸런스 게임 (Balance Game)

한경 부트캠프 **4차 8시간 해커톤** 프로젝트. 사용자가 밸런스 게임 질문을 등록하고, 다른 사용자들이 투표하면 결과가 양쪽 퍼센트 바로 표시되는 웹 서비스.

---

## 📋 해커톤 개요

| 항목 | 내용 |
|------|------|
| 형식 | 8시간 해커톤 |
| 결과물 | Spring Boot + Thymeleaf + DB + Spring Security 기반 CRUD 웹서비스 |
| 배포 | GCP VM 인스턴스 + Cloudflare SSL |
| **기술 제약** | **한경 부트캠프 chapter 5~6에서 배운 내용만 사용** (벗어나는 기술 금지) |

### ⚠️ 기술 범위 제약 (중요)
부트캠프 ch5~6 범위 **안에서만** 구현한다. 아직 안 배운 기술은 쓰지 않는다.

- 표준 계층 아키텍처: `Controller → Service → Repository → Domain/DTO`
- **DB 접근은 JdbcTemplate + RowMapper로 직접 SQL** — **JPA(@Entity, JpaRepository 등) 사용 금지**
- Spring Security: 폼 로그인/로그아웃, BCrypt 암호화, `ROLE_USER`/`ROLE_ADMIN` 인가, `SecurityFilterChain`
- Thymeleaf 템플릿
- `@Transactional`(원자성), AOP(프록시)
- Java 17, Gradle, Spring Initializr

---

## 🎮 기능 명세

- **질문 등록**: 로그인한 사용자는 누구나 가능
- **투표**: 한 질문당 한 사용자는 **한 번만** 투표 (`votes` 테이블의 `UNIQUE(question_id, user_id)`)
- **투표 결과**: **바(bar) 형식**으로 양쪽 선택지의 퍼센트(%)를 함께 표시
- **댓글**: 없음
- 비로그인 사용자도 질문 목록과 결과는 볼 수 있으나, 등록/투표는 로그인 필요

---

## 🛠 기술 스택

| 구분 | 사용 기술 |
|------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.6 |
| Build | Gradle (Wrapper 포함) |
| View | Thymeleaf + thymeleaf-extras-springsecurity6 |
| DB 접근 | Spring JDBC (`JdbcTemplate` + `RowMapper`) — **JPA 미사용** |
| DB | MariaDB (드라이버: `mariadb-java-client`) |
| 보안 | Spring Security (폼 로그인, BCrypt) |
| 검증 | spring-boot-starter-validation |

---

## 🗂 패키지 구조

```
com.balance.balancegame
├── BalanceGameApplication.java
├── config
│   └── SecurityConfig.java          # 폼 로그인/로그아웃, BCrypt, 권한 규칙
├── controller
│   ├── AuthController.java          # /login, /signup
│   └── QuestionController.java      # 목록/등록/상세/투표
├── service
│   ├── UserService.java             # 회원가입(BCrypt)
│   ├── CustomUserDetailsService.java# DB 기반 로그인 인증
│   ├── QuestionService.java         # 질문 등록 + 결과 집계
│   └── VoteService.java             # 투표(1인 1표 검증)
├── repository                       # 전부 JdbcTemplate + RowMapper
│   ├── UserRepository.java
│   ├── QuestionRepository.java      # save 시 KeyHolder로 PK 반환
│   └── VoteRepository.java
├── domain                           # User, Question, Vote (POJO)
└── dto                              # SignupForm, QuestionForm, QuestionResult(집계/퍼센트)
```

템플릿: `src/main/resources/templates/` (`login`, `signup`, `questions/list`, `questions/form`, `questions/detail`)
스타일: `src/main/resources/static/css/style.css` (결과 바 포함)
스키마: `src/main/resources/schema.sql` (앱 시작 시 자동 실행)

---

## 🧱 DB 스키마

```sql
users     (id, username UNIQUE, password, role, created_at)
questions (id, title, option_a, option_b, user_id → users.id, created_at)
votes     (id, question_id → questions.id, user_id → users.id, choice('A'/'B'), created_at,
           UNIQUE(question_id, user_id))   -- 1인 1표 보장
```

`application.properties`의 `spring.sql.init.mode=always` 로 `schema.sql`이 매 기동 시 실행됨(`CREATE TABLE IF NOT EXISTS`).

---

## ▶️ 실행 방법

### 1. DB 준비
```sql
CREATE DATABASE balancegame CHARACTER SET utf8mb4;
```

### 2. 접속 정보 확인
`src/main/resources/application.properties` 에서 본인 환경에 맞게 수정:
```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/balancegame
spring.datasource.username=root
spring.datasource.password=1234   # ← 본인 비밀번호로
```

### 3. 실행
```bash
./gradlew bootRun
```
→ 브라우저에서 `http://localhost:8080`

### 데모 데이터
첫 기동 시 `data.sql`이 자동 실행되어 **질문 12개 + 데모 투표**가 채워집니다(`INSERT IGNORE`라 재기동해도 중복 없음).
데모 계정으로 로그인해 투표를 테스트할 수 있어요: **`demo1` ~ `demo8` / 비밀번호 `1234`**.

---

## ⚙️ 개발 환경 메모 (세션 간 인수인계)

- **JDK 버전**: build.gradle 툴체인은 **Java 17**. 노트북(JDK 17)에서는 그대로 빌드/실행됨.
  - 데스크톱에는 JDK 21만 있어, 거기서 빌드하려면 툴체인을 임시 21로 바꿔 돌린 뒤 17로 되돌렸음.
- **포트**: 기본 8080. (검증했던 데스크톱은 8080을 Oracle TNS Listener가 점유 중이라 `--args=--server.port=8081`로 띄웠음.)
- **DB 호환**: 검증 환경의 3306에는 MySQL 9.6이 떠 있었는데, `mariadb-java-client` 드라이버로 정상 접속됨.

---

## ✅ 진행 상황

**완료 (엔드투엔드 검증됨)**
- 회원가입 → 로그인 → 질문 등록 → 투표 → 결과 바 표시 전체 동작 확인
- schema.sql 자동 테이블 생성, 보호 URL 로그인 리다이렉트 정상

**남은 작업**
- [ ] `ROLE_ADMIN` 계정/관리자 화면 (현재 미사용)
- [ ] 화면 디테일 보완
- [ ] GCP VM 배포 + Cloudflare SSL
