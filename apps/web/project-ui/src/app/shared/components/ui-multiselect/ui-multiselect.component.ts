import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  HostListener,
  Input,
  computed,
  forwardRef,
  inject,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface MultiSelectOption<T = unknown> {
  label: string;
  value: T;
  description?: string;
  badge?: string;
}

@Component({
  selector: 'app-ui-multiselect',
  standalone: true,
  imports: [CommonModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiMultiSelectComponent),
      multi: true,
    },
  ],
  template: `
    <div class="relative w-full text-left" [class.opacity-60]="disabled()">
      <!-- Trigger Button -->
      <div
        tabindex="0"
        role="combobox"
        [attr.aria-expanded]="isOpen()"
        [attr.aria-disabled]="disabled()"
        (click)="toggleOpen()"
        (keydown.enter)="toggleOpen()"
        (keydown.space)="toggleOpen()"
        (keydown.escape)="close()"
        class="flex min-h-[42px] w-full cursor-pointer items-center justify-between gap-2 rounded-lg border bg-surface px-3 py-1.5 transition-colors focus:outline-none focus:ring-2"
        [ngClass]="
          isOpen()
            ? 'border-brand-amber-strong ring-2 ring-brand-amber-strong'
            : 'border-brand-950/20 hover:border-brand-950/40'
        "
      >
        <!-- Selected Items or Placeholder -->
        <div class="flex flex-1 flex-wrap items-center gap-1.5 overflow-hidden">
          @if (selectedValues().length === 0) {
            <span class="text-sm text-brand-950/40">{{ placeholder }}</span>
          } @else if (displayMode === 'chips') {
            @for (val of visibleSelectedValues(); track trackByValue(val)) {
              <span
                class="inline-flex items-center gap-1 rounded-md bg-brand-amber/15 px-2 py-0.5 text-xs font-medium text-brand-amber-strong"
              >
                <span>{{ getLabel(val) }}</span>
                @if (!disabled()) {
                  <button
                    type="button"
                    (click)="removeItem(val, $event)"
                    class="rounded-sm hover:bg-brand-amber/20 hover:text-brand-950 focus:outline-none"
                    [attr.aria-label]="'Remover ' + getLabel(val)"
                  >
                    <svg
                      class="h-3 w-3"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M6 18L18 6M6 6l12 12"
                      />
                    </svg>
                  </button>
                }
              </span>
            }
            @if (extraCount() > 0) {
              <span
                class="inline-flex items-center rounded-md bg-brand-950/10 px-1.5 py-0.5 text-xs font-semibold text-brand-950/70"
              >
                +{{ extraCount() }}
              </span>
            }
          } @else {
            <span class="text-sm font-medium text-brand-950">
              {{ selectedValues().length }} selecionado(s)
            </span>
          }
        </div>

        <!-- Icons (Clear & Chevron) -->
        <div class="flex items-center gap-1 text-brand-950/50">
          @if (showClear && selectedValues().length > 0 && !disabled()) {
            <button
              type="button"
              (click)="clearAll($event)"
              class="p-0.5 text-brand-950/40 hover:text-brand-950"
              title="Limpar seleção"
            >
              <svg
                class="h-4 w-4"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          }

          <svg
            class="h-4 w-4 transition-transform duration-200"
            [class.rotate-180]="isOpen()"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </div>
      </div>

      <!-- Dropdown Overlay -->
      @if (isOpen()) {
        <div
          class="absolute z-50 mt-1 max-h-80 w-full min-w-[280px] overflow-hidden rounded-xl border border-brand-950/15 bg-surface shadow-xl shadow-brand-950/10 transition-all animate-in fade-in-50 zoom-in-95"
        >
          <!-- Search Header & Actions -->
          <div class="border-b border-brand-950/10 bg-brand-950/[0.02] p-2">
            @if (filter) {
              <div class="relative mb-2">
                <input
                  type="text"
                  [value]="filterQuery()"
                  (input)="onFilterInput($event)"
                  [placeholder]="filterPlaceholder"
                  class="w-full rounded-md border border-brand-950/15 bg-surface py-1.5 pr-3 pl-8 text-xs text-brand-950 placeholder:text-brand-950/40 focus:border-brand-amber-strong focus:outline-none focus:ring-1 focus:ring-brand-amber-strong"
                />
                <svg
                  class="absolute top-2 left-2.5 h-3.5 w-3.5 text-brand-950/40"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                  />
                </svg>
              </div>
            }

            @if (showSelectAll) {
              <div class="flex items-center justify-between px-1 text-xs">
                <button
                  type="button"
                  (click)="selectAll()"
                  class="font-medium text-brand-amber-strong hover:underline"
                >
                  Selecionar todos
                </button>
                <button
                  type="button"
                  (click)="deselectAll()"
                  class="text-brand-950/50 hover:text-brand-950 hover:underline"
                >
                  Desmarcar todos
                </button>
              </div>
            }
          </div>

          <!-- Options List -->
          <div class="max-h-56 overflow-y-auto p-1">
            @if (filteredOptions().length === 0) {
              <div class="px-3 py-4 text-center text-xs text-brand-950/50">
                Nenhuma opção encontrada.
              </div>
            } @else {
              @for (opt of filteredOptions(); track trackByOption(opt)) {
                <div
                  (click)="toggleOption(opt)"
                  class="flex cursor-pointer items-start gap-2.5 rounded-lg px-2.5 py-2 text-sm transition-colors hover:bg-brand-950/5"
                  [class.bg-brand-amber/10]="isSelected(opt.value)"
                >
                  <!-- Custom Checkbox -->
                  <div
                    class="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded border transition-colors"
                    [ngClass]="
                      isSelected(opt.value)
                        ? 'border-brand-amber-strong bg-brand-amber-strong text-white'
                        : 'border-brand-950/30 bg-surface'
                    "
                  >
                    @if (isSelected(opt.value)) {
                      <svg
                        class="h-3 w-3 stroke-[3]"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          d="M5 13l4 4L19 7"
                        />
                      </svg>
                    }
                  </div>

                  <!-- Option Text -->
                  <div class="flex flex-1 flex-col">
                    <div class="flex items-center justify-between gap-1.5">
                      <span
                        class="font-medium"
                        [ngClass]="
                          isSelected(opt.value)
                            ? 'text-brand-amber-strong'
                            : 'text-brand-950'
                        "
                      >
                        {{ opt.label }}
                      </span>
                      @if (opt.badge) {
                        <span
                          class="rounded-full bg-brand-950/5 px-2 py-0.5 text-[10px] font-semibold text-brand-950/60"
                        >
                          {{ opt.badge }}
                        </span>
                      }
                    </div>

                    @if (opt.description) {
                      <span class="mt-0.5 text-xs text-brand-950/60">
                        {{ opt.description }}
                      </span>
                    }
                  </div>
                </div>
              }
            }
          </div>
        </div>
      }
    </div>
  `,
})
export class UiMultiSelectComponent<T = unknown> implements ControlValueAccessor {
  private elementRef = inject(ElementRef);

