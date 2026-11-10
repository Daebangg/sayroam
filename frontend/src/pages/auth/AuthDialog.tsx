import type { PendingSocialSignup, SocialSignupInput } from "@/hooks/auth.types";
import { useCountdown } from "@/hooks/useCountdown";
import { useSocialLogin } from "@/hooks/api/auth/useSocialLogin";
import { useState } from "react";
import { LoginModal } from "@/components/features/auth/LoginModal";
import { EmailFlowModal } from "@/components/features/auth/EmailFlowModal";
import { useEmailFlow, type EmailFlowMode } from "@/hooks/api/auth/useEmailFlow";
import type { AuthMessage, LoginInput } from "@/hooks/auth.types";

type AuthDialogProps = {
    socialSignup?: PendingSocialSignup;
    onSocialComplete: (input: SocialSignupInput) => Promise<boolean>;
    onCancelSocial: () => void;
    onClose: () => void;
    onSubmit: (input: LoginInput) => Promise<boolean>;
    busy: boolean;
    message: AuthMessage | null;
    retryAt?: number;
    remainingAttempts?: number;
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

function SocialSignupDialog({
    signup,
    onSubmit,
    onLogin,
    onClose,
    busy,
}: {
    signup: PendingSocialSignup;
    onSubmit: AuthDialogProps["onSocialComplete"];
    onLogin: () => void;
    onClose: () => void;
    busy: boolean;
}) {
    const flow = useEmailFlow("signup");
    const remaining = useCountdown(signup.expiresAt);
    return (
        <EmailFlowModal
            mode="social"
            emailVerificationRequired={signup.emailVerificationRequired}
            suggestedEmail={signup.suggestedEmail ?? null}
            signupExpiresIn={remaining}
            busy={busy || flow.busy}
            sent={flow.sent}
            verified={flow.verified}
            expiresIn={flow.expiresIn}
            resendIn={flow.resendIn}
            message={flow.message}
            onEmailChange={flow.invalidate}
            onRequest={flow.requestCode}
            onVerify={flow.verifyCode}
            onLogin={onLogin}
            onClose={() => {
                if (!busy && !flow.busy) onClose();
            }}
            onComplete={async (input) => {
                const proof = flow.getSignupProof();
                if (signup.emailVerificationRequired && !proof) return false;
                return onSubmit({
                    nickname: input.nickname,
                    ...(signup.emailVerificationRequired && proof ? proof : {}),
                });
            }}
        />
    );
}

export function AuthDialog(props: AuthDialogProps) {
    const social = useSocialLogin(!props.busy);
    const [mode, setMode] = useState<"login" | EmailFlowMode>("login");
    if (props.socialSignup)
        return (
            <SocialSignupDialog
                signup={props.socialSignup}
                onSubmit={props.onSocialComplete}
                busy={props.busy}
                onClose={props.onClose}
                onLogin={() => {
                    props.onCancelSocial();
                    setMode("login");
                }}
            />
        );
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
    return (
        <LoginModal
            {...props}
            open
            providers={social.providers}
            onSocialLogin={social.start}
            busy={props.busy || social.busy}
            message={social.message ?? props.message}
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
