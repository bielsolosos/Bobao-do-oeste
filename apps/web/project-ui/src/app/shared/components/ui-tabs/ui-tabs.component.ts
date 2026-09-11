import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TabItem {
  id: string;
  label: string;
  icon?: string;
  badge?: number;
}

@Component({
  selector: 'app-ui-tabs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div>
      <div class="sm:hidden">
        <label class="sr-only" for="ui-tabs-select">Selecionar seção</label>
        <select
          id="ui-tabs-select"
          class="block w-full rounded-lg border border-brand-950/20 bg-surface py-2.5 pl-3 pr-10 text-base text-brand-950 focus:border-brand-amber-strong focus:outline-none focus:ring-2 focus:ring-brand-amber-strong sm:text-sm"
          [value]="activeTabId"
          (change)="onSelectNative($event)"
        >
          @for (tab of tabs; track tab.id) {
            <option [value]="tab.id">{{ tab.label }}</option>
          }
        </select>
      </div>

      <div class="hidden sm:block">
        <div class="border-b border-brand-950/10">
          <nav
            class="-mb-px flex gap-6 overflow-x-auto"
            role="tablist"
            aria-label="Seções"
          >
            @for (tab of tabs; track tab.id) {
              <button
                type="button"
                role="tab"
                [id]="'tab-' + tab.id"
                [attr.aria-selected]="activeTabId === tab.id"
                [attr.aria-controls]="'panel-' + tab.id"
                [attr.tabindex]="activeTabId === tab.id ? 0 : -1"
                (click)="selectTab(tab.id)"
                (keydown)="onKeydown($event, tab.id)"
                [ngClass]="
                  activeTabId === tab.id
                    ? 'border-brand-amber-strong text-brand-amber-strong'
                    : 'border-transparent text-brand-950/60 hover:border-brand-950/20 hover:text-brand-950'
                "
                class="flex items-center gap-2 whitespace-nowrap border-b-2 px-1 py-3.5 text-sm font-medium transition-colors"
              >
                @if (tab.icon) {
                  <span [innerHTML]="tab.icon" class="h-5 w-5" aria-hidden="true"></span>
                }
                {{ tab.label }}
                @if (tab.badge !== undefined) {
                  <span
                    [ngClass]="
                      activeTabId === tab.id
                        ? 'bg-brand-amber/20 text-brand-amber-strong'
                        : 'bg-brand-950/10 text-brand-950/70'
                    "
                    class="ml-1 rounded-full px-2.5 py-0.5 text-xs font-medium"
                  >
                    {{ tab.badge }}
                  </span>
                }
              </button>
            }
          </nav>
        </div>
      </div>
    </div>
  `,
})
export class UiTabsComponent {
  @Input() tabs: TabItem[] = [];
  @Input() activeTabId: string = '';
  @Output() tabChange = new EventEmitter<string>();

  selectTab(id: string) {
    this.tabChange.emit(id);
  }

  onSelectNative(event: Event) {
    this.selectTab((event.target as HTMLSelectElement).value);
  }

  onKeydown(event: KeyboardEvent, currentId: string) {
    const keys = ['ArrowRight', 'ArrowLeft', 'Home', 'End'];
    if (!keys.includes(event.key)) return;
    event.preventDefault();

    const index = this.tabs.findIndex((t) => t.id === currentId);
    if (index < 0) return;

    let nextIndex = index;
    if (event.key === 'ArrowRight') nextIndex = (index + 1) % this.tabs.length;
    if (event.key === 'ArrowLeft') nextIndex = (index - 1 + this.tabs.length) % this.tabs.length;
    if (event.key === 'Home') nextIndex = 0;
    if (event.key === 'End') nextIndex = this.tabs.length - 1;

    const next = this.tabs[nextIndex];
    if (!next) return;
    this.selectTab(next.id);

    queueMicrotask(() => {
      const el = document.getElementById('tab-' + next.id);
      el?.focus();
    });
  }
}
