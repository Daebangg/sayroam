import { pagePaths } from "@/routing/paths";

export const appNavigation = [
    { to: pagePaths.plaza, icon: "home", label: "navigation.plaza" },
    { to: pagePaths.trips, icon: "trips", label: "navigation.trips" },
    { to: pagePaths.matching, icon: "matching", label: "navigation.matching" },
    { to: pagePaths.rooms, icon: "rooms", label: "navigation.rooms" },
] as const;

export const tripPreviews = [
    {
        id: "santorini",
        scene: "coast",
        title: "trips.santorini",
        startDate: "2026-12-02",
        endDate: "2026-12-06",
        participants: 3,
        capacity: 6,
        region: "overseas",
        workation: false,
    },
    {
        id: "paris",
        scene: "city",
        title: "trips.paris",
        startDate: "2026-12-10",
        endDate: "2026-12-14",
        participants: 2,
        capacity: 4,
        region: "overseas",
        workation: false,
    },
    {
        id: "bali",
        scene: "island",
        title: "trips.bali",
        startDate: "2026-12-01",
        endDate: "2026-12-14",
        participants: 3,
        capacity: 6,
        region: "overseas",
        workation: true,
    },
    {
        id: "swiss",
        scene: "mountain",
        title: "trips.swiss",
        startDate: "2026-12-05",
        endDate: "2026-12-12",
        participants: 1,
        capacity: 4,
        region: "overseas",
        workation: false,
    },
    {
        id: "jeju",
        scene: "island",
        title: "trips.jeju",
        startDate: "2026-12-18",
        endDate: "2026-12-21",
        participants: 2,
        capacity: 4,
        region: "domestic",
        workation: false,
    },
] as const;
export type PreviewTrip = (typeof tripPreviews)[number];
export const matchingPreviews = tripPreviews.slice(1, 4);

export const roomPreviews = [
    {
        id: "okinawa",
        scene: "island",
        title: "rooms.okinawa",
        message: "rooms.okinawaMessage",
        time: "rooms.justNow",
        unread: 2,
    },
    {
        id: "swiss",
        scene: "mountain",
        title: "rooms.swiss",
        message: "rooms.swissMessage",
        time: "rooms.yesterday",
        unread: 0,
    },
    {
        id: "paris",
        scene: "city",
        title: "rooms.paris",
        message: "rooms.parisMessage",
        time: "rooms.twoDaysAgo",
        unread: 0,
    },
] as const;
export type PreviewRoom = (typeof roomPreviews)[number];
