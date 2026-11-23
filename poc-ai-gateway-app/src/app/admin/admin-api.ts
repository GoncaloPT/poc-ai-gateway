import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminTool, Clients, ToolUpdate } from './admin.models';

/**
 * Client of the AI gateway admin API ({@code /admin}). Read state is exposed as shared, reloadable resources so the
 * clients and tools pages stay consistent after a change on either page.
 */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  /** All AI clients and whether they are enabled. */
  readonly clients = httpResource<Clients>(() => '/admin/clients', { defaultValue: {} });

  /** All tools known to the gateway, including discovered ones that are not exposed yet. */
  readonly tools = httpResource<AdminTool[]>(() => '/admin/tools', { defaultValue: [] });

  /**
   * Registers, enables or disables an AI client. A disabled client gets 403 on its next request.
   *
   * @param clientId AI client id
   * @param enabled new state
   * @returns all clients after the change
   */
  setClientEnabled(clientId: string, enabled: boolean): Observable<Clients> {
    return this.http.put<Clients>(`/admin/clients/${encodeURIComponent(clientId)}`, { enabled });
  }

  /**
   * Changes status, client grants and description override of a tool.
   *
   * @param tool tool to change
   * @param update new rules
   * @returns the updated tool
   */
  updateTool(tool: AdminTool, update: ToolUpdate): Observable<AdminTool> {
    const path = `/admin/tools/${encodeURIComponent(tool.upstream)}/${encodeURIComponent(tool.internalName)}`;
    return this.http.put<AdminTool>(path, update);
  }

  /** Reloads clients and tools from the gateway. */
  reload(): void {
    this.clients.reload();
    this.tools.reload();
  }
}
