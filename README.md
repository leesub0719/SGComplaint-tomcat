# SGComplaint Tomcat

서경운수 민원 관리 기능을 **Spring MVC + Thymeleaf + JavaScript + REST API + MyBatis**로 구성하고,
Spring Boot 내장 서버가 아닌 **외부 Apache Tomcat 11**에 WAR로 배포·운영하는 프로젝트입니다.

## 프로젝트 목적

- 기존 Java/Spring MVC 소스 분석과 기능 수정 능력 증명
- Thymeleaf 최초 화면 렌더링과 분리된 JavaScript의 REST API 호출 경험 증명
- 외부 Tomcat 설치, WAR 배포, 로그 분석, 재배포와 롤백 경험 증명
- MySQL/MyBatis 데이터 처리와 Spring Security 인증·CSRF 적용
- CI에서 테스트한 WAR만 운영 배포 대상으로 사용하는 절차 수립

## 요청 흐름

```text
브라우저
  ├─ GET /complaints ──> @Controller ──> Thymeleaf HTML 최초 렌더링
  └─ fetch('/api/...') ─> @RestController ─> Service ─> MyBatis Mapper ─> MySQL
                              └────────────── JSON 응답 ──────────────┘
```

화면 템플릿은 `src/main/resources/templates`, 브라우저 로직은
`src/main/resources/static/js`에 분리되어 있습니다. 외부 Tomcat에서는 WAR 이름이 기본
Context Path가 되므로 `sgcomplaint.war`는 `/sgcomplaint`로 서비스됩니다.

## 주요 REST API

아래 URL 앞에는 운영 Context Path인 `/sgcomplaint`가 붙습니다.

| 메서드 | 경로 | 용도 | 인증 |
|---|---|---|---|
| GET | `/api/public/complaints` | 공개 민원 목록 조회 | 불필요 |
| GET | `/api/csrf` | 변경 요청용 CSRF 헤더 이름·토큰 조회 | 불필요 |
| GET | `/api/mypage/inquiries` | 본인 민원 목록 조회 | 회원 |
| PUT | `/api/mypage/inquiries/{complaintNo}` | 본인 민원 수정 | 회원 + CSRF |
| DELETE | `/api/mypage/inquiries/{complaintNo}` | 본인 민원 삭제 | 회원 + CSRF |
| GET | `/api/mypage/profile` | 내 정보 조회 | 회원 |
| GET | `/api/admin/dashboard` | 관리자 통계·최근 민원 | 관리자 |
| POST | `/api/admin/complaints/{complaintNo}` | 답변과 상태 변경 | 관리자 + CSRF |
| POST | `/api/phone-verifications/request` | 휴대전화 인증번호 요청 | 정책에 따름 |

## 요구 환경

- Java 17
- Apache Tomcat 11.x
- MySQL 8.x
- Gradle Wrapper(저장소 포함)

Tomcat 10 이하에는 배포하지 않습니다. 이 프로젝트는 Spring Boot 4/Jakarta Servlet API를
사용하므로 Tomcat 11을 기준으로 검증합니다.

## 빌드와 로컬 검증

PowerShell:

```powershell
$env:DB_PASSWORD = '로컬-DB-비밀번호'
.\gradlew.bat clean test bootWar
```

결과 파일:

```text
build/libs/sgcomplaint.war
```

WAR 내부에는 애플리케이션 클래스와 Thymeleaf 템플릿이 `WEB-INF/classes`에,
외부 컨테이너가 제공할 Tomcat 라이브러리는 `WEB-INF/lib-provided`에 들어갑니다.

## 외부 Tomcat 빠른 배포

1. Tomcat 11 전용 서비스 계정을 만들고 JDK 17을 연결합니다.
2. `deploy/tomcat/setenv.*.example`을 참고해 환경변수를 설정합니다.
3. 필요 시 `deploy/tomcat/sgcomplaint.xml.example`을
   `conf/Catalina/localhost/sgcomplaint.xml`로 복사합니다.
