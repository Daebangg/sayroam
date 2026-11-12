package com.daebangg.sayroam.auth.application.port.out;

public interface AccountPersistenceFailures {
    boolean isUnavailable(RuntimeException failure);
}
