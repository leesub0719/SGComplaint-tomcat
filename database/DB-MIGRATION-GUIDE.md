# DB 변경 이력 관리 (Flyway) — 2026-09-07

## 핵심

애플리케이션의 조회·등록·수정·삭제 쿼리는 계속 MyBatis입니다.
Flyway는 테이블/컬럼/인덱스 등 DB 구조 변경 SQL의 버전과 실행 이력만 관리합니다.

- SQL 위치: src/main/resources/db/migration/
- 현재 버전: V1__current_schema.sql (현재 프로젝트의 14개 테이블 기준)
- 이력 테이블: flyway_schema_history
- 이후 변경 파일: V2__설명.sql, V3__설명.sql 순서
- 자동 실행: DB_MIGRATION_ENABLED=true일 때 애플리케이션 시작 전에 미적용 SQL만 실행
- 기본값은 false입니다. 최초 등록 없이 WAR만 바꿔도 기존 앱 실행에는 영향을 주지 않도록 했습니다.
- 앱 시작 시 자동 baseline, clean, 자동 repair는 사용하지 않습니다.
- 기존 SQL 파일을 한꺼번에 재실행하지 않습니다. 비밀번호 초기화 등 과거의 일회성 데이터 SQL도 가져오지 않습니다.

과거의 수동 SQL 실행 시각은 복원할 수 없습니다.
기존 DB는 현재 구조를 확인한 날짜의 V1 BASELINE 한 건으로 기록되고, 이후 새 버전부터 실행 이력이 쌓입니다.
빈 DB에서는 V1 SQL이 실행되고 SQL 유형의 이력이 기록됩니다.

## 1. 기존 개발 DB에서 먼저 확인

프로젝트 루트에서 PowerShell을 실행합니다.
DB 비밀번호는 명령문에 직접 쓰지 않고 입력 창에서 받습니다.

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/sgcomplaint?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
$env:DB_NAME='sgcomplaint'
$env:DB_USERNAME='root'
$credential=Get-Credential -UserName root -Message '개발 DB 비밀번호 입력'
$env:DB_PASSWORD=$credential.GetNetworkCredential().Password
.\gradlew.bat dbCheck
```

V1_CHECK_OK일 때만 다음 명령으로 진행합니다.
baseline 명령도 자체적으로 동일한 구조 검사를 다시 수행합니다.

```powershell
.\gradlew.bat dbBaseline
.\gradlew.bat dbInfo
.\gradlew.bat dbValidate
Remove-Item Env:DB_PASSWORD
```

- DB_URL은 확인할 대상 DB의 JDBC 주소입니다. 위 useSSL=false 설정은 로컬 예시이며 원격 운영 접속에 그대로 사용하지 마세요.
- ROOT가 아니라 다른 계정을 쓰는 환경은 DB_USERNAME과 입력 창 사용자 이름을 맞춰 주세요.
- 환경변수는 이 PowerShell 창에만 적용됩니다. Eclipse 실행 설정에는 DB_MIGRATION_ENABLED=true를 별도로 추가해야 합니다.
- 개발/운영 각 DB에서 최초 기준 등록을 각각 해야 합니다.
- dbBaseline은 기존 업무 테이블과 데이터를 변경하지 않고 Flyway 이력 테이블과 V1 기준 행만 만듭니다.
- 비어 있는 DB에는 baseline하지 마세요. dbCheck의 EMPTY_DB 확인 후 dbMigrate로 V1 테이블을 만듭니다.
- DB 자체의 생성, 계정 생성 및 권한 부여는 이 도구가 하지 않습니다.

## 2. 기존 운영 DB 최초 적용

전제: 외부 Tomcat 10.1 서비스는 tomcat10이며, 운영 환경변수와 업로드 경로가
`deploy/tomcat/setenv.sh.example` 기준으로 설정되어 있습니다.
DB 도구는 운영 서버에 임의 스크립트를 복사하지 않고 검증된 릴리스 작업공간의 Gradle task로 실행합니다.

1. DB와 기존 `webapps/sgcomplaint.war`를 백업합니다.
2. Tomcat 서비스를 중지합니다.

```bash
sudo systemctl stop tomcat10
```

3. 검증된 릴리스 작업공간에서 운영 DB 환경변수를 주입합니다. 비밀번호는 명령 이력이나 파일에 남기지 않습니다.
4. 아래 명령으로 DB 구조를 읽기 전용 확인합니다.

```bash
./gradlew --no-daemon dbCheck
```

5. V1_CHECK_OK 확인 후 최초 기준을 등록합니다.

```bash
./gradlew --no-daemon dbBaseline
./gradlew --no-daemon dbInfo
./gradlew --no-daemon dbValidate
```

6. Tomcat 운영 환경 설정에 아래 두 값을 추가하거나 같은 항목의 값을 수정합니다.
   DB_URL/DB_USERNAME/DB_PASSWORD와 업로드·SMS 설정은 그대로 유지합니다.

```bash
sudo nano /opt/tomcat/bin/setenv.sh
```

```properties
DB_NAME=sgcomplaint
DB_MIGRATION_ENABLED=true
```

7. 테스트를 통과한 WAR를 배포 스크립트로 반영하고 상태를 확인합니다.

```bash
TOMCAT_HOME=/opt/tomcat TOMCAT_SERVICE=tomcat10 \
  ./deploy/linux/deploy-tomcat.sh
