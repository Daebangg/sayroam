package com.daebangg.sayroam.auth.domain;

/** 가입용 증명과 구분되는 계정 관리 작업. 다른 목적의 증명으로 대체할 수 없다. */
public enum AccountVerificationPurpose {
    PASSWORD_CHANGE,
    ACCOUNT_DELETION
}
