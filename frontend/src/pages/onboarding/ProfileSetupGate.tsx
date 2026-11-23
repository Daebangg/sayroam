import { useEffect, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui";
import { useTravelStyle } from "@/hooks/api/profile/useTravelStyle";
import { useProfileEditor } from "@/hooks/api/auth/useProfileEditor";
import type { PageContext } from "@/routing/context";
import { ProfileSetupPage } from "./ProfileSetupPage";

type ProfileSetupGateProps = {
    member: NonNullable<PageContext["member"]>;
    onUpdate: PageContext["onUpdateProfileDetails"];
    children: ReactNode;
    onActivity: (active: boolean) => void;
};
export function ProfileSetupGate({
    member,
    onUpdate,
    children,
    onActivity,
}: ProfileSetupGateProps) {
    const { t } = useTranslation();
    // 열람 기록이 저장돼도 지금 열린 안내는 계속 진행할 수 있다.
    const [closed, setClosed] = useState(member.profileSetupSeen);
    const style = useTravelStyle(closed ? undefined : member.userId);
    const details = useProfileEditor(onUpdate);
    const [visitSaveFailed, setVisitSaveFailed] = useState(false);
    const [visitAttempt, setVisitAttempt] = useState(0);
    const active = !closed;
    const showingSetup = active && (style.values !== null || style.failed);
    useEffect(() => {
        if (!showingSetup || member.profileSetupSeen) return;
        const controller = new AbortController();
        void onUpdate({ profileSetupSeen: true }, controller.signal).catch(() => {
            if (!controller.signal.aborted) setVisitSaveFailed(true);
        });
        return () => controller.abort();
    }, [showingSetup, member.profileSetupSeen, onUpdate, visitAttempt]);
    useEffect(() => {
        onActivity(active);
        return () => onActivity(false);
    }, [active, onActivity]);
    if (closed) return children;
    return (
        <ProfileSetupPage
            member={member}
            savedStyle={style.values}
            styleFailed={style.failed}
            onReloadStyle={style.reload}
            busy={details.busy || style.busy || (showingSetup && !member.profileSetupSeen)}
            homePath="/"
            onLeave={() => setClosed(true)}
            visitError={
                visitSaveFailed && (
                    <div role="alert">
                        <p>{t("profileSetup.visitError")}</p>
                        <Button
                            onClick={() => {
                                setVisitSaveFailed(false);
                                setVisitAttempt((attempt) => attempt + 1);
                            }}
                        >
                            {t("profileSetup.retry")}
                        </Button>
                    </div>
                )
            }
            error={
                details.message
                    ? t(`settings.detailsMessages.${details.message}`)
                    : style.failed && style.values !== null
                      ? t("profileSetup.styleError")
                      : null
            }
            onSave={async (gender, birthDate, values) => {
                const input: Parameters<typeof details.save>[0] = {};
                if (member.gender !== gender) input.gender = gender;
                if (member.birthDate !== birthDate) input.birthDate = birthDate;
                if (Object.keys(input).length && !(await details.save(input))) return false;
                return style.save(values);
            }}
        />
    );
}
