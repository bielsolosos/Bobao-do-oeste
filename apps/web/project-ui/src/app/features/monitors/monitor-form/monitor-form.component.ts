import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MonitorService } from '../../../core/services/monitor.service';
import {
  AnalysisType,
  ProductMonitorRequest,
  ScrapingFrequency,
  Vendor,
} from '../../../core/models/monitor.model';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiFormFieldComponent } from '../../../shared/components/ui-form-field/ui-form-field.component';
import { UiPageHeaderComponent } from '../../../shared/components/ui-page-header/ui-page-header.component';
import { UiStatePanelComponent } from '../../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';
import { ComponentWithPendingChanges } from '../../../core/guards/pending-changes.guard';

function priceRangeValidator(group: AbstractControl): ValidationErrors | null {
  const min = group.get('minPrice')?.value;
  const max = group.get('maxPrice')?.value;
  if (min != null && max != null && Number(min) > Number(max)) {
    return { priceRange: true };
  }
  return null;
}

@Component({
  selector: 'app-monitor-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    UiButtonComponent,
    UiCardComponent,
    UiFormFieldComponent,
    UiPageHeaderComponent,
    UiStatePanelComponent,
  ],
  templateUrl: './monitor-form.component.html',
})
export class MonitorFormComponent implements OnInit, ComponentWithPendingChanges {
  private fb = inject(FormBuilder);
  private monitorService = inject(MonitorService);
  public router = inject(Router);
  private route = inject(ActivatedRoute);
  private toast = inject(UiToastService);

  isSaving = signal(false);
  isLoading = signal(false);
  loadError = signal(false);
  isEditMode = false;
  monitorId: string | null = null;

  keywords = signal<string[]>([]);
  keywordInput = signal('');

  readonly frequencies: { value: ScrapingFrequency; label: string }[] = [
    { value: 'EVERY_MINUTE', label: 'A cada 1 minuto (agressivo)' },
    { value: 'EVERY_5_MINUTES', label: 'A cada 5 minutos' },
    { value: 'EVERY_30_MINUTES', label: 'A cada 30 minutos' },
    { value: 'HOURLY', label: 'A cada hora' },
    { value: 'EVERY_6_HOURS', label: 'A cada 6 horas' },
    { value: 'DAILY', label: 'Uma vez ao dia' },
    { value: 'TWICE_DAILY', label: 'Duas vezes ao dia' },
    { value: 'WEEKLY', label: 'Semanal' },
    { value: 'MANUAL', label: 'Somente manual' },
  ];

  form = this.fb.group(
    {
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      vendor: ['OLX' as Vendor, Validators.required],
      frequency: ['EVERY_30_MINUTES' as ScrapingFrequency, Validators.required],
      minPrice: [null as number | null, [Validators.min(0)]],
      maxPrice: [null as number | null, [Validators.min(0)]],
      stateFilter: [''],
      regionFilter: [''],
      requireDelivery: [false],
      analysisType: ['NONE' as AnalysisType, Validators.required],
      simpleFields: this.fb.group({
        prompt: [''],
      }),
      notebookFields: this.fb.group({
        minimumRamGb: [8],
        needsDedicatedGpu: [null as boolean | null],
      }),
    },
    { validators: priceRangeValidator },
  );

  get analysisTypeCtrl() {
    return this.form.get('analysisType')!;
  }

  formError = computed(() => {
    if (this.form.errors?.['priceRange']) {
      return 'O preço mínimo não pode ser maior que o preço máximo.';
    }
    if (this.keywords().length === 0) {
      return 'Adicione ao menos uma palavra-chave.';
    }
    return '';
  });

  ngOnInit() {
    this.monitorId = this.route.snapshot.paramMap.get('id');
    if (this.monitorId) {
      this.isEditMode = true;
      this.loadMonitor(this.monitorId);
    }
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.isSaving();
  }

