# AI Gateway POC

Three Spring Boot projects and an Angular admin app that show how to put a governing **AI gateway** in front of MCP tools offered by
microservices.

```
                        poc-ai-gateway (:8080)
AI client ──POST /mcp──►┌───────────────────────────────────────────────┐
                        │ Spring Cloud Gateway (Server MVC), route "mcp"│
                        │   ClientAccessFilter kill switch 503, cl. 403 │
                        │   rateLimit  Bucket4j, client+user, 429       │
                        │   requestSize  413                            │
                        │   handler = MCP server (no proxy, no forward) │
                        │ MCP server (Spring AI, stateless)             │
                        │   ClientScopedToolsTransport per-client       │
                        │   tools/list + tools/call gate                │
                        │   ToolCallForwarder  rename, token, audit     │
                        │ MCP client (MCP Java SDK), one per upstream   │──POST /mcp──► poc-mcp-tool-service (:8081)
                        │ UpstreamDiscovery  tools/list every 30 s      │                ├─ REST /api/tasks
                        │ ToolRegistry + /admin API                     │                └─ MCP  /mcp (TaskMcpTools)
                        └───────────────────────────────────────────────┘
                                                                        ──POST /mcp──► poc-car-brand-service (:8082)
                                                                                       ├─ REST /api/car-brands
                                                                                       └─ MCP  /mcp (CarBrandMcpTools)
```

| Project | Stack | Purpose |
|---|---|---|
| `poc-mcp-tool-service` | Spring Boot 4.2, Spring AI 2.1 MCP server (stateless Streamable HTTP) | Sample microservice. One `TaskService` (in-memory store) exposed via a REST controller (`POST`/`GET`/`DELETE /api/tasks`) and via `@McpTool` methods (`create_task`, `list_tasks`, `delete_task`). |
| `poc-car-brand-service` | Same stack as `poc-mcp-tool-service` | Second sample microservice, so the gateway merges tools of several upstreams. `CarBrandService` (in-memory, unique by name) via REST (`POST`/`GET /api/car-brands`, `DELETE /api/car-brands/{name}`) and `@McpTool` methods (`add_car_brand`, `list_car_brands`, `remove_car_brand`). Public names get the `cars_` prefix. |
| `poc-ai-gateway-app` | Angular 22 (standalone, signals, zoneless) | Admin UI for the gateway's `/admin` API. **Tools**: filter by status/upstream/text, enable/disable, edit client grants and description override, review discovered tools. **Clients**: enable/disable, register new ones, see which tools each can use. Dev server proxies `/admin` to `:8080`. |
| `poc-ai-gateway` | Spring Boot 4.2, Spring Cloud Gateway Server MVC 5.1 (Spring Cloud 2026.0), Spring AI 2.1 MCP server outward, MCP Java SDK 2.2 client inward | Single public `/mcp` endpoint. Aggregates the tools of all upstreams, decides which ones each AI client sees and may call, and audits every call. |

## Why Spring Cloud Gateway + MCP server + MCP client

Two layers in one app, each doing what it is good at:

- **Spring Cloud Gateway (HTTP edge)**: kill switch, client allowlist, rate limit, request size. These are
  MCP-agnostic: every HTTP request counts the same. The route is defined with the SCG Java DSL
  (`edge/EdgeConfig`), using SCG's `Bucket4jFilterFunctions.rateLimit` and `BeforeFilterFunctions.requestSize` plus
  our `ClientAccessFilter`. Limits come from `ai-gateway.edge` in `application.yml`.
- **MCP server + clients (governance)**: MCP is JSON-RPC over a single endpoint. Which tools a client sees and may
  call lives inside the JSON-RPC body, and `tools/list` must be **merged** across upstreams. SCG only routes HTTP, so
  this part uses the MCP server and client libraries, leaving only the governance logic as own code.

The route's handler **is** the MCP server's router function: no proxy, no forward, no internal endpoint to protect.
It replaces Spring AI's default router bean, so `/mcp` is only reachable through the gateway filters.

