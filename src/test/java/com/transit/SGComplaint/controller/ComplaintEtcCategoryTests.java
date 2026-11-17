package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.service.ComplaintService;
import com.transit.SGComplaint.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplaintEtcCategoryTests {

    @Test
    void complaintListSupportsEtcCategory() {
        EmployeeService employeeService = mock(EmployeeService.class);
        ComplaintService complaintService = mock(ComplaintService.class);
        CsrfToken csrfToken = mock(CsrfToken.class);
        when(complaintService.getPublicComplaints("ETC", "", 0))
                .thenReturn(Page.empty());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = new ComplaintController(employeeService, complaintService)
                .complaintList("ETC", "", 0, csrfToken, model);

        assertEquals("complaint/list", view);
        assertEquals("ETC", model.get("selectedCategory"));
        assertEquals("기타", model.get("boardTitle"));
        verify(complaintService).getPublicComplaints("ETC", "", 0);
    }

    @Test
    void complaintFormSupportsEtcCategory() {
        EmployeeService employeeService = mock(EmployeeService.class);
        ComplaintService complaintService = mock(ComplaintService.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("member");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = new ComplaintController(employeeService, complaintService)
                .complaintForm(authentication, "ETC", model);

        assertEquals("complaint/create", view);
        assertEquals("ETC", model.get("selectedCategory"));
        assertEquals("기타 문의 글쓰기", model.get("formTitle"));
        assertEquals("문의 제목을 입력해 주세요", model.get("titlePlaceholder"));
        verify(employeeService).getRequiredActiveEmployee("member");
    }
}
