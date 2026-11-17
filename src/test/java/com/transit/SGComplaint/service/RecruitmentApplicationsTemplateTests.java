package com.transit.SGComplaint.service;

import com.transit.SGComplaint.SGComplaintApplication;
import com.transit.SGComplaint.DTO.JobApplicationItem;
import com.transit.SGComplaint.DTO.JobPostingDetail;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SGComplaintApplication.class)
class RecruitmentApplicationsTemplateTests {
    @Autowired SpringTemplateEngine templateEngine;
    @Autowired ServletContext servletContext;

    @Test
    void rendersRecruitmentApplicationsWithApplicantPopup() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/sgcomplaint");
        MockHttpServletResponse response = new MockHttpServletResponse();
        IWebExchange exchange = JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(request, response);
        WebContext context = new WebContext(exchange, Locale.KOREAN);

        context.setVariable("activeMenu", "recruitments");
        context.setVariable("adminName", "관리자");
        context.setVariable("isMaster", true);
        context.setVariable("posting", new JobPostingDetail(
                1L, "버스운전기사 모집", "채용 내용", "2026-12-31",
                "접수중", "open", true, List.of("RESUME")));
        context.setVariable("applications", List.of(new JobApplicationItem(
                1L, "지원자", "010-0000-0000", "applicant@example.com",
                "경력", "지원 메모", "2026.11.10 10:00", 2)));

        String html = templateEngine.process("admin/recruitment-applications", context);

        assertTrue(html.contains("지원자 상세정보"));
        assertTrue(html.contains("2개"));
    }
}
