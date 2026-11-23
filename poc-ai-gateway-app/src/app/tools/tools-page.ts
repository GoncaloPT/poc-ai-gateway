import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AdminApi } from '../admin/admin-api';
import { AdminTool, ToolStatus, ToolUpdate, currentRules } from '../admin/admin.models';
import { errorMessage } from '../admin/error-message';
import { ToolEditor } from './tool-editor';

/**
 * Lists every tool the gateway knows (enabled, disabled and newly discovered), with quick enable/disable and an
 * editor for client grants and description override.
 */
@Component({
  selector: 'app-tools-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ToolEditor],
  templateUrl: './tools-page.html',
})
export class ToolsPage {
  protected readonly api = inject(AdminApi);

  protected readonly statusFilter = signal<ToolStatus | ''>('');
  protected readonly upstreamFilter = signal('');
  protected readonly search = signal('');
  protected readonly editing = signal<string | null>(null);
  protected readonly busy = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly clientIds = computed(() => Object.keys(this.api.clients.value()).sort());

  protected readonly upstreams = computed(() => [...new Set(this.api.tools.value().map((tool) => tool.upstream))].sort());

  protected readonly discoveredCount = computed(
    () => this.api.tools.value().filter((tool) => tool.status === 'DISCOVERED').length,
  );

  protected readonly visibleTools = computed(() => {
    const status = this.statusFilter();
    const upstream = this.upstreamFilter();
    const term = this.search().trim().toLowerCase();
    return this.api
      .tools.value()
      .filter((tool) => !status || tool.status === status)
      .filter((tool) => !upstream || tool.upstream === upstream)
      .filter(
        (tool) =>
          !term ||
          tool.publicName.toLowerCase().includes(term) ||
          (tool.description ?? '').toLowerCase().includes(term),
      )
      .sort((a, b) => a.publicName.localeCompare(b.publicName));
  });

  protected readonly editedTool = computed(() => this.api.tools.value().find((tool) => tool.id === this.editing()));

  /** Enables a disabled or discovered tool, or disables an enabled one, keeping grants and description. */
  protected toggle(tool: AdminTool): void {
    this.save(tool, { ...currentRules(tool), status: tool.status === 'ENABLED' ? 'DISABLED' : 'ENABLED' });
  }

  protected save(tool: AdminTool, update: ToolUpdate): void {
    this.busy.set(tool.id);
    this.error.set(null);
    this.api.updateTool(tool, update).subscribe({
      next: (updated) => {
        this.api.tools.update((tools) => tools.map((current) => (current.id === updated.id ? updated : current)));
        this.editing.set(null);
        this.busy.set(null);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.busy.set(null);
      },
    });
  }

  protected statusLabel(status: ToolStatus): string {
    return { ENABLED: 'Enabled', DISABLED: 'Disabled', DISCOVERED: 'Discovered' }[status];
  }
}
