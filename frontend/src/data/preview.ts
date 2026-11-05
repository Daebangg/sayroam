export type PreviewView = "plaza" | "trips" | "matching" | "rooms" | "brand";
export type PreviewTheme = "light" | "dark";

export const previewNavigation = [
    { view: "plaza", icon: "home", label: "navigation.plaza" },
    { view: "trips", icon: "trips", label: "navigation.trips" },
    { view: "matching", icon: "matching", label: "navigation.matching" },
    { view: "rooms", icon: "rooms", label: "navigation.rooms" },
] as const;

export const tripPreviews = [
    {
        id: "swiss",
        image: "/images/alpine-dawn.png",
        title: "trips.swiss",
        dates: "trips.swissDates",
    },
    {
        id: "italy",
        image: "/images/amalfi-coast.png",
        title: "trips.italy",
        dates: "trips.italyDates",
    },
] as const;

export const matchingPreviews = [
    {
        id: "italy",
        image: "/images/amalfi-coast.png",
        title: "matching.italy",
        dates: "matching.italyDates",
        people: "2 / 4",
    },
    {
        id: "swiss",
        image: "/images/alpine-dawn.png",
        title: "matching.swiss",
        dates: "matching.swissDates",
        people: "3 / 6",
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
