import type { components } from "./generated/schema.ts";

type ApiProblem = components["schemas"]["ApiProblem"];
type FieldError = components["schemas"]["FieldError"];

type ApiErrorKind = "http" | "network" | "aborted" | "invalid-response";

type ApiErrorOptions = {
    status?: number;
    problem?: ApiProblem;
    cause?: unknown;
};

export class ApiError extends Error {
    readonly kind: ApiErrorKind;
    readonly status: number | undefined;
    readonly problem: ApiProblem | undefined;

    constructor(kind: ApiErrorKind, options: ApiErrorOptions = {}) {
        super(kind, { cause: options.cause });
        this.name = "ApiError";
        this.kind = kind;
        this.status = options.status ?? options.problem?.status;
        this.problem = options.problem;
    }
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isFieldErrors(value: unknown): value is FieldError[] {
    return (
        Array.isArray(value) &&
        value.every(
            (item: unknown) =>
                isRecord(item) && typeof item.field === "string" && typeof item.reason === "string",
        )
    );
}

export function toApiProblem(response: Response, value: unknown): ApiProblem {
    const source: Record<string, unknown> = isRecord(value) ? value : {};

    const problem: ApiProblem = {
        status: response.status,
        code: typeof source.code === "string" ? source.code : `HTTP_${response.status}`,
        title: typeof source.title === "string" ? source.title : response.statusText,
        detail: typeof source.detail === "string" ? source.detail : "HTTP request failed",
        instance: typeof source.instance === "string" ? source.instance : response.url,
    };

    if (typeof source.type === "string") problem.type = source.type;
    if (isFieldErrors(source.errors)) problem.errors = source.errors;

    const retryHeader = response.headers.get("Retry-After");
    const headerSeconds =
        retryHeader !== null && /^\d+$/.test(retryHeader) ? Number(retryHeader) : undefined;

    const seconds =
        typeof source.retryAfterSeconds === "number" ? source.retryAfterSeconds : headerSeconds;

    if (seconds !== undefined && Number.isFinite(seconds) && seconds >= 0) {
        problem.retryAfterSeconds = seconds;
    }

    return problem;
}