Server MVC, not WebFlux: the gateway reuses servlet-based libraries (e.g. a blocking, servlet-only OAuth token
introspection library) and runs on virtual threads. Stateless MCP has no long-lived streams that would need an event
loop.

## What the gateway does

- **Discovery**: every `discovery-interval` the gateway calls `tools/list` on each upstream. New tools are registered
  as `DISCOVERED` (hidden) unless seeded in `application.yml`. Changed definitions are taken over, vanished tools
  removed. If an upstream is down its tools stay listed.
- **Client allowlist**: unknown or disabled clients get `403` before any MCP processing.
- **Rate limit**: 60 HTTP requests/min per client+user (`AI_GATEWAY_RATE_LIMIT`), all MCP methods count. Over the
  limit: `429` with `X-RateLimit-Remaining`.
- **Request size**: bodies over 1 MB get `413`.
- **Deny by default**: only `ENABLED` tools are published. `delete_task` and `remove_car_brand` are not seeded, so they
  stay hidden until an admin enables them.
- **Grants**: a tool can be limited to some clients. The write tools `tasks_create_task` and `cars_add_car_brand` are
  granted to `chat-assistant` only; the read tools `tasks_list_tasks` and `cars_list_car_brands` to every enabled
  client. `tools/list` is filtered per client, and calls of non-granted
  tools get the same "Unknown tool" error as non-existing tools.
- **Renaming**: internal `create_task` becomes public `tasks_create_task` (`<domain>_<tool>`). The description can be
  overridden; the input schema and hints are passed through unchanged.
- **Identity forwarding**: the inbound `Authorization` header and user id are forwarded to the upstream, so it can
  apply its own access checks.
- **Kill switches**: `AI_GATEWAY_ENABLED=false` returns `503` for everything; per-tool status and per-client flag at
  runtime through the admin API.
- **Upstream down**: calls return an MCP error result "temporarily unavailable" instead of failing the protocol.
- **Audit**: one line per call with client, user, method, tool, upstream, outcome and duration, including edge
  rejections (`DENIED_CLIENT`, `DENIED_RATE_LIMIT`, `DENIED_REQUEST_SIZE`, `DENIED_INVALID_REQUEST`) and transport
  errors (`ERROR`). No arguments or results.
- **No internals in errors**: Spring AI's MCP transport returns an `McpError` (an exception) as error body, which
  Jackson serializes with its stack trace. `McpErrorSanitizer`, the innermost route filter, turns it into a plain
  JSON-RPC error (code and message).
- **Admin API**: `/admin/clients` and `/admin/tools` to list, enable, disable and grant at runtime.

## Run

Requires Java 25 and Node (for the admin UI).

```bash
./run.sh
```

