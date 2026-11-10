import { useEffect, useState, useSyncExternalStore } from "react";
import { createAccountController } from "./accountController.ts";
import { useCountdown } from "../../useCountdown";
import type { AccountFlowView, AccountPurpose, AccountResult } from "../../account.types.ts";

export function useAccountAction(
    purpose: AccountPurpose,
    onResult: (result: AccountResult) => void,
) {
    const [controller] = useState(() => createAccountController(purpose, onResult));
    const state: AccountFlowView = useSyncExternalStore(
        controller.subscribe,
        controller.getSnapshot,
    );
    useEffect(() => () => controller.dispose(), [controller]);
    return {
        ...state,
        expiresIn: useCountdown(state.expiresAt),
        resendIn: useCountdown(state.resendAt),
        requestCode: controller.requestCode,
        verifyCode: controller.verifyCode,
        complete: controller.complete,
    };
}
