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
