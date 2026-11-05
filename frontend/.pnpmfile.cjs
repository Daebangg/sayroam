module.exports = {
    hooks: {
        readPackage(pkg) {
            if (pkg.name === "openapi-typescript" && pkg.version === "7.13.0") {
                // 생성기의 TypeScript 5 AST 런타임을 앱의 TypeScript 6과 분리합니다.
                pkg.dependencies.typescript = "5.9.3";
                delete pkg.peerDependencies.typescript;
            }
            return pkg;
        },
    },
};
