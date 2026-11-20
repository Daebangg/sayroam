import { useAccountAction } from "@/hooks/api/auth/useAccountAction";
import { AccountActionDialog } from "@/components/features/settings/AccountDialogs";
import type { AccountPurpose, AccountResult } from "@/hooks/account.types";

type AccountActionFlowProps = {
    purpose: AccountPurpose;
    email: string | null;
    onClose: () => void;
    onEmailSent: () => () => void;
    onResult: (result: AccountResult) => Promise<void>;
};
export function AccountActionFlow({
    purpose,
    email,
    onClose,
    onEmailSent,
    onResult,
}: AccountActionFlowProps) {
    const flow = useAccountAction(purpose, onResult);
    return (
        <AccountActionDialog
            {...flow}
            purpose={purpose}
            email={email}
            onClose={onClose}
            onRequest={async () => {
                const dismissNotice = onEmailSent();
                const success = await flow.requestCode();
                if (!success) dismissNotice();
                return success;
            }}
            onVerify={flow.verifyCode}
            onComplete={flow.complete}
        />
    );
}
