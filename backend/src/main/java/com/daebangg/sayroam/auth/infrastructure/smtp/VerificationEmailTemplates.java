package com.daebangg.sayroam.auth.infrastructure.smtp;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.auth.infrastructure.config.AccountVerificationProperties;
import com.daebangg.sayroam.auth.infrastructure.config.EmailVerificationProperties;
import com.daebangg.sayroam.auth.infrastructure.config.PasswordResetProperties;
import com.daebangg.sayroam.global.mail.BrandedEmailContent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

@Component
public class VerificationEmailTemplates {
    private final EmailVerificationProperties signup;
    private final AccountVerificationProperties account;
    private final PasswordResetProperties reset;

    public VerificationEmailTemplates(EmailVerificationProperties signup, AccountVerificationProperties account,
                                      PasswordResetProperties reset) {
        this.signup = signup;
        this.account = account;
        this.reset = reset;
    }

    public BrandedEmailContent signup(String code) {
        return verification("이메일 인증", "여행의 시작을 함께해요", "이메일 주소를 확인해 주세요",
                "SayRoam 가입을 위해 인증 화면에 아래 코드를 입력해 주세요.",
                List.of(), code, signup.codeTtlSeconds(), "회원가입 이메일 인증에만 사용할 수 있어요.");
    }

    public BrandedEmailContent account(String code, AccountVerificationPurpose purpose) {
        return switch (purpose) {
            case PASSWORD_CHANGE -> verification("비밀번호 변경 본인 확인", "계정을 안전하게",
                    "비밀번호 변경을 확인해 주세요", "새 비밀번호로 변경하려면 인증 화면에 아래 코드를 입력해 주세요.",
                    List.of(), code, account.codeTtlSeconds(), "비밀번호 변경 본인 확인에만 사용할 수 있어요.");
            case ACCOUNT_DELETION -> verification("회원 탈퇴 본인 확인", "계정 본인 확인",
                    "회원 탈퇴 요청을 확인해 주세요", "본인이 요청한 회원 탈퇴라면 인증 화면에 아래 코드를 입력해 주세요.",
                    List.of("코드 확인만으로 탈퇴가 완료되지는 않아요. 마지막 안내를 확인한 뒤 직접 진행해 주세요."),
                    code, account.codeTtlSeconds(), "회원 탈퇴 본인 확인에만 사용할 수 있어요.");
        };
    }

    public BrandedEmailContent passwordReset(String code) {
        return verification("비밀번호 재설정 본인 확인", "다시 여행을 이어가요", "비밀번호를 재설정해 주세요",
                "비밀번호 재설정을 요청하셨다면 인증 화면에 아래 코드를 입력해 주세요.",
                List.of("기존 이메일·비밀번호 로그인 계정에만 재설정을 진행할 수 있어요.",
                        "소셜 로그인으로만 가입했다면 해당 소셜 로그인을 이용해 주세요."),
                code, reset.codeTtlSeconds(), "비밀번호 재설정 본인 확인에만 사용할 수 있어요.");
    }

    private BrandedEmailContent verification(String subject, String category, String title, String introduction,
                                              List<String> details, String code, int ttlSeconds, String purposeNotice) {
        Assert.isTrue(code != null && code.matches("[0-9]{6}"), "Verification code must contain six digits");
        String duration = ttlSeconds % 60 == 0 ? ttlSeconds / 60 + "분" : ttlSeconds + "초";
        return new BrandedEmailContent(subject + " 코드", subject + "을 위한 인증 코드를 확인해 주세요.",
                category, title, introduction, details,
                new BrandedEmailContent.Highlight("인증 코드", code,
                        "발급 요청 시점부터 최대 " + duration + " 동안 유효해요. 남은 시간은 인증 화면에서 확인해 주세요."),
                null, purposeNotice + " 코드를 다른 사람에게 알려주지 마세요. 본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
    }
}
