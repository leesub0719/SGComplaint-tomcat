package com.transit.SGComplaint.service;

import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.mapper.AdminDeletionAuditMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MinorOperationalImprovementsTests {

    @Test void myPageSkipLinkHasHiddenUntilFocusedCss() throws Exception {
        String css = resource("/static/css/public-layout.css");
        String html = resource("/templates/mypage/inquiries.html");
        assertTrue(html.contains("class=\"public-skip-link\""));
        assertTrue(css.contains(".public-skip-link"));
        assertTrue(css.contains(".public-skip-link:focus"));
    }

    @Test void memberAndInquiryQueriesArePaged() throws Exception {
        String employees = resource("/mapper/full/EmployeeMapper.xml");
        String complaints = resource("/mapper/full/ComplaintRepository.xml");
        assertTrue(employees.contains("<select id=\"selectMemberPage\""));
        assertTrue(employees.contains("LIMIT #{limit}"));
        assertTrue(employees.contains("OFFSET #{offset}"));
        assertTrue(complaints.contains("<select id=\"selectMemberPage\""));
        assertTrue(resource("/templates/mypage/inquiries.html").contains("inquiryPage.totalElements"));
        assertTrue(resource("/templates/admin/members.html").contains("memberPage.totalPages"));
    }

    @Test void indexFriendlySearchDoesNotUseLeadingWildcard() throws Exception {
        for (String path : new String[]{"/mapper/full/EmployeeMapper.xml",
                "/mapper/full/ComplaintRepository.xml", "/mapper/full/NoticeRepository.xml",
                "/mapper/PartnerMapper.xml"}) {
            String xml = resource(path);
            assertFalse(xml.contains("CONCAT('%'"), path);
        }
    }

    @Test void v2CreatesOperationalHistoryAndPersistentLoginTables() throws Exception {
        String sql = resource("/db/migration/V2__minor_operational_improvements.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS persistent_logins"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sgtransit_admin_delete_audit"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sgtransit_withdrawal_id_history"));
        assertTrue(sql.contains("information_schema.STATISTICS"));
        assertTrue(sql.contains("INDEX_NAME='idx_partner_name'"));
        assertFalse(sql.toUpperCase().contains("DROP TABLE"));
        assertFalse(sql.toUpperCase().contains("TRUNCATE"));
    }

    @Test void deletionAuditProducesValidEscapedSnapshot() {
        AdminDeletionAuditMapper mapper = mock(AdminDeletionAuditMapper.class);
        Employee admin = Employee.createAdmin("admin", "encoded", "관리자",
                "admin@example.com", "01000000000", "");
        new AdminDeletionAuditService(mapper).record(admin, "NOTICE", 7L,
                "제목", Map.of("title", "따옴표 \" 와 줄\n바꿈", "count", 2));
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(mapper).insert(isNull(), eq("admin"), eq("NOTICE"), eq(7L), eq("제목"), json.capture());
        assertTrue(json.getValue().startsWith("{") && json.getValue().endsWith("}"));
        assertTrue(json.getValue().contains("\"count\":2"));
        assertTrue(json.getValue().contains("\"title\":\"따옴표 \\\" 와 줄\\n바꿈\""));
    }

    private String resource(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