  loadMonitor(id: string) {
    this.isLoading.set(true);
    this.loadError.set(false);

    this.monitorService.getMonitorById(id).subscribe({
      next: (m) => {
        const keywords = (m.searchQueries ?? [])
          .map((q) => q?.queryTerm)
          .filter((k): k is string => !!k);
        this.keywords.set(keywords);

        const first = m.searchQueries?.[0];
        this.form.patchValue({
          name: m.name ?? '',
          description: m.description ?? '',
          vendor: m.targetVendor ?? 'OLX',
          frequency: m.frequency ?? 'EVERY_30_MINUTES',
          minPrice: first?.minPrice ?? null,
          maxPrice: first?.maxPrice ?? null,
          stateFilter: first?.stateFilter ?? '',
          regionFilter: first?.regionFilter ?? '',
          requireDelivery: first?.requireDelivery ?? false,
          analysisType: m.analysisType ?? 'NONE',
        });

        if (m.expectedSpecs) {
          if (m.analysisType === 'SIMPLE') {
            this.form.get('simpleFields')?.patchValue({
              prompt: m.expectedSpecs.prompt ?? '',
            });
          } else if (m.analysisType === 'NOTEBOOK') {
            this.form.get('notebookFields')?.patchValue({
              minimumRamGb: m.expectedSpecs.minimumRamGb ?? 8,
              needsDedicatedGpu:
                m.expectedSpecs.needsDedicatedGpu !== undefined
                  ? m.expectedSpecs.needsDedicatedGpu
                  : null,
            });
          }
        }

        this.form.markAsPristine();
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.loadError.set(true);
      },
    });
  }

  retryLoad() {
    if (this.monitorId) this.loadMonitor(this.monitorId);
  }

  addKeyword() {
    const value = this.keywordInput().trim();
    if (!value) return;
    if (!this.keywords().some((k) => k.toLowerCase() === value.toLowerCase())) {
      this.keywords.update((list) => [...list, value]);
      this.form.markAsDirty();
    }
    this.keywordInput.set('');
  }

  onKeywordInput(event: Event) {
    this.keywordInput.set((event.target as HTMLInputElement).value);
  }

  onKeywordKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.addKeyword();
    }
  }

  removeKeyword(keyword: string) {
    this.keywords.update((list) => list.filter((k) => k !== keyword));
    this.form.markAsDirty();
  }

  onSubmit() {
    this.form.markAllAsTouched();

    if (this.form.invalid || this.keywords().length === 0) {
      this.toast.warning('Atenção', this.formError() || 'Revise os campos destacados.');
      return;
    }

    this.isSaving.set(true);
    const v = this.form.getRawValue();

    let analysisTypeFields: ProductMonitorRequest['analysisTypeFields'];
    if (v.analysisType === 'SIMPLE') {
      analysisTypeFields = { prompt: v.simpleFields?.prompt ?? undefined };
    } else if (v.analysisType === 'NOTEBOOK') {
      analysisTypeFields = {
        minimumRamGb: v.notebookFields?.minimumRamGb ?? undefined,
        needsDedicatedGpu: v.notebookFields?.needsDedicatedGpu,
      };
    }

    const payload: ProductMonitorRequest = {
      name: v.name!,
      description: v.description || undefined,
      vendor: v.vendor as Vendor,
      frequency: v.frequency as ScrapingFrequency,
      analysisType: v.analysisType as AnalysisType,
      analysisTypeFields,
      searchKeywords: this.keywords(),
      minPrice: v.minPrice ?? undefined,
      maxPrice: v.maxPrice ?? undefined,
      stateFilter: v.stateFilter || undefined,
      regionFilter: v.regionFilter || undefined,
      requireDelivery: v.requireDelivery ?? false,
    };

    const request$ = this.isEditMode
      ? this.monitorService.updateMonitor(this.monitorId!, payload)
      : this.monitorService.createMonitor(payload);

    request$.subscribe({
      next: () => {
        this.form.markAsPristine();
        this.toast.success('Sucesso', `Monitor ${this.isEditMode ? 'atualizado' : 'criado'} com sucesso!`);
        this.router.navigate(['/monitors']);
      },
      error: (err) => {
        this.toast.error('Erro ao salvar', err.error?.message || 'Ocorreu um erro inesperado.');
        this.isSaving.set(false);
      },
    });
  }

  cancel() {
    this.router.navigate(['/monitors']);
  }

  isInvalid(controlName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }
}
