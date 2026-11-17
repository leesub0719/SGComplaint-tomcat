package com.transit.SGComplaint.service;

import com.transit.SGComplaint.SGComplaintApplication;
import com.transit.SGComplaint.DTO.JobApplicationRequest;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SGComplaintApplication.class)
class RecruitmentApplyTemplateTests {
    @Autowired SpringTemplateEngine templateEngine;
    @Autowired ServletContext servletContext;

    @Test
    void rendersOnlyDocumentsSelectedForThePosting() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/sgcomplaint");
        MockHttpServletResponse response = new MockHttpServletResponse();
        IWebExchange exchange = JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(request, response);
        WebContext context = new WebContext(exchange, Locale.KOREAN);

        context.setVariable("posting", new JobPostingDetail(
                1L, "버스운전기사 모집", "채용 내용", "2026-12-31",
                "접수중", "open", true,
                List.of("RESUME", "DRIVER_LICENSE", "RESIDENT_REGISTRATION")));
        context.setVariable("applicationRequest", new JobApplicationRequest());

        String html = templateEngine.process("recruit/apply", context);

        assertTrue(html.contains("name=\"resume\""));
        assertTrue(html.contains("name=\"driverLicense\""));
        assertTrue(html.contains("주민등록등본"));
        assertFalse(html.contains("name=\"busDriverLicense\""));
        assertFalse(html.contains("name=\"careerCertificate\""));
        assertFalse(html.contains("채용신체검사서"));
    }
}
