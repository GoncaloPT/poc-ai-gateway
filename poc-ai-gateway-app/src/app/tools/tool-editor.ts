import { ChangeDetectionStrategy, Component, OnInit, computed, input, output, signal } from '@angular/core';
import { AdminTool, PUBLIC_NAME_PATTERN, ToolStatus, ToolUpdate } from '../admin/admin.models';

/**
 * Edits the exposure rules of one tool: public name, status, granted clients and description override.
 */
@Component({
  selector: 'app-tool-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tool-editor.html',
})
export class ToolEditor implements OnInit {
  /** Tool to edit. The editor is recreated per tool, so its draft starts from this value. */
  readonly tool = input.required<AdminTool>();
  /** All known client ids, to choose grants from. */
  readonly clientIds = input.required<string[]>();
  readonly busy = input(false);

  readonly saved = output<ToolUpdate>();
  readonly cancelled = output<void>();

  protected readonly statuses: ToolStatus[] = ['ENABLED', 'DISABLED'];

  protected readonly publicName = signal('');
  protected readonly status = signal<ToolStatus>('ENABLED');
  protected readonly grants = signal<string[]>([]);
  protected readonly overrideDescription = signal(false);
  protected readonly description = signal('');

  /** Write tools need an explicit grant; the gateway rejects an enabled write tool open to every client. */
  protected readonly openWriteTool = computed(
    () => this.status() === 'ENABLED' && this.tool().readOnly !== true && this.grants().length === 0,
  );

  protected readonly publicNameValid = computed(() => PUBLIC_NAME_PATTERN.test(this.publicName().trim()));

  protected readonly valid = computed(
    () =>
      this.publicNameValid() &&
      !this.openWriteTool() &&
      (!this.overrideDescription() || this.description().trim().length > 0),
  );

  ngOnInit(): void {
    const tool = this.tool();
    this.publicName.set(tool.publicName);
    this.status.set(tool.status === 'DISCOVERED' ? 'ENABLED' : tool.status);
    this.grants.set([...tool.clients]);
    this.overrideDescription.set(tool.descriptionOverride !== null);
    this.description.set(tool.descriptionOverride ?? tool.upstreamDescription ?? '');
  }

  protected toggleGrant(clientId: string, granted: boolean): void {
    this.grants.update((current) =>
      granted ? [...current, clientId].sort() : current.filter((id) => id !== clientId),
    );
  }

  protected save(): void {
    if (this.valid()) {
      this.saved.emit({
        publicName: this.publicName().trim(),
        status: this.status(),
        clients: this.grants(),
        description: this.overrideDescription() ? this.description().trim() : null,
      });
    }
  }
}
