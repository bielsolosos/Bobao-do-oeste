import { Component, Input, Output, EventEmitter } from '@angular/core';
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
        <select
          class="block w-full rounded-md border-gray-300 py-2 pl-3 pr-10 text-base focus:border-blue-500 focus:outline-none focus:ring-blue-500 sm:text-sm"
          [value]="activeTabId"
          (change)="onSelectNative($event)"
        >
          @for (tab of tabs; track tab.id) {
            <option [value]="tab.id">{{ tab.label }}</option>
          }
        </select>
      </div>
      <div class="hidden sm:block">
        <div class="border-b border-gray-200">
          <nav class="-mb-px flex space-x-8" aria-label="Tabs">
            @for (tab of tabs; track tab.id) {
              <button
                type="button"
                (click)="selectTab(tab.id)"
                [ngClass]="
                  activeTabId === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
                "
                class="whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium flex items-center gap-2"
              >
                @if (tab.icon) {
                  <span [innerHTML]="tab.icon" class="w-5 h-5"></span>
                }
                {{ tab.label }}
                @if (tab.badge !== undefined) {
                  <span
                    [ngClass]="
                      activeTabId === tab.id
                        ? 'bg-blue-100 text-blue-600'
                        : 'bg-gray-100 text-gray-900'
                    "
                    class="hidden ml-2 rounded-full py-0.5 px-2.5 text-xs font-medium md:inline-block"
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
    const select = event.target as HTMLSelectElement;
    this.selectTab(select.value);
  }
}
