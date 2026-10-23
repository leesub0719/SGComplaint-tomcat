package com.transit.SGComplaint.service;

import com.transit.SGComplaint.SGComplaintApplication;
import com.transit.SGComplaint.DTO.SignupAgreementEvidence;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = SGComplaintApplication.class)
class SignupPageTests {
    @Autowired WebApplicationContext context;

    @Test void signupRequiresAgreement() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build().perform(get("/signup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/terms"));
    }

    @Test void signupRendersWithoutSmsOrAddress() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("signupAgreementEvidence", new SignupAgreementEvidence(
                "2026-09-04", "2026-09-07", LocalDateTime.now()));
        var mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        mvc.perform(get("/signup").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"phone\"")))
                .andExpect(content().string(not(containsString("id=\"request-phone-code\""))))
                .andExpect(content().string(not(containsString("id=\"postcode\""))));
    }
}
