import { useEffect, useState, useSyncExternalStore } from "react";
import { createNotificationController } from "./notificationController";
import type { NotificationState } from "../../notification.types";

type NotificationSession = NotificationState &
    Pick<
        ReturnType<typeof createNotificationController>,
        "setOpen" | "setUnreadOnly" | "reload" | "loadMore" | "setRead" | "readAll" | "delete"
    >;

export function useNotifications(enabled: boolean): NotificationSession {
    const [controller] = useState(createNotificationController);
    const state = useSyncExternalStore(controller.subscribe, controller.getSnapshot);
    useEffect(() => {
        if (!enabled) {
            controller.disconnect();
            return;
        }
        const available = (): boolean => !document.hidden && navigator.onLine;
        const disconnect = controller.connect(available());
        const resume = (): void => controller.setAvailable(available());
        document.addEventListener("visibilitychange", resume);
        window.addEventListener("online", resume);
        window.addEventListener("offline", resume);
        window.addEventListener("focus", resume);
        return () => {
            document.removeEventListener("visibilitychange", resume);
            window.removeEventListener("online", resume);
            window.removeEventListener("offline", resume);
            window.removeEventListener("focus", resume);
            disconnect();
        };
    }, [controller, enabled]);
    return {
        ...state,
        setOpen: controller.setOpen,
        setUnreadOnly: controller.setUnreadOnly,
        reload: controller.reload,
        loadMore: controller.loadMore,
        setRead: controller.setRead,
        readAll: controller.readAll,
        delete: controller.delete,
    };
}
