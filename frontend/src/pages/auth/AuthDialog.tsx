import { useSocialLogin } from "@/hooks/api/auth/useSocialLogin";
import { useState } from "react";
import { LoginModal } from "@/components/features/auth/LoginModal";
import { EmailFlowModal } from "@/components/features/auth/EmailFlowModal";
import { useEmailFlow, type EmailFlowMode } from "@/hooks/api/auth/useEmailFlow";
import type { AuthMessage, LoginInput } from "@/hooks/auth.types";

type AuthDialogProps = {
    onCancelSocial: () => void;
    onClose: () => void;
    onSubmit: (input: LoginInput) => Promise<boolean>;
    busy: boolean;
    message: AuthMessage | null;
    retryAt?: number;
    remainingAttempts?: number;
    onRetryProfile: () => void;
    onComplete: (mode: EmailFlowMode) => void;
    onEmailSent: () => () => void;
};

function EmailDialog({
    mode,
    onClose,
    onLogin,
    onComplete,
    onEmailSent,
}: {
    mode: EmailFlowMode;
    onClose: () => void;
    onLogin: () => void;
    onComplete: () => void;
    onEmailSent: () => () => void;
}) {
    const flow = useEmailFlow(mode);
    return (
        <EmailFlowModal
            mode={mode}
            busy={flow.busy}
            sending={flow.sending}
            sent={flow.sent}
            verified={flow.verified}
            expiresIn={flow.expiresIn}
            resendIn={flow.resendIn}
            message={flow.message}
            messageTarget={flow.messageTarget}
            onEmailChange={flow.invalidate}
            onRequest={async (email) => {
                const dismissNotice = onEmailSent();
                const success = await flow.requestCode(email);
                if (!success) dismissNotice();
                return success;
            }}
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
    const social = useSocialLogin(!props.busy);
    const [mode, setMode] = useState<"login" | EmailFlowMode>("login");
    if (mode !== "login")
        return (
            <EmailDialog
                key={mode}
                mode={mode}
                onEmailSent={props.onEmailSent}
                onClose={props.onClose}
                onLogin={() => setMode("login")}
                onComplete={() => {
                    setMode("login");
                    props.onComplete(mode);
                }}
            />
        );
    return (
        <LoginModal
            {...props}
            open
            providers={social.providers}
            onSocialLogin={(provider) => {
                props.onCancelSocial();
                return social.start(provider, props.message === "socialEmailRequired");
            }}
            busy={props.busy || social.busy}
            message={
                (social.message ?? props.message) === "socialCancelled"
                    ? null
                    : (social.message ?? props.message)
            }
            onSubmit={(input) => {
                social.clearMessage();
                return props.onSubmit(input);
            }}
            onMode={(mode) => {
                social.clearMessage();
                setMode(mode);
            }}
        />
    );
}
