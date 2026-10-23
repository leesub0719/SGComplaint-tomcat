# 외부 Tomcat 장애 대응 실행서

## 기본 원칙

화면에 보이는 마지막 예외만 수정하지 말고 요청 경로와 로그의 **첫 번째 `Caused by`**를
찾습니다. 변경 전 로그·환경·WAR 체크섬을 보존하고, 원인 범위를 Tomcat·Spring·DB·프록시로
나눠 확인합니다.

## 로그 위치

| 환경 | 주요 로그 |
|---|---|
| Linux 패키지/systemd | `journalctl -u tomcat11`, `CATALINA_BASE/logs/catalina.out` |
| Tomcat 공통 | `logs/catalina.YYYY-MM-DD.log`, `logs/localhost.YYYY-MM-DD.log` |
| HTTP 요청 | AccessLogValve를 켠 경우 `logs/localhost_access_log.YYYY-MM-DD.txt` |
| Windows 서비스 | `CATALINA_BASE/logs`, Windows 이벤트 뷰어, 서비스 stdout/stderr 설정 경로 |

비밀번호, 인증 토큰, 주민정보와 민원 본문 전체를 장애 기록이나 채용 포트폴리오 캡처에
노출하지 않습니다.

## 1. 배포 후 404

확인 순서:

1. `webapps/sgcomplaint.war` 파일명과 권한
2. `logs/localhost.*.log`의 WAR 전개 실패
3. `/sgcomplaint/actuator/health`처럼 Context Path 포함 여부
4. Thymeleaf/JavaScript 링크에 서버 루트 절대경로가 남았는지 확인

대표 원인은 잘못된 Context Path, 읽기 권한, WAR 손상, 중복 Context 설정입니다.

## 2. 시작 중 500 또는 Context 시작 실패

```bash
journalctl -u tomcat11 --since "10 minutes ago" --no-pager
grep -R "Caused by:" /opt/tomcat/logs
```

첫 원인이 `BeanCreationException`이면 그 아래의 MyBatis XML, DB 연결, 필수 환경변수 오류를
확인합니다. `DB_PASSWORD`, `REMEMBER_ME_KEY`, 업로드 절대경로가 운영에 주입되었는지 확인하되
값 자체를 로그에 출력하지 않습니다.

## 3. POST/PUT/DELETE 403

- 로그인 세션 쿠키가 같은 호스트·Context Path로 전송되는지 확인합니다.
- `/api/csrf` 또는 Thymeleaf meta 태그에서 받은 헤더 이름과 토큰을 함께 전송합니다.
- HTTPS 프록시 환경에서 `X-Forwarded-Proto`와 secure cookie 설정을 확인합니다.
- 권한 오류를 해결하려고 CSRF나 Spring Security를 전역 비활성화하지 않습니다.

## 4. DB 연결 실패

로그 키워드:

```text
Communications link failure
Access denied for user
HikariPool
Too many connections
```

네트워크와 포트를 확인한 뒤 DB URL, 계정 권한, 최대 연결 수, Hikari 풀을 순서대로 봅니다.
운영 DB 계정은 애플리케이션에 필요한 스키마 권한만 부여합니다.

## 5. MyBatis 매핑 실패

`Failed to parse mapping resource`, `Invalid bound statement`, SQL 문법 오류가 나오면 다음을
확인합니다.

1. 오류 로그에 표시된 XML 파일과 statement id
2. Mapper 인터페이스 메서드명·파라미터명
3. DTO 필드와 SQL 별칭
4. WAR 안에 최신 mapper XML이 포함됐는지 `jar tf`로 확인

수정 후 `clean test bootWar`로 오래된 빌드 산출물을 제거하고 다시 검증합니다.

## 6. 메모리·응답 지연

- Tomcat 요청 스레드 사용량과 access log 응답시간
- JVM heap/GC와 `OutOfMemoryError`
- Hikari 활성·대기 연결 수
- 느린 SQL과 외부 SMS 호출 시간
- 부하테스트의 오류율, 처리량, p95

평균 응답시간만으로 판단하지 않습니다. p95는 요청 100개 중 느린 쪽 5개를 제외한 경계값으로,
대부분 사용자가 겪는 지연 상한을 파악하는 데 사용합니다. 먼저 병목이 Tomcat 스레드, DB 풀,
SQL, 외부 연동 중 어디인지 구분한 뒤 한 항목씩 조정하고 같은 조건으로 재시험합니다.

## 포트폴리오용 장애 기록 형식

```text
현상: WAR 배포 후 /sgcomplaint에서 404 발생
영향: 사용자와 관리자가 서비스에 접근하지 못함
근거: localhost 로그에 Context 전개 실패와 중복 Context 메시지 확인
원인: server.xml Context와 conf/Catalina/localhost 설정을 동시에 등록
조치: server.xml 설정 제거, sgcomplaint.xml 한 곳으로 통합 후 재배포
검증: health UP, HTML 200, 공개 REST API JSON, 로그인/관리자 답변 정상
예방: CI WAR 구조 검사와 배포 체크리스트에 중복 Context 점검 추가
```

실제 발생한 로그와 검증 결과가 있을 때만 자신의 해결 사례로 제시합니다.
