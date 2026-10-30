package com.daebangg.sayroam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;

// 메일 자동 설정은 app.mail.enabled 조건을 적용한 MailConfig에서 가져온다.
@SpringBootApplication(exclude = MailSenderAutoConfiguration.class)
public class SayroamApplication {

    public static void main(String[] args) {
        SpringApplication.run(SayroamApplication.class, args);
    }

}
