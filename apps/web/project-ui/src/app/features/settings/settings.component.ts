import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AvailableAiModelsResponse,
  ModelOptionDto,
  ModelVendor,
  VendorModelsDto,
} from '../../core/models/user-config.model';
import { AuthService } from '../../core/services/auth.service';
import { UserConfigService } from '../../core/services/user-config.service';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiFormFieldComponent } from '../../shared/components/ui-form-field/ui-form-field.component';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { UiStatePanelComponent } from '../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiToastService } from '../../shared/components/ui-toast/ui-toast.service';

interface VendorDisplayOption {
  value: ModelVendor;
  label: string;
  badge: string;
  tagline: string;
  description: string;
  recommendedFor: string;
  iconBg: string;
  accentColor: string;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    UiPageHeaderComponent,
    UiCardComponent,
    UiFormFieldComponent,
    UiButtonComponent,
    UiStatePanelComponent,
  ],
  templateUrl: './settings.component.html',
})
export class SettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userConfigService = inject(UserConfigService);
  private authService = inject(AuthService);
  private toast = inject(UiToastService);

  isLoading = signal(true);
  isSaving = signal(false);
  loadError = signal(false);

  availableModels = signal<AvailableAiModelsResponse | null>(null);
  selectedVendor = signal<ModelVendor>('GEMINI');

  readonly vendorDisplayOptions: VendorDisplayOption[] = [
    {
      value: 'GEMINI',
      label: 'Google Gemini',
      badge: 'Google AI',
      tagline: 'Velocidade extrema e suporte multimodal nativo',
      description:
        'Excelente para triagem rápida, leitura ágil de fotos de anúncios e baixíssima latência na esteira de scraping.',
      recommendedFor: 'Recomendado para monitoramentos em tempo real e alto volume de itens',
      iconBg: 'bg-amber-500/10 text-brand-amber-strong border-brand-amber/20',
      accentColor: 'border-brand-amber ring-2 ring-brand-amber/20 bg-brand-amber/[0.03]',
    },
    {
      value: 'DEEPSEEK',
      label: 'DeepSeek',
      badge: 'DeepSeek AI',
      tagline: 'Raciocínio lógico refinado e alto custo-benefício',
      description:
        'Ideal para análises minuciosas de descrições complexas, desambiguação rigorosa de hardware e filtros profundos.',
      recommendedFor: 'Recomendado para avaliações complexas e especificações difíceis',
      iconBg: 'bg-sky-500/10 text-sky-600 border-sky-500/20',
      accentColor: 'border-sky-500 ring-2 ring-sky-500/20 bg-sky-500/[0.03]',
    },
  ];

  form = this.fb.group({
    aiVendor: ['GEMINI' as ModelVendor, [Validators.required]],
    cheapModel: ['', [Validators.required]],
    strongModel: ['', [Validators.required]],
  });

  currentVendorData = computed<VendorModelsDto | undefined>(() => {
    const data = this.availableModels();
    const vendor = this.selectedVendor();
    if (!data?.vendors) return undefined;
    return data.vendors.find((v) => v.vendor === vendor);
  });

  availableCheapModels = computed<ModelOptionDto[]>(() => {
    return this.currentVendorData()?.cheapModels ?? [];
  });

  availableStrongModels = computed<ModelOptionDto[]>(() => {
    return this.currentVendorData()?.strongModels ?? [];
  });

  ngOnInit() {
    this.loadData();

    this.form.get('aiVendor')?.valueChanges.subscribe((vendor) => {
      if (vendor && (vendor === 'GEMINI' || vendor === 'DEEPSEEK')) {
        this.selectedVendor.set(vendor);
        this.autoSelectModelsForVendor(vendor);
      }
    });
  }

  loadData() {
    this.isLoading.set(true);
    this.loadError.set(false);

    this.userConfigService.getAvailableModels().subscribe({
      next: (response) => {
        this.availableModels.set(response);

        const currentConfig = this.authService.currentUser()?.config;
        if (currentConfig) {
          const vendor: ModelVendor = currentConfig.aiVendor === 'DEEPSEEK' ? 'DEEPSEEK' : 'GEMINI';
          this.selectedVendor.set(vendor);
          this.form.patchValue({
            aiVendor: vendor,
            cheapModel: currentConfig.cheapModel || '',
            strongModel: currentConfig.strongModel || '',
          });
          this.ensureValidSelection(vendor);
        } else {
          this.authService.loadMe().subscribe((user) => {
            const config = user?.config;
            const vendor: ModelVendor = config?.aiVendor === 'DEEPSEEK' ? 'DEEPSEEK' : 'GEMINI';
            this.selectedVendor.set(vendor);
            this.form.patchValue({
              aiVendor: vendor,
              cheapModel: config?.cheapModel || '',
              strongModel: config?.strongModel || '',
            });
            this.ensureValidSelection(vendor);
          });
        }

        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar catálogo de modelos IA:', err);
        this.loadError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  selectVendor(vendor: ModelVendor) {
    if (this.form.get('aiVendor')?.value !== vendor) {
      this.form.patchValue({ aiVendor: vendor });
      this.form.markAsDirty();
    }
  }

  private ensureValidSelection(vendor: ModelVendor) {
    const vendorData = this.availableModels()?.vendors?.find((v) => v.vendor === vendor);
    if (!vendorData) return;

    const currentCheap = this.form.get('cheapModel')?.value;
    const currentStrong = this.form.get('strongModel')?.value;

    const cheapValid = vendorData.cheapModels.some((m) => m.id === currentCheap);
    const strongValid = vendorData.strongModels.some((m) => m.id === currentStrong);

    this.form.patchValue({
      cheapModel: cheapValid ? currentCheap : vendorData.cheapModels[0]?.id || '',
      strongModel: strongValid ? currentStrong : vendorData.strongModels[0]?.id || '',
    });
  }

  private autoSelectModelsForVendor(vendor: ModelVendor) {
    const vendorData = this.availableModels()?.vendors?.find((v) => v.vendor === vendor);
    if (!vendorData) return;

    const currentCheap = this.form.get('cheapModel')?.value;
    const currentStrong = this.form.get('strongModel')?.value;

    const cheapValid = vendorData.cheapModels.some((m) => m.id === currentCheap);
    const strongValid = vendorData.strongModels.some((m) => m.id === currentStrong);

    this.form.patchValue({
      cheapModel: cheapValid ? currentCheap : vendorData.cheapModels[0]?.id || '',
      strongModel: strongValid ? currentStrong : vendorData.strongModels[0]?.id || '',
    });
  }

  onSubmit() {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.toast.warning('Atenção', 'Selecione os modelos válidos para continuar.');
      return;
    }

    this.isSaving.set(true);
    const formValue = this.form.getRawValue();

    this.userConfigService
      .updateConfig({
        aiVendor: formValue.aiVendor!,
        cheapModel: formValue.cheapModel!,
        strongModel: formValue.strongModel!,
      })
      .subscribe({
        next: (updatedConfig) => {
          this.isSaving.set(false);
          this.form.markAsPristine();

          const currentUser = this.authService.currentUser();
          if (currentUser) {
            this.authService.currentUser.set({
              ...currentUser,
              config: updatedConfig,
            });
          }

          this.toast.success('Sucesso', 'Configurações de IA salvas com sucesso!');
        },
        error: (err) => {
          this.isSaving.set(false);
          console.error('Erro ao salvar configurações de IA:', err);
          this.toast.error(
            'Erro ao salvar',
            err?.error?.message || 'Não foi possível salvar as configurações.',
          );
        },
      });
  }
}
