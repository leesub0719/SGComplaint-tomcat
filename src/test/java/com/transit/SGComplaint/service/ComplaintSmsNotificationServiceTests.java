package com.transit.SGComplaint.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transit.SGComplaint.domain.ComplaintSmsNotification;
import com.transit.SGComplaint.event.ComplaintAnsweredEvent;
import com.transit.SGComplaint.mapper.ComplaintSmsNotificationMapper;
import com.transit.SGComplaint.sms.SmsSendException;
import com.transit.SGComplaint.sms.SmsSender;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ComplaintSmsNotificationServiceTests {

    private final ComplaintSmsNotificationMapper mapper =
            Mockito.mock(ComplaintSmsNotificationMapper.class);
    private final SmsSender smsSender = Mockito.mock(SmsSender.class);

    @Test
    void sendsOnePrivacySafeMessageForARegisteredAnswer() throws Exception {
        ComplaintSmsNotificationService service = service(true);
        when(mapper.insertIfAbsent(any())).thenAnswer(invocation -> {
            ComplaintSmsNotification notification = invocation.getArgument(0);
            setNotificationNo(notification, 7L);
            return 1;
        });
        when(mapper.claim(7L)).thenReturn(1);
        when(mapper.findById(7L)).thenAnswer(invocation -> {
            ArgumentCaptor<ComplaintSmsNotification> captor =
                    ArgumentCaptor.forClass(ComplaintSmsNotification.class);
            verify(mapper).insertIfAbsent(captor.capture());
            return Optional.of(captor.getValue());
        });

        service.handleAnswerRegistered(
                new ComplaintAnsweredEvent(31L, "010-1234-5678"));

        verify(smsSender).sendMessage(
                eq("01012345678"),
                eq("[서경버스] 민원 31번 답변이 등록되었습니다. 사이트에서 확인해 주세요."));
        verify(mapper).markSent(7L);
    }

    @Test
    void disabledFeatureDoesNotCreateOrSendANotification() {
        service(false).handleAnswerRegistered(
                new ComplaintAnsweredEvent(31L, "010-1234-5678"));

        verify(mapper, never()).insertIfAbsent(any());
        verify(smsSender, never()).sendMessage(any(), any());
    }

    @Test
    void invalidRecipientIsRejectedBeforeCreatingARecord() {
        service(true).handleAnswerRegistered(
                new ComplaintAnsweredEvent(31L, "032-123-4567"));

        verify(mapper, never()).insertIfAbsent(any());
        verify(smsSender, never()).sendMessage(any(), any());
    }

    @Test
    void smsFailureIsRecordedWithoutEscapingToAnswerTransaction() throws Exception {
        ComplaintSmsNotificationService service = service(true);
        when(mapper.insertIfAbsent(any())).thenAnswer(invocation -> {
            ComplaintSmsNotification notification = invocation.getArgument(0);
            setNotificationNo(notification, 8L);
            return 1;
        });
        when(mapper.claim(8L)).thenReturn(1);
        when(mapper.findById(8L)).thenAnswer(invocation -> {
            ArgumentCaptor<ComplaintSmsNotification> captor =
                    ArgumentCaptor.forClass(ComplaintSmsNotification.class);
            verify(mapper).insertIfAbsent(captor.capture());
            return Optional.of(captor.getValue());
        });
        Mockito.doThrow(new SmsSendException("외부 문자 서비스 오류"))
                .when(smsSender).sendMessage(any(), any());

        service.handleAnswerRegistered(
                new ComplaintAnsweredEvent(31L, "010-1234-5678"));

        verify(mapper).markFailed(8L, "외부 문자 서비스 오류");
    }

    @Test
    void notificationStorageFailureDoesNotEscapeAfterAnswerCommit() {
        when(mapper.insertIfAbsent(any()))
                .thenThrow(new IllegalStateException("알림 DB 오류"));

        service(true).handleAnswerRegistered(
                new ComplaintAnsweredEvent(31L, "010-1234-5678"));

        verify(smsSender, never()).sendMessage(any(), any());
    }

    private ComplaintSmsNotificationService service(boolean enabled) {
        return new ComplaintSmsNotificationService(mapper, smsSender, enabled);
    }

    private static void setNotificationNo(
            ComplaintSmsNotification notification,
            Long notificationNo) throws Exception {
        Field field = ComplaintSmsNotification.class
                .getDeclaredField("notificationNo");
        field.setAccessible(true);
        field.set(notification, notificationNo);
    }
}
