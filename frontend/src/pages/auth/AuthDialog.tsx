import { useState } from "react";
import { LoginModal } from "@/components/features/auth/LoginModal";
import { EmailFlowModal } from "@/components/features/auth/EmailFlowModal";
import { useEmailFlow, type EmailFlowMode } from "@/hooks/api/auth/useEmailFlow";
import type { AuthMessage, LoginInput } from "@/hooks/auth.types";

type AuthDialogProps = {
    onClose: () => void;
    onSubmit: (input: LoginInput) => Promise<boolean>;
    busy: boolean;
    message: AuthMessage | null;
    onRetryProfile: () => void;
    onComplete: (mode: EmailFlowMode) => void;
};

function EmailDialog({
    mode,
    onClose,
    onLogin,
    onComplete,
}: {
    mode: EmailFlowMode;
    onClose: () => void;
    onLogin: () => void;
    onComplete: () => void;
}) {
    const flow = useEmailFlow(mode);
    return (
        <EmailFlowModal
            mode={mode}
            busy={flow.busy}
            sent={flow.sent}
            verified={flow.verified}
            expiresIn={flow.expiresIn}
            resendIn={flow.resendIn}
            message={flow.message}
            onEmailChange={flow.invalidate}
            onRequest={flow.requestCode}
            onVerify={flow.verifyCode}
            onClose={onClose}
            onLogin={onLogin}
            onComplete={async (input) => {
                const success = await flow.complete(input);
                if (success) onComplete();
                return success;
            }}
        />
    );
}

export function AuthDialog(props: AuthDialogProps) {
    const [mode, setMode] = useState<"login" | EmailFlowMode>("login");
    if (mode !== "login")
        return (
            <EmailDialog
                key={mode}
                mode={mode}
                onClose={props.onClose}
                onLogin={() => setMode("login")}
                onComplete={() => {
                    setMode("login");
                    props.onComplete(mode);
                }}
            />
        );
    return <LoginModal {...props} open onMode={setMode} />;
}
