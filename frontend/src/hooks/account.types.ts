import type { components } from "../api/generated/schema.ts";

export type AccountPurpose = components["schemas"]["AccountVerificationRequest"]["purpose"];
export type LoginMethods = components["schemas"]["UserLoginMethodsResponse"];
export type AccountResult = {
    purpose: AccountPurpose;
    outcome: "complete" | "uncertain" | "reauthenticate";
};
export type AccountMessage =
    | "codeInvalid"
    | "expired"
    | "limited"
    | "delivery"
    | "unavailable"
    | "security"
    | "conflict"
    | "passwordNotAllowed"
    | "verificationNotAllowed"
    | "uncertain"
    | "invalidPassword";
export type AccountFlowView = {
    stage: "request" | "code" | "verified";
    busy: boolean;
    sending: boolean;
    message: AccountMessage | null;
    expiresAt: number;
    resendAt: number;
};

export type NicknameEditor = {
    busy: boolean;
    message: "invalid" | "unavailable" | "uncertain" | null;
    save: (nickname: string) => Promise<boolean>;
    reset: () => void;
};

export type ProfileDetailsUpdate = Pick<
    components["schemas"]["UpdateUserProfileRequest"],
    "gender" | "birthDate"
>;
export type ProfileEditor<T> = {
    busy: boolean;
    message: "invalid" | "uncertain" | "unavailable" | null;
    save: (input: T) => Promise<boolean>;
    reset: () => void;
};

export type ProfilePhoto = {
    previewUrl: string | null;
    cropSource: string | null;
    closeCrop: () => void;
    applyPhoto: (dataUrl: string) => Promise<void>;
    changed: boolean;
    busy: boolean;
    message: "invalid" | "uncertain" | "unavailable" | "fileInvalid" | "fileTooLarge" | null;
    selectFile: (file: File) => Promise<void>;
    selectPreset: (url: string) => void;
    cancel: () => void;
    save: () => Promise<void>;
};