Starts both tool services, waits until they are up, then starts the gateway and the admin UI
(http://localhost:4200, needs Node). Logs go to `logs/`; Ctrl+C stops all.
Or start them one by one:

```bash
(cd poc-mcp-tool-service && ./mvnw spring-boot:run)
(cd poc-car-brand-service && ./mvnw spring-boot:run)
(cd poc-ai-gateway && ./mvnw spring-boot:run)
(cd poc-ai-gateway-app && npm ci && npx ng serve)
```


## Try it - inspector

[MCP Inspector](https://github.com/modelcontextprotocol/inspector) v2. The client and user come from headers, so pass
them with `--header`.

```bash
# Web UI, preconnected
npx @modelcontextprotocol/inspector --server-url http://localhost:8080/mcp --transport http \
  --header "X-Client-Id: chat-assistant" --header "X-User-Id: alice"

# CLI
npx @modelcontextprotocol/inspector --cli --server-url http://localhost:8080/mcp --transport http \
  --header "X-Client-Id: ide-agent" --header "X-User-Id: alice" \
  --method tools/call --tool-name tasks_list_tasks
```

In the web UI the headers can also be edited in the server settings. Switch `X-Client-Id` to `ide-agent` to see the
filtered tool list, or to `legacy-bot` to get `403`.

The inspector shows protocol era **Legacy** (its default): the server answers the `initialize` handshake with
protocol `2025-11-25`. `auto` works too (the `server/discover` probe gets `-32601`, then it falls back). `modern`
fails until the MCP Java SDK supports the newer protocol revision.
Add the server:



## Try it - terminal

```bash
mcp() { curl -s localhost:8080/mcp -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' -H "X-Client-Id: $1" -H 'X-User-Id: alice' -d "$2"; echo; }

# chat-assistant sees both write and both read tools; ide-agent only tasks_list_tasks and cars_list_car_brands
mcp chat-assistant '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
mcp ide-agent '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# create a task through the gateway, stored in memory by the tool service
mcp chat-assistant '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"tasks_create_task","arguments":{"title":"Fix bug","description":"NPE in login","priority":"HIGH"}}}'

# ide-agent can't write, but can read: the task created above is returned
mcp ide-agent '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"tasks_create_task","arguments":{"title":"x","description":"y","priority":"LOW"}}}'
mcp ide-agent '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"tasks_list_tasks","arguments":{}}}'

# tools of the second upstream, merged into the same tools/list
mcp chat-assistant '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"cars_add_car_brand","arguments":{"name":"Toyota","country":"Japan"}}}'
mcp ide-agent '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"cars_list_car_brands","arguments":{}}}'

# disabled client: 403
mcp legacy-bot '{"jsonrpc":"2.0","id":7,"method":"tools/list"}'
```

Admin UI at http://localhost:4200 (`poc-ai-gateway-app`), or the admin API directly:

```bash
# all tools incl. the discovered, hidden delete_task and remove_car_brand
curl -s localhost:8080/admin/tools

# approve delete_task for chat-assistant only
curl -s -X PUT localhost:8080/admin/tools/task-service/delete_task -H 'Content-Type: application/json' \
  -d '{"status":"ENABLED","clients":["chat-assistant"]}'

# disable a client: its next request gets 403
curl -s -X PUT localhost:8080/admin/clients/chat-assistant -H 'Content-Type: application/json' -d '{"enabled":false}'
```

The REST path of the tool services works independently of MCP:

```bash
curl -s localhost:8081/api/tasks -H 'Content-Type: application/json' \
  -d '{"title":"Fix bug","description":"NPE in login","priority":"HIGH"}'
curl -s localhost:8081/api/tasks
curl -s localhost:8082/api/car-brands -H 'Content-Type: application/json' -d '{"name":"Volvo","country":"Sweden"}'
curl -s localhost:8082/api/car-brands
```

## POC simplifications

- Client and user come from the `X-Client-Id` / `X-User-Id` headers. In a real setup both come from the validated
  OAuth access token (client claim + subject), which is forwarded unchanged to the upstream.
- Registry and clients are in memory, seeded from YAML; admin changes are lost on restart. A real setup uses a DB,
  an authenticated admin API and audits every admin change.
- Upstreams are static in YAML. A real setup discovers them, e.g. by Kubernetes labels.
- The audit goes to the log. A real setup writes it to a DB (fail-open).
- Calls rejected by the MCP SDK's input schema validation never reach `ToolCallForwarder` and are not audited.
- Rate limit buckets are in memory (Caffeine) per instance. A real setup uses the Bucket4j Redis proxy manager so all
  replicas share them.
- Spring AI's MCP transport returns responses with read-only headers, while SCG's `rateLimit` adds
  `X-RateLimit-Remaining` afterwards. `MutableHeadersResponse` (inner filter) bridges that.
- The tool services use the same Spring AI transport and still put stack traces into their error bodies. They are
  internal only; a real setup adds the same sanitizing there (or in a shared library).
- Not included: circuit breaker, response size cap, tracing.
- Tasks are kept in memory: lost on restart and not shared between replicas.


