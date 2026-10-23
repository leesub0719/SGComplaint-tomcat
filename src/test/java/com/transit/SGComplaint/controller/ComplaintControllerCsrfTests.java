package com.transit.SGComplaint.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.transit.SGComplaint.service.ComplaintService;
import com.transit.SGComplaint.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.Page;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.ExtendedModelMap;

class ComplaintControllerCsrfTests {

    @Test
    void complaintListResolvesDeferredCsrfTokenBeforeLoadingPage() {
        EmployeeService employeeService = mock(EmployeeService.class);
        ComplaintService complaintService = mock(ComplaintService.class);
        CsrfToken csrfToken = mock(CsrfToken.class);
        when(complaintService.getPublicComplaints("ALL", "", 0)).thenReturn(Page.empty());

        ComplaintController controller = new ComplaintController(employeeService, complaintService);
        String view = controller.complaintList(
                "ALL", "", 0, csrfToken, new ExtendedModelMap());

        assertEquals("complaint/list", view);
        InOrder order = inOrder(csrfToken, complaintService);
        order.verify(csrfToken).getToken();
        order.verify(complaintService).getPublicComplaints("ALL", "", 0);
    }
}
