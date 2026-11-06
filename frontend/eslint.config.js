import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import tseslint from "typescript-eslint";
import { defineConfig, globalIgnores } from "eslint/config";

const sharedJsxRestrictions = [
    {
        selector: "CallExpression[callee.name='fetch']",
        message: "API 호출은 src/api/api.ts의 공통 라우터를 거칩니다.",
    },
    {
        selector:
            "CallExpression[callee.object.name=/^(window|globalThis|self)$/][callee.property.name='fetch']",
        message: "API 호출은 src/api/api.ts의 공통 라우터를 거칩니다.",
    },
    {
        selector: "NewExpression[callee.name='XMLHttpRequest']",
        message: "API 호출은 src/api/api.ts의 공통 라우터를 거칩니다.",
    },
    {
        selector:
            "ImportExpression[source.value=/generated/], ImportExpression[source.value=/transport/], ImportExpression[source.value=/http\\.ts/], ImportExpression[source.value=/router\\.ts/], ImportExpression[source.value=/csrf\\.ts/]",
        message: "공통 요청과 전송 계층을 동적 import로 우회하지 않습니다.",
    },
    {
        selector: "JSXAttribute[name.name='style']",
        message: "외형은 src/styles/app.css의 공용 클래스와 토큰으로 관리합니다.",
    },
    {
        selector: "JSXOpeningElement[name.name='style']",
        message: "style 태그 대신 src/styles/app.css를 사용합니다.",
    },
    {
        selector: "JSXText[value=/\\S/]",
        message: "사용자 노출 문구는 src/locales의 번역 키로 관리합니다.",
    },
];

const apiBoundaryPatterns = [
    {
        group: [
            "openapi-fetch",
            "**/generated/**",
            "**/http.ts",
            "**/router.ts",
            "**/transport.ts",
            "**/csrf.ts",
        ],
        allowTypeImports: true,
        message:
            "공통 정책을 거치는 src/api/api.ts를 사용합니다. 전송 계층은 기반 모듈만 참조합니다.",
    },
];

export default defineConfig([
    globalIgnores(["dist", "src/api/generated/**"]),
    {
        files: ["**/*.{ts,tsx,mts}"],
        extends: [
            js.configs.recommended,
            tseslint.configs.recommended,
            reactHooks.configs.flat.recommended,
            reactRefresh.configs.vite,
        ],
        languageOptions: {
            globals: globals.browser,
        },
    },
    {
        files: ["public/assets/theme.js"],
        extends: [js.configs.recommended],
        languageOptions: { globals: globals.browser },
    },
    {
        files: ["scripts/**/*.mts"],
        languageOptions: { globals: globals.node },
    },
    {
        files: ["src/**/*.{ts,tsx}"],
        rules: {
            "no-restricted-syntax": ["error", ...sharedJsxRestrictions],
            "no-restricted-imports": ["error", { patterns: apiBoundaryPatterns }],
        },
    },
    {
        files: [
            "src/pages/**/*.tsx",
            "src/components/layout/**/*.tsx",
            "src/components/features/**/*.tsx",
        ],
        rules: {
            "no-restricted-syntax": [
                "error",
                ...sharedJsxRestrictions,
                {
                    selector: "JSXOpeningElement[name.name=/^(button|input|select|textarea)$/]",
                    message:
                        "직접 컨트롤을 만들지 말고 components/ui의 Button·TextField·Checkbox·DropdownMenu를 사용합니다.",
                },
            ],
        },
    },
    {
        files: ["src/pages/**/*.{ts,tsx}", "src/components/**/*.{ts,tsx}"],
        rules: {
            "no-restricted-imports": [
                "error",
                {
                    patterns: [
                        ...apiBoundaryPatterns,
                        {
                            group: ["@/api", "@/api/**", "**/api/**"],
                            message:
                                "페이지와 컴포넌트는 API를 직접 호출하지 않고 도메인 훅을 사용합니다.",
                        },
                    ],
                },
            ],
        },
    },
    {
        files: ["src/api/{api,router,http,csrf,transport}.ts"],
        rules: {
            "no-restricted-imports": "off",
        },
    },
    {
        files: ["src/api/transport.ts"],
        rules: { "no-restricted-syntax": "off" },
    },
    {
        files: ["src/components/**/*.{ts,tsx}"],
        rules: {
            "no-restricted-imports": [
                "error",
                {
                    patterns: [
                        ...apiBoundaryPatterns,
                        {
                            group: ["@/api", "@/api/**", "**/api/**"],
                            message: "컴포넌트는 API를 직접 호출하지 않습니다.",
                        },
                        {
                            group: [
                                "@/hooks/api/**",
                                "**/hooks/api/**",
                                "@/pages/**",
                                "**/pages/**",
                            ],
                            message:
                                "도메인 훅 호출은 페이지가 소유하고 상태와 액션을 props로 전달합니다.",
                        },
                    ],
                },
            ],
        },
    },
]);
