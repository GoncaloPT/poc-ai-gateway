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
  status: ToolStatus;
  /** Empty = every enabled client. */
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
 * @returns the update that keeps the tool's current rules
 */
export function currentRules(tool: AdminTool): ToolUpdate {
  return { status: tool.status, clients: [...tool.clients], description: tool.descriptionOverride };
}
