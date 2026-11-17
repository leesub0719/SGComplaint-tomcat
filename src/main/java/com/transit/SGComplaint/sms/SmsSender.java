package com.transit.SGComplaint.sms;

public interface SmsSender {
    void sendVerificationCode(String phone, String code);

    void sendMessage(String phone, String text);
}
