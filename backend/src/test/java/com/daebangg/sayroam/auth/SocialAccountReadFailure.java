package com.daebangg.sayroam.auth;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.Callable;

final class SocialAccountReadFailure {
    private SocialAccountReadFailure() {
    }

    static <T> T during(JdbcTemplate jdbc, Callable<T> action) throws Exception {
        jdbc.execute("RENAME TABLE user_social_account TO test_saved_social_accounts");
        try {
            jdbc.execute("""
                    CREATE FUNCTION test_fail_social_read() RETURNS VARCHAR(100) DETERMINISTIC
                    BEGIN
                      SIGNAL SQLSTATE '40001' SET MYSQL_ERRNO = 1213, MESSAGE_TEXT = 'test social read failure';
                      RETURN '';
                    END
                    """);
            jdbc.execute("""
                    CREATE VIEW user_social_account AS
                    SELECT id, user_id, provider, test_fail_social_read() AS provider_user_id,
                           email, last_login_at, created_at, updated_at
                    FROM test_saved_social_accounts
                    """);
            return action.call();
        } finally {
            jdbc.execute("DROP VIEW IF EXISTS user_social_account");
            jdbc.execute("DROP FUNCTION IF EXISTS test_fail_social_read");
            jdbc.execute("RENAME TABLE test_saved_social_accounts TO user_social_account");
        }
    }
}
