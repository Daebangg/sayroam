import type { components } from "@/api/generated/schema";

export type MyTrip = components["schemas"]["TripSummaryResponse"];
export type TripPlace = components["schemas"]["TripPlaceResponse"];
export type SearchedPlace = components["schemas"]["PlaceSearchItemResponse"];
/** 지도에서 고른 위치. 이름은 회원이 직접 입력한다. */
export type PickedLocation = { latitude: number; longitude: number };
