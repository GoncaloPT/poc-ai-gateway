#!/usr/bin/env bash
# Starts the tool services poc-mcp-tool-service (8081) and poc-car-brand-service (8082), waits until they are up,
# then starts poc-ai-gateway (8080) and the admin app poc-ai-gateway-app (4200).
# Logs go to logs/. Ctrl+C stops all.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOGS="$ROOT/logs"
mkdir -p "$LOGS"

PORTS=(8081 8082 8080 4200)
for port in "${PORTS[@]}"; do
  if lsof -ti "tcp:$port" -sTCP:LISTEN >/dev/null; then
    echo "Port $port already in use. Stop that process first."; exit 1
  fi
done

pids=()
cleanup() {
  trap - EXIT INT TERM
  echo "Stopping..."
  # spring-boot:run forks the app JVM, so stop the listeners as well as Maven.
  for port in "${PORTS[@]}"; do
    lsof -ti "tcp:$port" -sTCP:LISTEN | xargs kill 2>/dev/null || true
  done
  for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done
  wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

wait_for_port() {
  local name=$1 port=$2 pid=$3
  echo "Waiting for $name on :$port..."
  for _ in $(seq 1 120); do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "$name exited. See $LOGS/$name.log"; exit 1
    fi
    if curl -s -o /dev/null "http://localhost:$port"; then
      echo "$name up on :$port"; return
    fi
    sleep 1
  done
  echo "$name did not start in 120 s. See $LOGS/$name.log"; exit 1
}

start() {
  local name=$1 port=$2
  (cd "$ROOT/$name" && exec ./mvnw -q spring-boot:run) >"$LOGS/$name.log" 2>&1 &
  pids+=($!)
  wait_for_port "$name" "$port" "$!"
}

start_app() {
  local name=poc-ai-gateway-app port=4200
  if [ ! -d "$ROOT/$name/node_modules" ]; then
    echo "Installing $name dependencies..."
    (cd "$ROOT/$name" && npm ci --silent)
  fi
  (cd "$ROOT/$name" && exec npx ng serve --port "$port") >"$LOGS/$name.log" 2>&1 &
  pids+=($!)
  wait_for_port "$name" "$port" "$!"
}

start poc-mcp-tool-service 8081
start poc-car-brand-service 8082
start poc-ai-gateway 8080
start_app

echo "All running. Admin UI: http://localhost:4200. Logs: $LOGS. Ctrl+C to stop."
tail -n 0 -f "$LOGS/poc-mcp-tool-service.log" "$LOGS/poc-car-brand-service.log" "$LOGS/poc-ai-gateway.log" "$LOGS/poc-ai-gateway-app.log" &
pids+=($!)
wait
