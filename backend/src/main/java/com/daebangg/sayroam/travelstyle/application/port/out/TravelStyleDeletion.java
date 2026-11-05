package com.daebangg.sayroam.travelstyle.application.port.out;

public interface TravelStyleDeletion {
    /** 기존 탈퇴 트랜잭션에 참여해 개인 응답만 삭제한다. 공용 질문 항목은 보존한다. */
    void deleteForUser(long userId);
}
