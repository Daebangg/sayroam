import { Navigate, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { Button, ButtonLink, Card, Icon } from "@/components/ui";
import { useJoinRoom } from "@/hooks/api/rooms/useJoinRoom";
import { invitationCode, pagePaths, tripDetailPath } from "@/routing/paths";

// 회원 전용 경로라 App의 MemberGate가 로그인 전 진입을 막고, 로그인 후 같은 주소(해시 포함)로 돌아온다.
export function JoinRoomPage() {
    const code = invitationCode(useLocation().hash);
    // 같은 화면에서 다른 초대 링크를 열면 이전 코드의 참여 결과를 남기지 않고 처음부터 시작한다.
    return <JoinRoom key={code ?? ""} code={code} />;
}

function JoinRoom({ code }: { code: string | null }) {
    const { t } = useTranslation();
    const { status, tripId, join } = useJoinRoom();
    // 참여한 여행으로 바로 보낸다. 뒤로 가기로 참여 화면에 돌아오지 않도록 기록을 바꾼다.
    if (status === "joined" && tripId !== null)
        return <Navigate to={tripDetailPath(tripId)} replace />;
    const result =
        code === null
            ? { title: t("join.missingTitle"), description: t("join.missingDescription") }
            : status === "joined"
              ? { title: t("join.joinedTitle"), description: t("join.joinedDescription") }
              : status === "notFound"
                ? { title: t("join.notFoundTitle"), description: t("join.notFoundDescription") }
                : status === "expired"
                  ? { title: t("join.expiredTitle"), description: t("join.expiredDescription") }
                  : { title: t("join.heading"), description: t("join.description") };
    const canJoin =
        code !== null && (status === "idle" || status === "joining" || status === "failed");
    return (
        <main id="main-content" tabIndex={-1} className="workspace-page join-room">
            <Card className="join-room__card">
                <Icon name="matching" />
                <h1>{result.title}</h1>
                <p>{result.description}</p>
                {status === "failed" && (
                    <p className="join-room__error" role="alert">
                        {t("join.failed")}
                    </p>
                )}
                {canJoin ? (
                    <Button loading={status === "joining"} onClick={() => void join(code)}>
                        {t("join.accept")}
                    </Button>
                ) : (
                    <ButtonLink
                        to={pagePaths.trips}
                        variant={status === "joined" ? "brand" : "outline"}
                    >
                        {t("join.goTrips")}
                    </ButtonLink>
                )}
            </Card>
        </main>
    );
}
