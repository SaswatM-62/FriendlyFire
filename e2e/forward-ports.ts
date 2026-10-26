import net from "node:net";

/**
 * In the Playwright container "localhost" is the container, not your machine. This
 * forwards local ports to the stack, e.g. E2E_FORWARD="3000=frontend:3000
 * 8080=gateway-service:8080", so the browser uses http://localhost:3000 and
 * ws://localhost:8080 exactly like you do (same origin, so CORS still applies).
 */
export default async function forwardPorts(): Promise<() => Promise<void>> {
  const rules = (process.env.E2E_FORWARD ?? "").split(/\s+/).filter(Boolean);
  const servers = await Promise.all(
    rules.map((rule) => {
      const [localPort, target] = rule.split("=");
      const [host, port] = target.split(":");
      return forward(Number(localPort), host, Number(port));
    }),
  );
  return async () => {
    await Promise.all(servers.map((server) => new Promise((resolve) => server.close(resolve))));
  };
}

function forward(localPort: number, host: string, port: number): Promise<net.Server> {
  return new Promise((resolve, reject) => {
    const server = net.createServer((client) => {
      const upstream = net.connect(port, host);
      client.pipe(upstream).pipe(client);
      client.on("error", () => upstream.destroy());
      upstream.on("error", () => client.destroy());
    });
    server.once("error", reject);
    server.listen(localPort, () => resolve(server));
  });
}
