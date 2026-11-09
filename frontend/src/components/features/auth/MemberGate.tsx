import { useTranslation } from "react-i18next";
import { Button, ButtonLink, PageState } from "@/components/ui";

type MemberGateProps = { checking: boolean; unavailable: boolean; onLogin: () => void };

export function MemberGate({ checking, unavailable, onLogin }: MemberGateProps) {
    const { t } = useTranslation();
    return (
        <main id="main-content" tabIndex={-1} className="status-page">
            <PageState
                kind={checking ? "loading" : "error"}
                title={t(
                    checking
                        ? "auth.checking"
                        : unavailable
                          ? "auth.unavailableTitle"
                          : "auth.requiredTitle",
                )}
                description={t(
                    checking
                        ? "auth.checkingDescription"
                        : unavailable
                          ? "auth.unavailableDescription"
                          : "auth.requiredDescription",
                )}
                actions={
                    !checking ? (
                        <>
                            <Button variant="brand" onClick={onLogin}>
                                {t("auth.login")}
                            </Button>
                            <ButtonLink to="/" variant="outline">
                                {t("navigation.home")}
                            </ButtonLink>
                        </>
                    ) : undefined
                }
            />
        </main>
    );
}
