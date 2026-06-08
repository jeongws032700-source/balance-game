# 🚀 GCP VM 배포 가이드 (systemd + Cloudflare Tunnel)

Spring Boot 앱을 GCP VM에 **systemd 서비스**로 띄워 안 죽게 하고, **cloudflared(Cloudflare Tunnel)** 로 HTTPS 도메인을 붙이는 절차.

```
인터넷 ──HTTPS──▶ Cloudflare 엣지 ──터널(아웃바운드)──▶ [GCP VM] cloudflared ──▶ localhost:8080 (Spring Boot)
                                                                              └─ localhost:3306 (MariaDB)
```

- HTTPS/TLS는 **Cloudflare 엣지가 자동 처리** → VM에 인증서 설치 불필요
- 터널은 **아웃바운드** 연결이라 GCP 방화벽에서 8080/443 인바운드를 열 필요 없음 (보안 ↑)
- 전제: ① GCP VM(Debian/Ubuntu) 한 대, ② **Cloudflare에 등록된 도메인** 하나

> 메모리: e2-micro(1GB)는 MariaDB+JVM이 빠듯합니다. 가능하면 e2-small(2GB) 권장, 아니면 유닛의 `-Xmx`를 256~384m로 낮추세요.

---

## 1. jar 빌드

Gradle이 정상 동작하는 환경(본인 노트북/데스크톱 또는 VM)에서:

```bash
./gradlew clean bootJar
ls build/libs/        # → balance-game-0.0.1-SNAPSHOT.jar
```

VM으로 복사 (로컬에서 빌드한 경우):

```bash
gcloud compute scp build/libs/balance-game-0.0.1-SNAPSHOT.jar  <인스턴스>:~  --zone <존>
# 또는 scp -i <키> build/libs/...jar user@<VM_IP>:~
```

---

## 2. VM 기본 셋업 (Java 17 + MariaDB)

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless mariadb-server

# DB / 전용 계정 생성 (STRONG_PW 는 실제 비밀번호로)
sudo mariadb <<'SQL'
CREATE DATABASE IF NOT EXISTS balancegame CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'balance'@'localhost' IDENTIFIED BY 'STRONG_PW';
GRANT ALL PRIVILEGES ON balancegame.* TO 'balance'@'localhost';
FLUSH PRIVILEGES;
SQL
```

> 테이블은 앱 기동 시 `schema.sql`이, 데모 데이터는 `data.sql`이 자동 생성/주입합니다(`INSERT IGNORE`라 재기동해도 중복 없음).

---

## 3. 앱 배치 + 환경변수 + systemd

```bash
# 전용 사용자 & 디렉터리
sudo useradd -r -s /usr/sbin/nologin balance
sudo mkdir -p /opt/balance-game
sudo cp ~/balance-game-0.0.1-SNAPSHOT.jar /opt/balance-game/balance-game.jar
sudo chown -R balance:balance /opt/balance-game

# 환경변수 파일 (비밀번호 — 권한 잠금)
sudo mkdir -p /etc/balance-game
sudo cp deploy/balance-game.env.example /etc/balance-game/balance-game.env
sudo nano /etc/balance-game/balance-game.env      # DB_PASSWORD 등 채우기
sudo chown root:root /etc/balance-game/balance-game.env
sudo chmod 600        /etc/balance-game/balance-game.env

# systemd 서비스 등록
sudo cp deploy/balance-game.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now balance-game

# 확인
systemctl status balance-game
journalctl -u balance-game -f          # "Started BalanceGameApplication" 나오면 성공
curl -i http://localhost:8080/questions
```

`Restart=always`라 크래시/OOM 시 자동 재시작, `enable` 했으니 VM 재부팅에도 자동 기동됩니다.

---

## 4. cloudflared (Cloudflare Tunnel)

### 4-1. 설치 (Debian/Ubuntu)

```bash
sudo mkdir -p --mode=0755 /usr/share/keyrings
curl -fsSL https://pkg.cloudflare.com/cloudflare-main.gpg | sudo tee /usr/share/keyrings/cloudflare-main.gpg >/dev/null
echo "deb [signed-by=/usr/share/keyrings/cloudflare-main.gpg] https://pkg.cloudflare.com/cloudflared any main" \
  | sudo tee /etc/apt/sources.list.d/cloudflared.list
sudo apt update && sudo apt install -y cloudflared
```

### 4-2. 로그인 → 터널 생성

```bash
cloudflared tunnel login           # 출력된 URL을 브라우저로 열어 도메인 선택/인증 (~/.cloudflared/cert.pem 생성)
cloudflared tunnel create balance-game
# → Tunnel UUID 와 ~/.cloudflared/<UUID>.json 생성됨
```

### 4-3. 설정 배치

```bash
sudo mkdir -p /etc/cloudflared
sudo cp ~/.cloudflared/<UUID>.json /etc/cloudflared/
sudo cp deploy/cloudflared-config.example.yml /etc/cloudflared/config.yml
sudo nano /etc/cloudflared/config.yml      # <TUNNEL-UUID> 와 hostname(본인 도메인) 채우기
```

### 4-4. DNS 라우팅 + 서비스 등록

```bash
# 도메인 → 터널 CNAME 을 Cloudflare DNS에 자동 생성
cloudflared tunnel route dns balance-game balance.example.com

# systemd 서비스로 설치 후 상시 실행
sudo cloudflared --config /etc/cloudflared/config.yml service install
sudo systemctl enable --now cloudflared
systemctl status cloudflared
```

### 4-5. 확인

브라우저에서 **https://balance.example.com** 접속 → 질문 목록이 보이면 끝.

---

## 5. 운영 명령

| 작업 | 명령 |
|------|------|
| 앱 로그 보기 | `journalctl -u balance-game -f` |
| 터널 로그 보기 | `journalctl -u cloudflared -f` |
| 앱 재시작 | `sudo systemctl restart balance-game` |
| 상태 확인 | `systemctl status balance-game cloudflared` |

### 새 버전 배포 (재배포)

```bash
# 새 jar 복사 후
sudo cp ~/balance-game-0.0.1-SNAPSHOT.jar /opt/balance-game/balance-game.jar
sudo chown balance:balance /opt/balance-game/balance-game.jar
sudo systemctl restart balance-game
```

---

## 트러블슈팅

- **앱이 안 뜸**: `journalctl -u balance-game -e` → 대개 DB 접속 실패. `/etc/balance-game/balance-game.env`의 `DB_PASSWORD`/계정 권한 확인.
- **502/터널 연결됨인데 앱 응답 없음**: 앱이 `127.0.0.1:8080`에 떠 있는지(`curl localhost:8080/questions`), config.yml의 `service`가 `http://localhost:8080`인지 확인.
- **도메인이 안 열림**: `cloudflared tunnel route dns ...`가 성공했는지, Cloudflare DNS에 CNAME(프록시 주황색 구름)이 생겼는지 확인.
- **메모리 부족(OOM)으로 재시작 반복**: 유닛의 `-Xmx`를 낮추거나 VM 메모리 상향.
