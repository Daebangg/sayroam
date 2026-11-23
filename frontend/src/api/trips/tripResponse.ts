import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";

type TripSummary = components["schemas"]["TripSummaryResponse"];

function record(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function id(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value >= 1;
}

function date(value: unknown): value is string {
    return (
        typeof value === "string" &&
        /^\d{4}-\d{2}-\d{2}$/.test(value) &&
        Number.isFinite(Date.parse(value))
    );
}

function trip(value: unknown): value is TripSummary {
    return (
        record(value) &&
        id(value.tripId) &&
        (value.title === null || typeof value.title === "string") &&
        date(value.startDate) &&
        date(value.endDate) &&
        value.startDate <= value.endDate &&
        (value.roomId === null || id(value.roomId))
    );
}

export function validateTripList(
    value: unknown,
    status: number,
): components["schemas"]["TripListResponse"] {
    if (!record(value) || !Array.isArray(value.trips) || !value.trips.every(trip))
        throw new ApiError("invalid-response", { status });
    return { trips: value.trips };
}

export function validateCreatedTrip(
    value: unknown,
    status: number,
): components["schemas"]["CreateTripResponse"] {
    if (!record(value) || !id(value.tripId)) throw new ApiError("invalid-response", { status });
    return { tripId: value.tripId };
}

const categories = new Set([
    "ATTRACTION",
    "CULTURE",
    "ACTIVITY",
    "LODGING",
    "SHOPPING",
    "RESTAURANT",
]);

function text(value: unknown): value is string {
    return typeof value === "string";
}

function optionalText(value: unknown): value is string | null {
    return value === null || text(value);
}

function coordinate(value: unknown, limit: number): value is number {
    return typeof value === "number" && Number.isFinite(value) && Math.abs(value) <= limit;
}

function tripPlace(value: unknown): value is components["schemas"]["TripPlaceResponse"] {
    return (
        record(value) &&
        id(value.tripPlaceId) &&
        (value.source === "TOUR" || value.source === "CUSTOM") &&
        text(value.name) &&
        (value.category === null || categories.has(String(value.category))) &&
        optionalText(value.address) &&
        coordinate(value.latitude, 90) &&
        coordinate(value.longitude, 180) &&
        (value.day === null || id(value.day)) &&
        optionalText(value.memo) &&
        id(value.addedById)
    );
}

function searchedPlace(value: unknown): value is components["schemas"]["PlaceSearchItemResponse"] {
    return (
        record(value) &&
        text(value.sourceId) &&
        value.sourceId.length > 0 &&
        text(value.name) &&
        categories.has(String(value.category)) &&
        optionalText(value.address) &&
        coordinate(value.latitude, 90) &&
        coordinate(value.longitude, 180)
    );
}

export function validateTrip(value: unknown, status: number): TripSummary {
    if (!trip(value)) throw new ApiError("invalid-response", { status });
    return value;
}

export function validateTripPlaces(
    value: unknown,
    status: number,
): components["schemas"]["TripPlaceListResponse"] {
    if (!record(value) || !Array.isArray(value.places) || !value.places.every(tripPlace))
        throw new ApiError("invalid-response", { status });
    return { places: value.places };
}

export function validateAddedPlace(
    value: unknown,
    status: number,
): components["schemas"]["AddTripPlaceResponse"] {
    if (!record(value) || !id(value.tripPlaceId))
        throw new ApiError("invalid-response", { status });
    return { tripPlaceId: value.tripPlaceId };
}

export function validatePlaceSearch(
    value: unknown,
    status: number,
): components["schemas"]["PlaceSearchResponse"] {
    if (
        !record(value) ||
        typeof value.hasNext !== "boolean" ||
        !Array.isArray(value.places) ||
        !value.places.every(searchedPlace)
    )
        throw new ApiError("invalid-response", { status });
    return { hasNext: value.hasNext, places: value.places };
}

export function validateCreatedRoom(
    value: unknown,
    status: number,
): components["schemas"]["CreateTripRoomResponse"] {
    if (!record(value) || !id(value.roomId)) throw new ApiError("invalid-response", { status });
    return { roomId: value.roomId };
}

export function validateInvitation(
    value: unknown,
    status: number,
): components["schemas"]["CreateRoomInvitationResponse"] {
    if (!record(value) || !text(value.code) || !/^[A-Za-z0-9_-]{1,100}$/.test(value.code))
        throw new ApiError("invalid-response", { status });
    return { code: value.code };
}

export function validateJoinedRoom(
    value: unknown,
    status: number,
): components["schemas"]["JoinRoomResponse"] {
    if (!record(value) || !id(value.roomId)) throw new ApiError("invalid-response", { status });
    return { roomId: value.roomId };
}
