import { pagePaths } from "@/routing/paths";

export const appNavigation = [
    { to: pagePaths.plaza, icon: "home", label: "navigation.plaza" },
    { to: pagePaths.trips, icon: "trips", label: "navigation.trips" },
    { to: pagePaths.matching, icon: "matching", label: "navigation.matching" },
    { to: pagePaths.rooms, icon: "rooms", label: "navigation.rooms" },
] as const;

export const tripPreviews = [
    {
        id: "swiss",
        image: "/images/alpine-dawn.png",
        title: "trips.swiss",
        startDate: "2026-02-10",
        endDate: "2026-02-13",
    },
    {
        id: "italy",
        image: "/images/amalfi-coast.png",
        title: "trips.italy",
        startDate: "2026-03-01",
        endDate: "2026-03-08",
    },
] as const;

export const matchingPreviews = [
    {
        id: "italy",
        image: "/images/amalfi-coast.png",
        title: "matching.italy",
        startDate: "2026-03-01",
        endDate: "2026-03-08",
        participants: 2,
        capacity: 4,
    },
    {
        id: "swiss",
        image: "/images/alpine-dawn.png",
        title: "matching.swiss",
        startDate: "2026-02-10",
        endDate: "2026-02-13",
        participants: 3,
        capacity: 6,
    },
] as const;

export const roomPreviews = [
    {
        id: "swiss",
        image: "/images/alpine-dawn.png",
        title: "rooms.swiss",
        message: "rooms.swissMessage",
    },
    {
        id: "italy",
        image: "/images/amalfi-coast.png",
        title: "rooms.italy",
        message: "rooms.italyMessage",
    },
] as const;
