import { readdir, readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import postcss from "postcss";

const frontend = fileURLToPath(new URL("../", import.meta.url));
const styles = "styles/app.css";
const files = await readdir(resolve(frontend, "src"), { recursive: true });
const errors: string[] = [];
for (const path of files) {
    const file = path.replaceAll("\\", "/");
    if (/\.(css|scss|sass|less)$/.test(file) && file !== styles) {
        errors.push(`src/${file}: 스타일은 src/${styles}에서 관리합니다.`);
    }
}

const source = await readFile(resolve(frontend, "src", styles), "utf8");
const stylesheet = postcss.parse(source, { from: styles });
stylesheet.walkRules((rule) => {
    const declarations = new Set<string>();
    rule.each((node) => {
        if (node.type !== "decl") return;
        const key = `${node.prop.startsWith("--") ? node.prop : node.prop.toLowerCase()}:${node.value}:${node.important}`;
        if (declarations.has(key)) {
            errors.push(
                `${styles}:${node.source?.start?.line}: ${rule.selector}의 ${node.prop} 선언이 중복됩니다.`,
            );
        }
        declarations.add(key);
    });
});

if (errors.length) {
    process.stderr.write(errors.join("\n") + "\n");
    process.exitCode = 1;
} else {
    process.stdout.write("UI 스타일 소유권·동일 규칙 내 중복 선언 검사 통과\n");
}
