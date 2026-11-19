import type { components, operations } from "@/api/generated/schema";

export type CandidateQuery = operations["previewCompanionCandidates"]["parameters"]["query"];
export type CandidateResult = components["schemas"]["CompanionCandidateListResponse"];
export type MatchingDestination = components["schemas"]["DestinationSummary"];
export type MatchingRegistration = components["schemas"]["CompanionSearchResponse"];
