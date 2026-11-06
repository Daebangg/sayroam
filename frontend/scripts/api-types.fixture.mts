import type { api } from "../src/api/api.ts";
import type { components } from "../src/api/generated/schema.ts";

// 타입 검사 전용. 실제 네트워크 호출은 실행하지 않는다.
export function verifyApiTypes(client: typeof api): void {
    const user: Promise<components["schemas"]["CurrentUserResponse"]> = client.call("me");
    const empty: Promise<undefined> = client.call("logout");
    void [user, empty];
    void client.call("login", { body: { email: "user@example.test", password: "password" } });
    void client.call("refresh");
    void client.call("create", { body: { name: "여행" } });
    void client.call("me").then((profile) => {
        const id: number = profile.userId;
        const role: "USER" | "ADMIN" = profile.role;
        void [id, role];
        // @ts-expect-error 응답에 없는 필드
        void profile.password;
    });
    void client.call("create", { body: { name: "여행" } }).then((room) => {
        const id: number = room.roomId;
        void id;
    });
    // @ts-expect-error 명세에 없는 operationId
    void client.call("unknownOperation");
    // @ts-expect-error 로그인 본문은 필수
    void client.call("login");
    // @ts-expect-error 로그인 DTO의 필수 필드 누락
    void client.call("login", { body: { email: "user@example.test" } });
    // @ts-expect-error 다른 API 본문으로 operationId의 타입을 넓힐 수 없음
    void client.call("login", { body: { name: "여행" } });
    // @ts-expect-error 본문 없는 API에는 body를 전달할 수 없음
    void client.call("me", { body: {} });
    // @ts-expect-error 인증 요구는 생성 명세에서 결정
    void client.call("me", { auth: "none" });
    // @ts-expect-error 실제 fetch를 바꿔 공통 정책을 우회할 수 없음
    void client.call("me", { fetch: globalThis.fetch });
}
