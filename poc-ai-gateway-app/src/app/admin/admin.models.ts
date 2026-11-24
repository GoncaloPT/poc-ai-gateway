/** Lifecycle of a tool in the gateway registry. */
export type ToolStatus = 'DISCOVERED' | 'ENABLED' | 'DISABLED';

/** AI clients by id, value = enabled. */
export type Clients = Record<string, boolean>;

/** Tool as returned by {@code GET /admin/tools}. */
export interface AdminTool {
  /** Registry key: {@code upstream/internalName}. */
  id: string;
  upstream: string;
  internalName: string;
  /** Name shown to AI clients. */
  publicName: string;
  status: ToolStatus;
  /** Granted clients. Empty = every enabled client. */
  clients: string[];
  /** Description shown to AI clients. */
  description: string | null;
  upstreamDescription: string | null;
  /** Gateway override, null = upstream description is used. */
  descriptionOverride: string | null;
  readOnly: boolean | null;
  destructive: boolean | null;
}

/** Body of {@code PUT /admin/tools/{upstream}/{internalName}}. */
export interface ToolUpdate {
  /** Name shown to AI clients. 1-128 of A-Z, a-z, 0-9, '_', '-', '.'. */
  publicName: string;
  status: ToolStatus;
  /** Empty = every enabled client. Rejected by the gateway for enabled write tools. */
  clients: string[];
  /** Null = upstream description. */
  description: string | null;
}

/**
 * @param tool registry tool
 * @param clientId AI client id
 * @returns true if the client may see and call the tool (ignores whether the client itself is enabled)
 */
export function isGranted(tool: AdminTool, clientId: string): boolean {
  return tool.status === 'ENABLED' && (tool.clients.length === 0 || tool.clients.includes(clientId));
}

/**
 * @param tool registry tool
 * @returns true if the tool can only be enabled after granting at least one client (write tool without grants)
 */
export function needsGrant(tool: Pick<AdminTool, 'readOnly' | 'clients'>): boolean {
  return tool.readOnly !== true && tool.clients.length === 0;
}

/**
 * @param tool registry tool
 * @returns the update that keeps the tool's current rules
 */
export function currentRules(tool: AdminTool): ToolUpdate {
  return {
    publicName: tool.publicName,
    status: tool.status,
    clients: [...tool.clients],
    description: tool.descriptionOverride,
  };
}

/** MCP tool name rules, same check as the gateway. */
export const PUBLIC_NAME_PATTERN = /^[A-Za-z0-9_.-]{1,128}$/;
