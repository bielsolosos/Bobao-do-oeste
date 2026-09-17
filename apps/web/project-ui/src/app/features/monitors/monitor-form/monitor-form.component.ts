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
  DiskType,
  NotebookBrand,
  ProcessorBrand,
  ProcessorTier,
  ProductMonitorRequest,
  RamType,
  ScrapingFrequency,
  ScrapingFrequencyOption,
  ScreenResolution,
  Vendor,
} from '../../../core/models/monitor.model';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiFormFieldComponent } from '../../../shared/components/ui-form-field/ui-form-field.component';
import {
  MultiSelectOption,
  UiMultiSelectComponent,
} from '../../../shared/components/ui-multiselect/ui-multiselect.component';
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
    UiMultiSelectComponent,
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

  frequencies = signal<ScrapingFrequencyOption[]>([
    { name: 'EVERY_MINUTE', description: 'A cada 1 minuto (agressivo)' },
    { name: 'EVERY_5_MINUTES', description: 'A cada 5 minutos' },
    { name: 'EVERY_30_MINUTES', description: 'A cada 30 minutos' },
    { name: 'HOURLY', description: 'A cada hora' },
    { name: 'EIGHT_TIMES_DAILY', description: '8 vezes ao dia (a cada 3 horas)' },
    { name: 'SIX_TIMES_DAILY', description: '6 vezes ao dia (a cada 4 horas)' },
    { name: 'FOUR_TIMES_DAILY', description: '4 vezes ao dia (a cada 6 horas)' },
    { name: 'DAILY', description: 'Uma vez ao dia' },
    { name: 'TWICE_DAILY', description: 'Duas vezes ao dia' },
    { name: 'WEEKLY', description: 'Semanal' },
    { name: 'MANUAL', description: 'Somente manual' },
  ]);

  // --- Opções para MultiSelects de Notebook ---
  readonly notebookBrandOptions: MultiSelectOption<NotebookBrand>[] = [
    { label: 'Apple', value: 'APPLE', badge: 'MacBook' },
    { label: 'Dell', value: 'DELL', badge: 'XPS / Inspiron / G15' },
    { label: 'Lenovo', value: 'LENOVO', badge: 'ThinkPad / Legion / IdeaPad' },
    { label: 'Acer', value: 'ACER', badge: 'Nitro / Predator / Aspire' },
    { label: 'Asus', value: 'ASUS', badge: 'ROG / TUF / ZenBook' },
    { label: 'HP', value: 'HP', badge: 'Victus / Pavilion / Omen' },
    { label: 'Samsung', value: 'SAMSUNG', badge: 'Galaxy Book' },
    { label: 'Avell', value: 'AVELL', badge: 'Workstation / Gamer' },
    { label: 'LG', value: 'LG', badge: 'Gram' },
    { label: 'Vaio', value: 'VAIO' },
    { label: 'MSI', value: 'MSI', badge: 'Gamer' },
    { label: 'Alienware', value: 'ALIENWARE', badge: 'Ultra Gamer' },
    { label: 'Outra marca', value: 'OTHER' },
  ];

  readonly processorVendorOptions: MultiSelectOption<ProcessorBrand>[] = [
    { label: 'Intel', value: 'INTEL', description: 'Core i3, i5, i7, i9 e Core Ultra' },
    { label: 'AMD', value: 'AMD', description: 'Ryzen 3, 5, 7, 9 e AI' },
    { label: 'Apple Silicon', value: 'APPLE', description: 'Chips M1, M2, M3, M4 (Base, Pro, Max)' },
    { label: 'Qualcomm', value: 'QUALCOMM', description: 'Snapdragon X Plus e X Elite (ARM)' },
  ];

  readonly processorTierOptions: MultiSelectOption<ProcessorTier>[] = [
    {
      label: 'Básico / Uso Leve',
      value: 'ENTRY',
      description: 'Navegação, estudos e escritório básico (Core i3, Ryzen 3, N100)',
      badge: 'i3 / R3',
    },
    {
      label: 'Intermediário / Produtividade',
      value: 'INTERMEDIATE',
      description: 'Trabalho diário, multitarefa e programação (Core i5, Ryzen 5, M1/M2/M3 base, Ultra 5)',
      badge: 'i5 / R5 / M1',
    },
    {
      label: 'Alto Desempenho / Pesado',
      value: 'ADVANCED',
      description: 'Jogos pesados, render 3D e edição de vídeo (Core i7/i9, Ryzen 7/9, M Pro/Max/Ultra)',
      badge: 'i7 / i9 / R7 / M Pro',
    },
  ];

  readonly ramTypeOptions: MultiSelectOption<RamType>[] = [
    { label: 'DDR4', value: 'DDR4', description: 'Padrão tradicional de mercado' },
    { label: 'DDR5', value: 'DDR5', badge: 'Mais rápida', description: 'Alta velocidade e eficiência' },
    { label: 'LPDDR5 / LPDDR5X', value: 'LPDDR5', badge: 'Ultrabooks / Mac', description: 'Baixo consumo de energia' },
    { label: 'LPDDR4 / LPDDR4X', value: 'LPDDR4', description: 'Ultrabooks compactos' },
    { label: 'DDR3', value: 'DDR3', badge: 'Antiga', description: 'Notebooks mais antigos' },
  ];

  readonly diskTypeOptions: MultiSelectOption<DiskType>[] = [
    { label: 'SSD NVMe / M.2', value: 'SSD_NVME', badge: 'Ultra Rápido', description: 'Leituras ultra-rápidas acima de 2000MB/s' },
    { label: 'SSD SATA', value: 'SSD_SATA', description: 'SSD convencional 2.5" de alta confiabilidade' },
    { label: 'SSD (Genérico)', value: 'SSD', description: 'Qualquer tecnologia SSD' },
    { label: 'HD Mecânico', value: 'HDD', description: 'Disco rígido tradicional de grande capacidade' },
    { label: 'eMMC Flash', value: 'EMMC', description: 'Armazenamento flash básico integrado' },
  ];

  readonly screenResolutionOptions: MultiSelectOption<ScreenResolution>[] = [
    { label: 'Full HD (1080p)', value: 'FULL_HD', badge: '1920x1080', description: 'Padrão nítido mais comum' },
    { label: '2K / QHD', value: 'QHD_2K', badge: '2560x1440', description: 'Alta definição e amplo espaço visual' },
    { label: 'Retina / Liquid Retina', value: 'RETINA', badge: 'Apple', description: 'Telas de altíssima densidade de pixels' },
    { label: 'WUXGA (16:10)', value: 'WUXGA', badge: '1920x1200', description: 'Excelente para produtividade vertical' },
    { label: '2.5K / WQXGA (16:10)', value: 'WQXGA_2K', badge: '2560x1600', description: 'Display premium de trabalho e games' },
    { label: '4K Ultra HD', value: 'UHD_4K', badge: '3840x2160', description: 'Resolução máxima para edição visual' },
    { label: 'HD (720p)', value: 'HD', badge: '1366x768', description: 'Resolução básica de entrada' },
  ];

  readonly ramQuickOptions = [4, 8, 16, 32, 64];
  readonly storageQuickOptions = [
    { label: '128 GB', value: 128 },
    { label: '256 GB', value: 256 },
    { label: '512 GB', value: 512 },
    { label: '1 TB', value: 1024 },
    { label: '2 TB', value: 2048 },
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
        brands: [[] as NotebookBrand[]],
        processorVendors: [[] as ProcessorBrand[]],
        processorTiers: [[] as ProcessorTier[]],
        minimumProcessorGeneration: [null as number | null, [Validators.min(1)]],
        minimumRamGb: [null as number | null, [Validators.min(4)]],
        ramTypes: [[] as RamType[]],
        minimumStorageGb: [null as number | null, [Validators.min(64)]],
        diskTypes: [[] as DiskType[]],
        screenResolutions: [[] as ScreenResolution[]],
        needsDedicatedGpu: [null as boolean | null],
      }),
    },
    { validators: priceRangeValidator },
  );

  get analysisTypeCtrl() {
    return this.form.get('analysisType')!;
  }

  get notebookGroup() {
    return this.form.get('notebookFields')!;
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
    this.loadFrequencies();
    this.monitorId = this.route.snapshot.paramMap.get('id');
    if (this.monitorId) {
      this.isEditMode = true;
      this.loadMonitor(this.monitorId);
    }
  }

  loadFrequencies() {
    this.monitorService.getFrequencies().subscribe({
      next: (options) => {
        if (options && options.length > 0) {
          this.frequencies.set(options);
        }
      },
      error: (err) => console.warn('Usando frequências padrão locais:', err),
    });
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
              brands: m.expectedSpecs.brands ?? [],
              processorVendors: m.expectedSpecs.processorVendors ?? [],
              processorTiers: m.expectedSpecs.processorTiers ?? [],
              minimumProcessorGeneration: m.expectedSpecs.minimumProcessorGeneration ?? null,
              minimumRamGb: m.expectedSpecs.minimumRamGb ?? null,
              ramTypes: m.expectedSpecs.ramTypes ?? [],
              minimumStorageGb: m.expectedSpecs.minimumStorageGb ?? null,
              diskTypes: m.expectedSpecs.diskTypes ?? [],
              screenResolutions: m.expectedSpecs.screenResolutions ?? [],
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

  setQuickRam(gb: number) {
    const ctrl = this.notebookGroup.get('minimumRamGb');
    if (ctrl?.value === gb) {
      ctrl.setValue(null);
    } else {
      ctrl?.setValue(gb);
    }
    this.form.markAsDirty();
  }

  setQuickStorage(gb: number) {
    const ctrl = this.notebookGroup.get('minimumStorageGb');
    if (ctrl?.value === gb) {
      ctrl.setValue(null);
    } else {
      ctrl?.setValue(gb);
    }
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
      const nf = v.notebookFields;
      analysisTypeFields = {
        brands: nf?.brands?.length ? nf.brands : undefined,
        processorVendors: nf?.processorVendors?.length ? nf.processorVendors : undefined,
        processorTiers: nf?.processorTiers?.length ? nf.processorTiers : undefined,
        minimumProcessorGeneration: nf?.minimumProcessorGeneration ?? undefined,
        minimumRamGb: nf?.minimumRamGb ?? undefined,
        ramTypes: nf?.ramTypes?.length ? nf.ramTypes : undefined,
        minimumStorageGb: nf?.minimumStorageGb ?? undefined,
        diskTypes: nf?.diskTypes?.length ? nf.diskTypes : undefined,
        screenResolutions: nf?.screenResolutions?.length ? nf.screenResolutions : undefined,
        needsDedicatedGpu: nf?.needsDedicatedGpu,
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
        this.toast.success(
          'Sucesso',
          `Monitor ${this.isEditMode ? 'atualizado' : 'criado'} com sucesso!`,
        );
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
