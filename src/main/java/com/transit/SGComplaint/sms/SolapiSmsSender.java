package com.transit.SGComplaint.sms;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import com.transit.SGComplaint.config.SmsProperties;
import com.transit.SGComplaint.service.PhoneNumberUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SolapiSmsSender implements SmsSender {

    private final SmsProperties properties;

    public SolapiSmsSender(SmsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendVerificationCode(String phone, String code) {
        SmsProperties.Solapi solapi = properties.getSolapi();

        if (!StringUtils.hasText(solapi.getApiKey())
                || !StringUtils.hasText(solapi.getApiSecret())
                || !StringUtils.hasText(solapi.getSender())) {
            throw new SmsSendException(
                    "SOLAPI API Key, Secret Key, 발신번호 설정이 필요합니다.");
        }

        try {
            DefaultMessageService messageService =
                    SolapiClient.INSTANCE.createInstance(
                            solapi.getApiKey(), solapi.getApiSecret());

            Message message = new Message();
            message.setFrom(PhoneNumberUtils.digitsOnly(solapi.getSender()));
            message.setTo(phone);
            message.setText(
                    "[(주) 서경 마을버스] 휴대전화 인증번호는 ["
                    + code + "]입니다. 3분 안에 입력해 주세요.");

            messageService.send(message);
        } catch (Exception exception) {
            throw new SmsSendException(
                    "인증번호 문자 발송에 실패했습니다.", exception);
        }
    }
}
