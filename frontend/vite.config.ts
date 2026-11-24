import { fileURLToPath, URL } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

const frontendDir = fileURLToPath(new URL(".", import.meta.url));

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, frontendDir, "API_PROXY_");
    const apiTarget = env.API_PROXY_TARGET || "http://localhost:8080";

    return {
        plugins: [react()],
        resolve: {
            alias: {
                "@": fileURLToPath(new URL("./src", import.meta.url)),
            },
        },
        server: {
            port: 5173,
            strictPort: true,
            proxy: {
                "/images/profiles": {
                    target: apiTarget,
                    changeOrigin: true,
                },
                "/api": {
                    target: apiTarget,
                    changeOrigin: true,
                },
                // 백엔드 웹소켓은 같은 출처만 받는다. 개발 서버를 거친 연결도 백엔드 출처로 보낸다.
                "/ws": {
                    target: apiTarget,
                    changeOrigin: true,
                    ws: true,
                    configure: (proxy) => {
                        proxy.on("proxyReqWs", (proxyReq) => {
                            proxyReq.setHeader("origin", new URL(apiTarget).origin);
                        });
                    },
                },
            },
        },
        preview: {
            port: 4173,
            strictPort: true,
        },
    };
});
