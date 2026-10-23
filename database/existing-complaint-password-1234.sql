-- 기존 민원 중 게시글 비밀번호가 없는 게시물을 비밀번호 1234로 설정합니다.
-- BCrypt strength 10으로 생성한 해시이며 Spring Security PasswordEncoder와 호환됩니다.
USE bus_complaint_db;

START TRANSACTION;

UPDATE sgtransit_complaint
   SET complaint_password = '$2a$10$V6R3ECwakLkBKeEQGhWjyOlEUMUjkplMt8HyxjkmYET.aePEzi.We',
       updated_at = CURRENT_TIMESTAMP
 WHERE complaint_password IS NULL
    OR complaint_password = '';

SELECT ROW_COUNT() AS updated_complaint_count;

COMMIT;

-- 적용 결과를 확인합니다.
SELECT complaint_no,
       complaint_title,
       IF(complaint_password IS NULL OR complaint_password = '', 'MISSING', 'READY') AS password_state
  FROM sgtransit_complaint
 ORDER BY complaint_no DESC;