4. CI 산출물 또는 로컬에서 검증된 `sgcomplaint.war`를 `webapps`에 배포합니다.
5. `/sgcomplaint/actuator/health`가 `UP`인지 확인합니다.

Windows 서비스 배포 예시:

```powershell
.\deploy\windows\deploy-tomcat.ps1 `
  -TomcatHome 'C:\apache-tomcat-11' `
  -TomcatServiceName 'Tomcat11'
```

Linux systemd 배포 예시:

```bash
TOMCAT_HOME=/opt/tomcat TOMCAT_SERVICE=tomcat11 \
  ./deploy/linux/deploy-tomcat.sh
```

세부 절차는 [외부 Tomcat 설치와 설정](docs/ops/tomcat-installation.md),
[배포·롤백 실행서](docs/ops/deployment-runbook.md),
[장애 대응 실행서](docs/ops/troubleshooting.md)를 참고하세요.

## 배포 후 확인

```bash
BASE_URL=http://127.0.0.1:8080/sgcomplaint

curl -i "$BASE_URL/"
curl --fail "$BASE_URL/actuator/health"
curl --fail "$BASE_URL/api/public/complaints?category=ALL&page=0"
curl --fail -c cookies.txt "$BASE_URL/api/csrf"
```

기대 결과는 순서대로 HTML `200`, health JSON의 `UP`, 민원 목록 JSON,
`headerName`과 `token`을 포함한 CSRF JSON입니다. 변경 API는 로그인 세션 쿠키와 CSRF
헤더를 함께 보내야 합니다.

## CI

`.github/workflows/war-ci.yml`은 다음을 수행합니다.

1. Java 17에서 전체 테스트 실행
2. `bootWar` 생성
3. WAR의 애플리케이션 클래스·템플릿·provided Tomcat 구조 확인
4. 검증된 `sgcomplaint.war`를 14일간 GitHub Actions artifact로 보관

CI 성공은 운영 배포 완료가 아닙니다. 배포 후 health, 주요 HTML 화면, 공개 API와 인증 기능을
별도로 확인해야 합니다.

## 운영 장애 시나리오

| 현상 | 우선 확인 | 대표 조치 |
|---|---|---|
| 배포 직후 404 | WAR 이름, Context Path, `localhost` 로그 | `/sgcomplaint` 기준 URL 확인, 전개 실패 원인 제거 |
| 시작 중 500 | 애플리케이션 로그의 첫 `Caused by` | DB 환경변수·MyBatis XML·프로필 수정 |
| 로그인/POST 403 | CSRF 토큰, 세션 쿠키, 프록시 HTTPS 헤더 | Context Path 기반 URL과 CSRF 헤더 수정 |
| DB 연결 실패 | `Communications link failure`, Hikari 로그 | DB 방화벽·계정·URL 확인, 비밀번호 교체 |
| 재배포 후 구버전 동작 | WAR 시각, 전개 디렉터리, 프로세스 | Tomcat 정지 후 구 전개 디렉터리 제거·재배포 |
| 응답 지연/오류 증가 | access log, 스레드·DB 풀, p95 | 병목 구간 분리 후 풀·쿼리·타임아웃 조정 |

운영 로그와 원인 분석 순서는 [장애 대응 실행서](docs/ops/troubleshooting.md)에 정리되어 있습니다.

## 비밀정보 원칙

- 실제 DB/SMS 키는 Git에 저장하지 않습니다.
- `.env.example`과 `setenv.*.example`에는 변수 이름과 가짜 값만 둡니다.
- Linux는 실제 `setenv.sh`를 Tomcat 계정만 읽게 하고, Windows 서비스는 시스템 환경변수나
  Tomcat 서비스 관리자의 Java 옵션/환경을 사용합니다.
- 업로드 파일과 데이터베이스 백업은 WAR 외부의 영속 경로에 저장합니다.