  @Input() options: MultiSelectOption<T>[] = [];
  @Input() placeholder = 'Selecione as opções...';
  @Input() displayMode: 'chips' | 'count' = 'chips';
  @Input() maxSelectedLabels = 3;
  @Input() filter = true;
  @Input() filterPlaceholder = 'Filtrar opções...';
  @Input() showSelectAll = true;
  @Input() showClear = true;

  isOpen = signal(false);
  disabled = signal(false);
  selectedValues = signal<T[]>([]);
  filterQuery = signal('');

  private onChange: (value: T[]) => void = () => {};
  private onTouched: () => void = () => {};

  filteredOptions = computed(() => {
    const q = this.filterQuery().toLowerCase().trim();
    if (!q) return this.options;
    return this.options.filter(
      (opt) =>
        opt.label.toLowerCase().includes(q) ||
        (opt.description && opt.description.toLowerCase().includes(q)) ||
        (opt.badge && opt.badge.toLowerCase().includes(q)),
    );
  });

  visibleSelectedValues = computed(() => {
    return this.selectedValues().slice(0, this.maxSelectedLabels);
  });

  extraCount = computed(() => {
    const total = this.selectedValues().length;
    return total > this.maxSelectedLabels ? total - this.maxSelectedLabels : 0;
  });

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.close();
    }
  }

  toggleOpen() {
    if (this.disabled()) return;
    this.isOpen.update((v) => !v);
    if (!this.isOpen()) {
      this.onTouched();
    }
  }

  close() {
    if (this.isOpen()) {
      this.isOpen.set(false);
      this.filterQuery.set('');
      this.onTouched();
    }
  }

  isSelected(val: T): boolean {
    return this.selectedValues().some((item) => this.areEqual(item, val));
  }

  toggleOption(opt: MultiSelectOption<T>) {
    if (this.disabled()) return;
    const current = this.selectedValues();
    const index = current.findIndex((item) => this.areEqual(item, opt.value));

    let updated: T[];
    if (index >= 0) {
      updated = current.filter((_, i) => i !== index);
    } else {
      updated = [...current, opt.value];
    }

    this.updateValues(updated);
  }

  removeItem(val: T, event: MouseEvent) {
    event.stopPropagation();
    if (this.disabled()) return;
    const updated = this.selectedValues().filter((item) => !this.areEqual(item, val));
    this.updateValues(updated);
  }

  selectAll() {
    const allFiltered = this.filteredOptions().map((o) => o.value);
    const combined = [...this.selectedValues()];
    for (const val of allFiltered) {
      if (!combined.some((item) => this.areEqual(item, val))) {
        combined.push(val);
      }
    }
    this.updateValues(combined);
  }

  deselectAll() {
    const allFiltered = this.filteredOptions().map((o) => o.value);
    const updated = this.selectedValues().filter(
      (val) => !allFiltered.some((item) => this.areEqual(item, val)),
    );
    this.updateValues(updated);
  }

  clearAll(event: MouseEvent) {
    event.stopPropagation();
    if (this.disabled()) return;
    this.updateValues([]);
  }

  onFilterInput(event: Event) {
    this.filterQuery.set((event.target as HTMLInputElement).value);
  }

  getLabel(val: T): string {
    const found = this.options.find((opt) => this.areEqual(opt.value, val));
    return found ? found.label : String(val);
  }

  trackByValue(val: T): string {
    return typeof val === 'object' && val !== null ? JSON.stringify(val) : String(val);
  }

  trackByOption(opt: MultiSelectOption<T>): string {
    return opt.label + '_' + this.trackByValue(opt.value);
  }

  private areEqual(a: T, b: T): boolean {
    return a === b;
  }

  private updateValues(values: T[]) {
    this.selectedValues.set(values);
    this.onChange(values);
    this.onTouched();
  }

  // --- ControlValueAccessor Implementation ---
  writeValue(value: T[] | null): void {
    if (Array.isArray(value)) {
      this.selectedValues.set(value);
    } else {
      this.selectedValues.set([]);
    }
  }

  registerOnChange(fn: (value: T[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }
}
