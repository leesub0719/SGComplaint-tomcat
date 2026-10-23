# 외부 Tomcat 10.1 설치와 설정

## 1. 설치 기준

- JDK 17과 Tomcat 10.1을 전용 서비스 계정으로 실행합니다.
- `CATALINA_HOME`은 Tomcat 프로그램, `CATALINA_BASE`는 설정·로그·배포 파일로 분리할 수 있습니다.
- Manager/Host Manager 예제 애플리케이션은 운영에서 제거하거나 외부 접근을 차단합니다.
- Tomcat 관리 포트는 인터넷에 직접 공개하지 않고 Nginx/IIS 같은 리버스 프록시 뒤에 둡니다.

## 2. 환경변수

운영 필수값:

```text
SPRING_PROFILES_ACTIVE=prod,tomcat
DB_URL
DB_USERNAME
DB_PASSWORD
REMEMBER_ME_KEY
COMPLAINT_UPLOAD_DIR
ANSWER_UPLOAD_DIR
NOTICE_IMAGE_UPLOAD_DIR
MAIN_BANNER_UPLOAD_DIR
DDOKBUS_GUIDE_DIR
```

Linux에서 `setenv.sh`를 사용한다면 권한을 제한합니다.

```bash
sudo install -o tomcat -g tomcat -m 0600 \
  deploy/tomcat/setenv.sh.example /opt/tomcat/bin/setenv.sh
sudo editor /opt/tomcat/bin/setenv.sh
```

Windows에서 `startup.bat`로 실행할 때만 `setenv.bat`가 적용됩니다. Windows 서비스는
`tomcat10w.exe` 또는 시스템 환경변수에 같은 값을 설정하고 서비스를 재시작해야 합니다.

## 3. Context Path

Tomcat은 기본적으로 WAR 파일명을 Context Path로 사용합니다.

```text
sgcomplaint.war -> /sgcomplaint
ROOT.war        -> /
```

운영 예시는 `/sgcomplaint`를 사용합니다. `sgcomplaint.xml.example`을 사용할 경우 파일명도
반드시 `sgcomplaint.xml`이어야 하며 `<Context path="...">`를 추가하지 않습니다.

JavaScript에서는 `/api/...` 같은 서버 루트 절대경로를 하드코딩하면 안 됩니다. Thymeleaf가
Context Path를 포함한 기준 URL을 전달하거나 현재 문서 URL을 기준으로 API 주소를 구성해야 합니다.

## 4. 리버스 프록시

HTTPS 종료를 프록시가 담당한다면 최소한 다음 헤더를 전달합니다.

```text
X-Forwarded-For
X-Forwarded-Proto
X-Forwarded-Host
```

`sgcomplaint.xml.example`의 `RemoteIpValve`는 같은 서버의 루프백 프록시만 신뢰합니다.
프록시가 다른 주소에 있으면 실제 사설 IP 대역만 `internalProxies`에 추가합니다. 모든 주소를
신뢰하도록 설정하면 클라이언트가 HTTPS 여부와 원격 주소를 위조할 수 있습니다.

## 5. 파일 권한

- Tomcat 프로그램 디렉터리는 서비스 계정이 임의로 수정하지 못하게 합니다.
- `webapps`, `logs`, `temp`, `work`와 외부 업로드 경로만 필요한 권한을 줍니다.
- 업로드 경로는 WAR 전개 디렉터리 밖에 둡니다. 재배포 시 삭제되지 않아야 합니다.
- 환경변수 파일과 DB 백업은 관리자·서비스 계정 외에는 읽지 못하게 합니다.

## 6. 최초 확인

```bash
curl --fail http://127.0.0.1:8080/sgcomplaint/actuator/health
curl -I http://127.0.0.1:8080/sgcomplaint/
```

health가 `UP`이어도 로그인, 공개 민원 JSON, 파일 업로드·다운로드와 관리자 답변을 별도로
확인해야 합니다.
