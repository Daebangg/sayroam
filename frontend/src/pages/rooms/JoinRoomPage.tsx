import { useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { Button, ButtonLink, Card, Icon } from "@/components/ui";
import { useJoinRoom } from "@/hooks/api/rooms/useJoinRoom";
import { invitationCode, pagePaths } from "@/routing/paths";

// 회원 전용 경로라 App의 MemberGate가 로그인 전 진입을 막고, 로그인 후 같은 주소(해시 포함)로 돌아온다.
export function JoinRoomPage() {
    const { t } = useTranslation();
    const code = invitationCode(useLocation().hash);
    const { status, join } = useJoinRoom();
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
