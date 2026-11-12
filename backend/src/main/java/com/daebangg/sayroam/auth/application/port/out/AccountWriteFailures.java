package com.daebangg.sayroam.auth.application.port.out;

public interface AccountWriteFailures {
    boolean isUnavailable(RuntimeException failure);
}
