package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.EmployeeSignupRequest;
import com.transit.SGComplaint.DTO.SignupAgreementEvidence;
import com.transit.SGComplaint.config.AuthenticationRateLimitFilter;
import com.transit.SGComplaint.config.SmsProperties;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.domain.PhoneVerification;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import com.transit.SGComplaint.mapper.SignupConsentMapper;
import com.transit.SGComplaint.mapper.WithdrawalHistoryMapper;
import com.transit.SGComplaint.repository.PhoneVerificationRepository;
import com.transit.SGComplaint.sms.SmsSender;
import jakarta.validation.Validation;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationHardeningTests {
    private final PhoneVerificationRepository repository = mock(PhoneVerificationRepository.class);
    private final SmsSender sender = mock(SmsSender.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AbuseLimitService limits = mock(AbuseLimitService.class);
    private final PhoneVerificationService service = new PhoneVerificationService(
            repository, sender, encoder, new SmsProperties(), limits);

    private void session() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }
    @AfterEach void clearSession() { RequestContextHolder.resetRequestAttributes(); }

    @Test void signupNeedsNeitherAddressNorSms() {
        EmployeeSignupRequest request = new EmployeeSignupRequest();
        request.setEmpId("test1234"); request.setEmpPassword("password123");
        request.setPasswordConfirm("password123"); request.setEmpName("테스트");
        request.setEmpEmail("test@example.com"); request.setEmpPhone("01012345678");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
        }
        EmployeeMapper employees = mock(EmployeeMapper.class);
        SignupConsentMapper consents = mock(SignupConsentMapper.class);
        PhoneVerificationService sms = mock(PhoneVerificationService.class);
        when(employees.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        new EmployeeService(employees, consents, sms, encoder,
                mock(WithdrawalHistoryMapper.class), mock(PersistentTokenRepository.class), 30).signupUser(request,
                new SignupAgreementEvidence("2026-09-04", "2026-09-07", LocalDateTime.now()));
        verifyNoInteractions(sms);
        verify(employees).saveAndFlush(argThat(e -> "".equals(e.getEmpAddress())));
        verify(consents).insertSignupConsent(any(), anyString(), anyString(), any());
    }

    @Test void deniedQuotaNeverSendsSms() {
        session();
        when(limits.allow(anyString(), anyString(), anyInt(), anyInt())).thenReturn(false);
        assertThrows(PhoneVerificationException.class, () -> service.requestCode("01012345678", "FIND_ID"));
        verifyNoInteractions(sender);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test void usedCodeCannotBeVerifiedAgain() {
        session();
        PhoneVerification verification = PhoneVerification.create("01012345678", "scope", "hash", LocalDateTime.now().plusMinutes(3));
        verification.markUsed();
        when(repository.findTopByPhoneOrderByRequestedAtDesc(anyString(), anyString())).thenReturn(Optional.of(verification));
        assertThrows(PhoneVerificationException.class, () -> service.verifyCode("01012345678", "123456", "FIND_ID"));
        verifyNoInteractions(encoder);
    }

    @Test void wrongCodePersistsAttempt() {
        session();
        PhoneVerification verification = PhoneVerification.create("01012345678", "scope", "hash", LocalDateTime.now().plusMinutes(3));
        when(repository.findTopByPhoneOrderByRequestedAtDesc(anyString(), anyString())).thenReturn(Optional.of(verification));
        assertThrows(PhoneVerificationException.class, () -> service.verifyCode("01012345678", "123456", "FIND_ID"));
        verify(repository).save(verification);
        assertTrue(verification.hasReachedAttemptLimit(1));
    }

    @Test void tokenCannotCrossPurposeOrSession() {
        session();
        when(repository.findByPhoneAndVerificationTokenHashAndUsedAtIsNull(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        assertThrows(PhoneVerificationException.class, () -> service.consumeVerification("01012345678", "token", "FIND_ID"));
        assertThrows(PhoneVerificationException.class, () -> service.consumeVerification("01012345678", "token", "RESET:7"));
        session();
        assertThrows(PhoneVerificationException.class, () -> service.consumeVerification("01012345678", "token", "RESET:7"));
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository, times(3)).findByPhoneAndVerificationTokenHashAndUsedAtIsNull(anyString(), anyString(), captor.capture());
        assertEquals(3, captor.getAllValues().stream().distinct().count());
    }

    @Test void ipQuotaReturns429AndStopsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/account-recovery/password/request");
        request.setServletPath("/api/account-recovery/password/request");
        MockHttpServletResponse response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = mock(jakarta.servlet.FilterChain.class);
        new AuthenticationRateLimitFilter(limits).doFilter(request, response, chain);
        assertEquals(429, response.getStatus());
        verifyNoInteractions(chain);
    }
}
