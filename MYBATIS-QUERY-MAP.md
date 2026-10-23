# (주) 서경 마을버스 완전 MyBatis 버전

이 프로젝트의 모든 데이터베이스 접근은 MyBatis SQL XML로 실행됩니다.
Spring Data JPA와 Hibernate 의존성은 제거했으며, `domain` 클래스도 JPA Entity가 아닌
일반 Java 객체(POJO)입니다.

## SQL 위치

`src/main/resources/mapper`

| 업무 | SQL XML | 주요 쿼리 |
|---|---|---|
| 회원가입·로그인·회원관리·계정복구 | `full/EmployeeRepository.xml` | 중복확인, 회원 INSERT, 로그인 조회, 비밀번호/권한 UPDATE, 회원검색 |
| 휴대전화 인증 | `full/PhoneVerificationRepository.xml` | 인증 생성, 최근 인증 조회, 실패/성공/사용 UPDATE |
| 민원 | `full/ComplaintRepository.xml` | 등록, 상세, 기간조회, 공개검색, 관리자 페이징, 상태변경 |
| 민원 첨부파일 | `full/ComplaintAttachmentRepository.xml` | 첨부 INSERT, 단건/복수 조회 |
| 답변 | `full/ComplaintAnswerRepository.xml` | 답변 INSERT/UPDATE, 민원별 조회 |
| 답변 첨부파일 | `full/ComplaintAnswerAttachmentRepository.xml` | 첨부 INSERT, 개수/단건/복수 조회 |
| 공지사항 | `full/NoticeRepository.xml` | 등록/수정, 검색/페이징, 팝업/상단 조회 |
| 공지 이미지 | `full/NoticeImageRepository.xml` | 이미지 INSERT/조회 |
| 메인 배너 | `MainBannerMapper.xml`, `full/MainBannerRepository.xml` | 목록/개수/순서, INSERT/DELETE |
| 협력업체 | `PartnerMapper.xml`, `full/PartnerRepository.xml` | 동적 검색/페이징, INSERT/UPDATE |
| 마을버스 운행안내 | `RouteOperationMapper.xml`, `full/RouteOperationRepository.xml` | 유형별 조회/개수, INSERT/UPDATE |
| 똑버스 안내 이미지 | `DdokBusGuideImageMapper.xml`, `full/DdokBusGuideImageRepository.xml` | 목록/개수, INSERT/DELETE |

XML 파일은 총 16개이며 SELECT/INSERT/UPDATE/DELETE statement는 총 71개입니다.

## Java Mapper 위치

- 일반 조회 Mapper: `src/main/java/com/transit/SGComplaint/mapper`
- 기존 Service 호출명 호환 Mapper: `src/main/java/com/transit/SGComplaint/repository`

`repository` 폴더의 인터페이스도 Spring Data Repository가 아닙니다. 모두 `@Mapper`로
등록되는 MyBatis Mapper입니다. 기존 Service 코드를 안전하게 유지하기 위해 클래스
이름만 `Repository`로 남겼으며 실제 실행 SQL은 대응 XML에 전부 명시되어 있습니다.
회원 관련 인터페이스는 학습하기 쉽도록 `mapper/EmployeeMapper.java`로 분리했습니다.

## 회원가입 SQL 따라가기

1. `MemberController.signup()`
2. `EmployeeService.signupUser()`
3. `EmployeeMapper.existsByEmpId()`
4. `EmployeeMapper.insertEmployee()`
5. `full/EmployeeRepository.xml`의 같은 `id`를 가진 SQL

비밀번호는 Service에서 BCrypt 암호화한 뒤 `emp_password`에 저장됩니다.

## SQL 로그

다음 설정이 이미 적용되어 있어 Eclipse Console에서 SQL 준비와 파라미터를 확인할 수 있습니다.

```properties
logging.level.com.transit.SGComplaint.mapper=DEBUG
logging.level.com.transit.SGComplaint.repository=DEBUG
```

더 자세한 MyBatis 실행 로그가 필요하면 아래도 추가할 수 있습니다.

```properties
mybatis.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
```

## 실행 전 설정

Eclipse `Run Configurations > Environment`에 설정합니다.

```text
DB_USERNAME=개발DB계정
DB_PASSWORD=개발DB비밀번호
SOLAPI_API_KEY=API키
SOLAPI_API_SECRET=Secret키
SOLAPI_SENDER=등록된발신번호
```

DB 스키마는 `database/schema.sql`을 먼저 실행하고, 필요한 기능별 migration SQL을
적용합니다.

## 안전한 SQL 작성 규칙

- 사용자 입력은 항상 `#{parameter}`로 바인딩합니다.
- `${parameter}` 문자열 치환은 SQL Injection 위험이 있으므로 사용하지 않습니다.
- 검색 조건은 `<where>`, `<if>`, 목록 조건은 `<foreach>`로 확인할 수 있습니다.
- 페이징은 목록 SQL의 `LIMIT/OFFSET`과 별도의 `COUNT(*)` SQL로 구성됩니다.
