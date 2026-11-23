import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";
import type { RecruitmentAction } from "./RecruitmentActionDialog";

type RecruitmentTextActionProps = {
    action: RecruitmentAction;
    disabled?: boolean;
    onClick: () => void;
};

export function RecruitmentTextAction({ action, disabled, onClick }: RecruitmentTextActionProps) {
    const { t } = useTranslation();
    return (
        <Button
            variant="plain"
            className="matching-text-action recruitment-text-action"
            disabled={disabled}
            onClick={onClick}
        >
            {action === "edit" && <Icon name="edit" />}
            {t(`matchingFlow.${action}`)}
        </Button>
    );
}
