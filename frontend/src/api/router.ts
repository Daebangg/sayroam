import type { FetchOptions, FetchResponse } from "openapi-fetch";
import { ApiError } from "./apiError.ts";
import type { apiRoutes, operations } from "./generated/schema.ts";
import { request, type RequestOptions } from "./http.ts";

export type OperationId = keyof typeof apiRoutes & keyof operations;
type Route = {
    readonly method: "get" | "post" | "put" | "patch" | "delete" | "head" | "options" | "trace";
    readonly path: string;
    readonly auth: "none" | "required";
    readonly csrf: boolean;
    readonly responses: Readonly<Record<string, boolean>>;
};
type BodyFlags<K extends OperationId> =
    (typeof apiRoutes)[K]["responses"][keyof (typeof apiRoutes)[K]["responses"]];
type SuccessData<K extends OperationId> = Extract<
    FetchResponse<
        operations[K],
        Record<string, never>,
        "application/json" | `${string}/${string}+json`
    >,
    { data: unknown }
>["data"];
export type ApiResponse<K extends OperationId> =
    | (true extends BodyFlags<K> ? SuccessData<K> : never)
    | (false extends BodyFlags<K> ? undefined : never);

export type ApiCallOptions<K extends OperationId> = Pick<
    FetchOptions<operations[K]>,
    "body" | "params" | "headers" | "signal" | "cache"
>;
type RequiredOperation = {
    [K in OperationId]: operations[K] extends
        | { requestBody: unknown }
        | {
              parameters:
                  | { path: unknown }
                  | { query: unknown }
                  | { header: unknown }
                  | { cookie: unknown };
          }
        ? K
        : never;
}[OperationId];
type CallArguments<K extends OperationId> = K extends RequiredOperation
    ? [options: NoInfer<ApiCallOptions<K>>]
    : [options?: NoInfer<ApiCallOptions<K>>];

type EndpointExtension<K extends OperationId> = {
    cookieAction?: RequestOptions["cookieAction"];
    parseResponse?: (value: unknown, status: number) => ApiResponse<K> | Promise<ApiResponse<K>>;
    wrap?: (
        next: () => Promise<ApiResponse<K>>,
        context: { operationId: K },
    ) => Promise<ApiResponse<K>>;
};
type RouterOptions = {
    extensions?: { [K in OperationId]?: EndpointExtension<K> };
    recover?: RequestOptions["recover"];
};
type ApiRouter = {
    call<K extends OperationId>(operationId: K, ...args: CallArguments<K>): Promise<ApiResponse<K>>;
};

// operationId와 입력의 관계는 공개 call 타입 및 생성된 경로표가 보장한다.
// 이 경계에서만 이종 경로를 openapi-fetch의 동적 request에 연결한다.
type Dispatch = (
    method: Route["method"],
    path: string,
    options: unknown,
) => Promise<{ data?: unknown; response: Response }>;

export function createApiRouter(
    routes: Readonly<Record<OperationId, Route>>,
    config: RouterOptions = {},
): ApiRouter {
    async function call<K extends OperationId>(
        operationId: K,
        ...args: CallArguments<K>
    ): Promise<ApiResponse<K>> {
        if (!Object.hasOwn(routes, operationId)) throw new ApiError("invalid-response");
        const route = routes[operationId];
        const input = args[0];
        const extension = config.extensions?.[operationId];
        return request(
            async (client) => {
                const next = async (): Promise<ApiResponse<K>> => {
                    const dispatch = client.request as Dispatch;
                    // 호출별 fetch·baseUrl·정책 우회를 허용하지 않고 계약 입력만 전달한다.
                    const { data, response } = await dispatch(route.method, route.path, {
                        body: input?.body,
                        params: input?.params,
                        headers: input?.headers,
                        cache: input?.cache,
                        parseAs: "text",
                    });
                    const hasBody =
                        route.responses[String(response.status)] ?? route.responses["2XX"];
                    const value: unknown = hasBody ? JSON.parse(String(data)) : undefined;
                    if (extension?.parseResponse)
                        return extension.parseResponse(value, response.status);
                    // 검증기를 등록하지 않은 응답의 정적 타입은 생성 계약을 따른다.
                    return value as ApiResponse<K>;
                };
                return extension?.wrap ? extension.wrap(next, { operationId }) : next();
            },
            {
                auth: route.auth,
                csrf: route.csrf,
                responses: route.responses,
                ...(input?.signal === undefined ? {} : { signal: input.signal }),
                ...(extension?.cookieAction === undefined
                    ? {}
                    : { cookieAction: extension.cookieAction }),
                ...(config.recover === undefined ? {} : { recover: config.recover }),
            },
        );
    }
    return Object.freeze({ call });
}
