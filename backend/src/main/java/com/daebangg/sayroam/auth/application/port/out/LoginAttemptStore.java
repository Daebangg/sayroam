package com.daebangg.sayroam.auth.application.port.out;

import java.time.Duration;

public interface LoginAttemptStore {
    /** 이메일·IP 제한을 함께 확인·갱신하고 화면에 표시할 이메일의 남은 횟수를 반환한다. */
    Evaluation evaluate(String normalizedEmail, String remoteAddress, Outcome outcome);

    record Evaluation(Duration retryAfter, int remainingAttempts) {
        public Evaluation {
            if (retryAfter == null || retryAfter.isNegative() || remainingAttempts < 0) {
                throw new IllegalArgumentException("Invalid login attempt evaluation");
            }
        }
    }

    enum Outcome { CHECK, FAILURE, SUCCESS }
}
