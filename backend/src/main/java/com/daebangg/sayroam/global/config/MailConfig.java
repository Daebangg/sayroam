package com.daebangg.sayroam.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
@Import(MailSenderAutoConfiguration.class)
public class MailConfig {
    // 비활성화 시 JavaMailSender가 없으므로 Boot의 시작 시 SMTP 연결 검사도 수행되지 않는다.
}
