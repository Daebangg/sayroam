import { createBrowserRouter } from "react-router";
import App from "@/App";
import {
    NotFoundPage,
    RootErrorPage,
    RootLoadingPage,
    RouteErrorPage,
} from "@/pages/status/StatusPage";
import { pagePaths, legacyPageUrl } from "./paths";

const legacy = legacyPageUrl(new URL(window.location.href));
if (legacy) window.history.replaceState(window.history.state, "", legacy);

export const router = createBrowserRouter([
    {
        Component: App,
        ErrorBoundary: RootErrorPage,
        HydrateFallback: RootLoadingPage,
        children: [
            {
                ErrorBoundary: RouteErrorPage,
                children: [
                    {
                        path: pagePaths.plaza,
                        lazy: async () => ({
                            Component: (await import("@/pages/home/HomePage")).HomePage,
                        }),
                    },
                    {
                        path: pagePaths.trips,
                        lazy: async () => ({
                            Component: (await import("@/pages/trips/TripsPage")).TripsPage,
                        }),
                    },
                    {
                        path: pagePaths.matching,
                        lazy: async () => ({
                            Component: (await import("@/pages/matching/MatchingPage")).MatchingPage,
                        }),
                    },
                    {
                        path: pagePaths.rooms,
                        lazy: async () => ({
                            Component: (await import("@/pages/rooms/RoomsPage")).RoomsPage,
                        }),
                    },
                    {
                        path: pagePaths.brand,
                        lazy: async () => ({
                            Component: (await import("@/pages/brand/BrandPage")).BrandPage,
                        }),
                    },
                    { path: "*", Component: NotFoundPage },
                ],
            },
        ],
    },
]);
