// FF-513: a production build must not contain the fake backend. Run after `npm run build`.
// The fake backend's code carries a marker string (src/mocks/data.ts); it must not
// appear anywhere in the build output, even if NEXT_PUBLIC_FAKE_BACKEND=true was set.
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

const MARKER = "ff-fake-backend-v1";
const roots = [".next/static", ".next/server"];
const found = [];

function scan(dir) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) scan(path);
    else if (/\.(js|mjs|cjs|html|json|rsc)$/.test(name) && readFileSync(path, "utf8").includes(MARKER)) found.push(path);
  }
}

for (const root of roots) {
  try {
    scan(root);
  } catch {
    console.error(`No ${root}: run \`npm run build\` first.`);
    process.exit(2);
  }
}

if (found.length > 0) {
  console.error(`The fake backend is in the production build:\n  ${found.join("\n  ")}`);
  process.exit(1);
}
console.log("OK: no fake-backend code in the production build.");
