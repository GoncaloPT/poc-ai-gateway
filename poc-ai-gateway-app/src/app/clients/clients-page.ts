import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AdminApi } from '../admin/admin-api';
import { isGranted } from '../admin/admin.models';
import { errorMessage } from '../admin/error-message';

/** One row of the clients table. */
interface ClientRow {
  id: string;
  enabled: boolean;
  tools: string[];
}

/**
 * Lists AI clients, enables and disables them, and registers new ones. Also shows which tools each client can use,
 * derived from the tool grants.
 */
@Component({
  selector: 'app-clients-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './clients-page.html',
})
export class ClientsPage {
  protected readonly api = inject(AdminApi);

  protected readonly newClientId = signal('');
  protected readonly busy = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly rows = computed<ClientRow[]>(() => {
    const tools = this.api.tools.value();
    return Object.entries(this.api.clients.value())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([id, enabled]) => ({
        id,
        enabled,
        tools: tools.filter((tool) => isGranted(tool, id)).map((tool) => tool.publicName),
      }));
  });

  protected readonly canAdd = computed(() => {
    const id = this.newClientId().trim();
    return id.length > 0 && !(id in this.api.clients.value());
  });

  /** Flips the enabled flag of a client. */
  protected toggle(row: ClientRow): void {
    this.save(row.id, !row.enabled);
  }

  /** Registers the client typed in the form, enabled. */
  protected add(): void {
    if (this.canAdd()) {
      this.save(this.newClientId().trim(), true, () => this.newClientId.set(''));
    }
  }

  private save(clientId: string, enabled: boolean, onSuccess?: () => void): void {
    this.busy.set(clientId);
    this.error.set(null);
    this.api.setClientEnabled(clientId, enabled).subscribe({
      next: (clients) => {
        this.api.clients.set(clients);
        this.busy.set(null);
        onSuccess?.();
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.busy.set(null);
      },
    });
  }
}
