package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.JobApplicationRequest;
import com.transit.SGComplaint.service.ApplicationFileValidationException;
import com.transit.SGComplaint.service.RecruitmentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecruitmentControllerTests {

    @Test
    void asyncSubmissionReturnsOnlyTheInvalidFileField() {
        RecruitmentService service = mock(RecruitmentService.class);
        RecruitmentController controller = new RecruitmentController(service);
        JobApplicationRequest request = new JobApplicationRequest();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(request, "applicationRequest");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        when(service.apply(1L, request)).thenThrow(new ApplicationFileValidationException(
                "driverLicense", "운전면허증 사본: PDF 파일 형식이 올바르지 않습니다.", null));

        var response = controller.applyAsync(1L, request, bindingResult, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("driverLicense", response.getBody().get("field"));
    }

    @Test
    void asyncSubmissionReturnsContextAwareSuccessUrl() {
        RecruitmentService service = mock(RecruitmentService.class);
        RecruitmentController controller = new RecruitmentController(service);
        JobApplicationRequest request = new JobApplicationRequest();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(request, "applicationRequest");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setContextPath("/sgcomplaint");
        when(service.apply(1L, request)).thenReturn(10L);

        var response = controller.applyAsync(1L, request, bindingResult, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("/sgcomplaint/recruit/notices/1?applied",
                response.getBody().get("redirectUrl"));
    }
}
