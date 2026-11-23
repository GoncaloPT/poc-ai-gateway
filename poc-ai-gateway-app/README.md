# poc-ai-gateway-app

Admin UI for `poc-ai-gateway` (Angular 22, standalone components, signals, zoneless).

- **Tools**: every tool the gateway knows, including discovered ones awaiting review. Filter by status, upstream or
  text; enable/disable; edit client grants (none = every enabled client) and the description shown to AI clients.
  Warns when a write tool is open to every client.
- **Clients**: enable/disable AI clients (disabled = `403`), register new ones, see which tools each client can use.

## Run

Needs the gateway on `:8080` (`../run.sh` starts everything).

```bash
npm ci
npx ng serve        # http://localhost:4200, /admin is proxied to :8080 (proxy.conf.json)
npx ng test --watch=false
```

POC: no login. In a real setup the admin API requires an admin role and is reached through an internal API
gateway, not the AI gateway's public Ingress.
