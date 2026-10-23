# WAR 배포·검증·롤백 실행서

## 배포 전

1. 변경 내용과 DB 마이그레이션 유무를 확인합니다.
2. GitHub Actions `WAR CI`가 성공했는지 확인합니다.
3. 운영 DB와 업로드 경로를 백업하고 복구 가능 여부를 확인합니다.
4. 현재 배포 WAR의 체크섬과 배포 시간을 기록합니다.
5. 점검 시간을 공지하고 신규 요청 유입을 통제합니다.

## 빌드

```bash
./gradlew --no-daemon clean test bootWar
sha256sum build/libs/sgcomplaint.war
```

Windows에서는 `gradlew.bat`와 `Get-FileHash`를 사용합니다. DB 비밀번호는 테스트에 필요한
환경에서만 주입하며 출력하거나 저장소 파일에 기록하지 않습니다.

## 안전한 배포 순서

1. Tomcat을 정상 종료합니다.
2. 기존 `sgcomplaint.war`와 `webapps/sgcomplaint`를 타임스탬프 경로에 백업합니다.
3. 기존 전개 디렉터리를 제거합니다.
4. 새 WAR를 임시 이름으로 복사한 뒤 `sgcomplaint.war`로 원자적으로 이름을 바꿉니다.
5. Tomcat을 시작합니다.
6. 로그에서 배포 성공과 Spring 시작 완료를 확인합니다.
7. health와 주요 화면/API를 확인합니다.

저장소의 `deploy/windows/deploy-tomcat.ps1`과 `deploy/linux/deploy-tomcat.sh`가 이 순서를
자동화합니다. 스크립트는 health 실패 시 자동 롤백하지 않습니다. 원인을 보존하고 운영자가
로그를 확인한 후 아래 절차로 명시적으로 롤백합니다.

## 스모크 테스트

```bash
BASE_URL=http://127.0.0.1:8080/sgcomplaint
curl --fail "$BASE_URL/actuator/health"
curl --fail --output /dev/null "$BASE_URL/"
curl --fail "$BASE_URL/api/public/complaints?category=ALL&page=0"
curl --fail "$BASE_URL/api/csrf"
```

브라우저에서는 로그인, 본인 민원 조회·수정·삭제, 관리자 답변과 파일 다운로드를 확인합니다.
변경 요청은 CSRF와 권한이 적용되어야 하므로 미인증 요청이 거절되는 것도 정상 동작입니다.

## 롤백 기준

- 애플리케이션이 제한 시간 내 시작되지 않음
- health가 연속 실패하거나 주요 화면/API가 5xx를 반환함
- 로그인·권한·CSRF가 정상 동작하지 않음
- 신규 DB 마이그레이션과 이전 애플리케이션 버전이 호환되지 않음

## 롤백 순서

1. Tomcat을 정지합니다.
2. 실패한 WAR와 전개 디렉터리를 장애 조사용 경로로 이동합니다.
3. 백업한 이전 WAR를 `webapps/sgcomplaint.war`로 복원합니다.
4. DB 마이그레이션이 있었다면 사전에 검증한 복구 절차를 적용합니다. 임의로 Flyway 이력을 삭제하지 않습니다.
5. Tomcat을 시작하고 동일한 스모크 테스트를 수행합니다.
6. 장애 시작·탐지·조치·복구 시각과 원인을 기록합니다.

## 배포 증적

- Git commit SHA와 Actions 실행 URL
- WAR SHA-256
- 배포자와 배포 시각
- health 및 주요 API 결과
- Tomcat 시작 로그의 애플리케이션 버전
- 롤백 여부와 후속 조치
