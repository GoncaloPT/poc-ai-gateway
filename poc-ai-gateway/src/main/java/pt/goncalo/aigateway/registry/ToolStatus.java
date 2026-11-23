package pt.goncalo.aigateway.registry;

/**
 * Lifecycle of a tool in the gateway registry.
 */
public enum ToolStatus {

	/** Found on an upstream, never approved. Not exposed. */
	DISCOVERED,

	/** Approved and exposed. */
	ENABLED,

	/** Switched off by an admin. Not exposed. */
	DISABLED

}