sudo systemctl status tomcat10 --no-pager -l
sudo journalctl -u tomcat10 -n 100 --no-pager
```

한 번 기준 등록과 활성화를 끝내면, 다음 배포부터는 새 WAR 안의 미적용 버전 SQL이 시작 시 자동 실행됩니다.
baseline 명령을 매번 실행하지 않습니다.

## 3. V1_CHECK_OK가 나오지 않을 때

구조 검사는 현재 V1의 다음 요소를 확인합니다.
- 14개 테이블, 121개 컬럼의 타입·NULL 여부·기본값·AUTO_INCREMENT·문자셋
- 38개 PK/UNIQUE/일반 인덱스 정의, 8개 외래키 정의
- 접속한 실제 DB명과 DB_NAME 일치 여부

MySQL 정수 표시 폭과 CURRENT_TIMESTAMP() 표기 차이는 정규화합니다.
인덱스 이름 차이, 추가 일반 인덱스, utf8mb4 내부의 collation 이름 차이는 허용합니다.
추가 UNIQUE/컬럼/테이블, 누락된 인덱스, 다른 기본값 등은 검토가 필요하므로 중단합니다.

MISSING/DIFFERENT 또는 UNEXPECTED/DIFFERENT 결과를 확인하세요.
이때 업무 테이블을 자동 ALTER/DROP하거나 Flyway 이력을 만들지 않습니다.
운영과 개발 구조 차이가 있을 수 있으므로 오류 결과를 전달해 조정 SQL을 검토한 뒤 다시 check합니다.
검사를 우회하려고 baseline-on-migrate=true를 설정하거나 이력 테이블을 수동 생성하지 마세요.

## 4. 변경 이력 조회

DBeaver에서 database/migration-history.sql을 실행하거나 아래 쿼리를 사용합니다.

```sql
SELECT installed_rank, version, description, type, script, checksum,
       installed_by, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

미적용 SQL까지 보려면 dbInfo 또는 서버 스크립트의 info를 사용합니다.

## 5. 다음 DB 변경을 추가하는 규칙

예시 파일명: src/main/resources/db/migration/V2__add_employee_status_index.sql
다음 SQL은 설명용이며 이번 작업에 V2로 추가하지 않았습니다.

```sql
CREATE INDEX idx_employee_status_created
    ON sgtransit_employee (emp_status, created_at);
```

1. 아직 배포하지 않은 다음 버전의 SQL 파일을 새로 만듭니다.
2. 개발 DB에서 적용하고 쿼리/기능을 검증합니다.
3. 운영 DB 수동 백업 후 SQL을 포함한 WAR를 배포합니다.
4. 시작 로그와 이력 테이블의 success를 확인합니다.

이미 적용된 V1/V2 파일은 수정하거나 삭제하지 말고 V3 등 새 파일을 만드세요.
이력 테이블의 checksum/version을 직접 수정하지 마세요.
database/의 기존 개별 SQL과 schema.sql은 과거 참고용입니다. Flyway 도입 후 신규 변경은 db/migration만 사용합니다.

Flyway validate는 저장된 이력과 SQL 파일의 체크섬을 비교합니다.
DB에서 직접 실행한 ALTER를 자동으로 모두 탐지하는 기능은 아닙니다.
또한 기존 DB의 V1 BASELINE 행은 실행한 V1 SQL의 체크섬이 없는 기준 표시입니다.
V1 파일은 신규 DB 재현을 위한 고정 스냅샷으로 보존하세요.

## 6. 실패 시 주의

- 이력 테이블을 DROP하거나 baseline/repair로 실패를 숨기지 마세요.
- MySQL DDL은 일부 작업이 즉시 반영될 수 있어 실패 시 전부 자동 롤백된다고 가정하면 안 됩니다. [MySQL 공식 설명](https://dev.mysql.com/doc/refman/8.4/en/implicit-commit.html)
- 실패 로그와 실제 테이블 상태를 확인하고, 백업 복구 또는 검토한 후속 조치를 정해야 합니다.
- 오래된 JAR로 되돌리는 것만으로 DB 구조가 되돌아가지는 않습니다.
- 자동 DB 실행에는 해당 DB의 DDL 권한이 필요합니다. 권한은 이번 작업에서 변경하지 않았습니다.
- clean/repair는 제공한 CLI/Gradle 명령에서 노출하지 않습니다.

## 검증 범위

기존 기능 테스트와 DB 변경 이력 테스트를 실행합니다.
이력 생성, 재실행 방지, baseline 시 기존 행 보존, SQL 수정 체크섬 오류, clean 차단은 격리된 메모리 H2에서 확인합니다.
기준 구조 비교와 적용 차단 로직은 MySQL 메타데이터 형식의 단위 테스트로 확인합니다.
현재 환경에 MySQL/Docker CLI가 없어 실제 MySQL에서 V1 DDL 실행은 검증하지 못했습니다.
운영 DB 접속/변경/배포도 실행하지 않았습니다. 반드시 개발 DB check부터 진행하세요.

## 참고

- [Spring Boot — Flyway 초기화](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
- [Flyway baseline](https://documentation.red-gate.com/flyway/reference/commands/baseline)
- [Flyway baseline-on-migrate 주의사항](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-baseline-on-migrate-setting)
