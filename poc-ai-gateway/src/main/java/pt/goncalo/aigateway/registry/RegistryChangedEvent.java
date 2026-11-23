package pt.goncalo.aigateway.registry;

/**
 * Published whenever the set of exposed tools may have changed, so the MCP server can re-sync its tool list.
 */
public record RegistryChangedEvent() {
}
