import { useLocation } from "react-router";
import { getMockupScreen, mockupUrl } from "@/routing/mockups";

const paths = { home: "/", trips: "/trips", matching: "/matching", rooms: "/rooms" } as const;

export function usePreviewNavigation(): {
    isPreview: boolean;
    pathFor: (screen: keyof typeof paths) => string;
} {
    const { pathname, search } = useLocation();
    const isPreview = getMockupScreen(pathname, search) !== null;
    return { isPreview, pathFor: (screen) => (isPreview ? mockupUrl(screen) : paths[screen]) };
}
