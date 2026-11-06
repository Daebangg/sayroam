import { fileURLToPath, URL } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

const frontendDir = fileURLToPath(new URL(".", import.meta.url));

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, frontendDir, "API_PROXY_");

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
                "/api": {
                    target: env.API_PROXY_TARGET || "http://localhost:8080",
                    changeOrigin: true,
                },
            },
        },
        preview: {
            port: 4173,
            strictPort: true,
        },
    };
});
