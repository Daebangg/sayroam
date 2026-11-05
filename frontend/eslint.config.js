import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import tseslint from "typescript-eslint";
import { defineConfig, globalIgnores } from "eslint/config";

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
        files: ["scripts/**/*.mts"],
        languageOptions: { globals: globals.node },
    },
    {
        files: ["src/**/*.{ts,tsx}"],
        rules: {
            "no-restricted-syntax": [
                "error",
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
        files: ["src/components/**/*.{ts,tsx}"],
        rules: {
            "no-restricted-imports": [
                "error",
                {
                    patterns: [
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
