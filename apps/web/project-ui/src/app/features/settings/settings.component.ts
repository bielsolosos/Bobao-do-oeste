import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AvailableAiModelsResponse, LlmModelOption, ModelVendor } from '../../core/models/user-config.model';
import { AuthService } from '../../core/services/auth.service';
import { UserConfigService } from '../../core/services/user-config.service';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiFormFieldComponent } from '../../shared/components/ui-form-field/ui-form-field.component';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { UiStatePanelComponent } from '../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiToastService } from '../../shared/components/ui-toast/ui-toast.service';

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

  form = this.fb.group({
    aiVendor: ['GEMINI' as ModelVendor, [Validators.required]],
    cheapModel: ['', [Validators.required]],
    strongModel: ['', [Validators.required]],
  });

  selectedVendor = signal<ModelVendor>('GEMINI');

  vendorOptions: { value: ModelVendor; label: string; description: string; badge: string; icon: string }[] = [
    {
      value: 'GEMINI',
      label: 'Google Gemini',
      description: 'Modelos multimodais de alta velocidade e contexto ultra-amplo.',
      badge: 'Google',
      icon: 'sparkles',
    },
    {
      value: 'DEEPSEEK',
      label: 'DeepSeek',
      description: 'Modelos de raciocínio profundo e excelente relação custo-benefício.',
      badge: 'DeepSeek',
      icon: 'cpu-chip',
    },
    {
      value: 'OLLAMA',
      label: 'Ollama (Local / On-Premise)',
      description: 'Execução de modelos open-source locais para privacidade total.',
      badge: 'Self-Hosted',
      icon: 'server',
    },
  ];

  availableCheapModels = computed<LlmModelOption[]>(() => {
    const models = this.availableModels();
    const vendor = this.selectedVendor();
    if (!models || !models.modelsByVendor || !models.modelsByVendor[vendor]) {
      return [];
    }
    return models.modelsByVendor[vendor].filter((m) => m.tier === 'CHEAP');
  });

  availableStrongModels = computed<LlmModelOption[]>(() => {
    const models = this.availableModels();
    const vendor = this.selectedVendor();
    if (!models || !models.modelsByVendor || !models.modelsByVendor[vendor]) {
      return [];
    }
    return models.modelsByVendor[vendor].filter((m) => m.tier === 'STRONG');
  });

  ngOnInit() {
    this.loadData();

    this.form.get('aiVendor')?.valueChanges.subscribe((vendor) => {
      if (vendor) {
        this.selectedVendor.set(vendor as ModelVendor);
        this.autoSelectModelsForVendor(vendor as ModelVendor);
      }
    });
  }

  loadData() {
    this.isLoading.set(true);
    this.loadError.set(false);

    this.userConfigService.getAvailableModels().subscribe({
      next: (models) => {
        this.availableModels.set(models);

        const currentConfig = this.authService.currentUser()?.config;
        if (currentConfig) {
          this.selectedVendor.set(currentConfig.aiVendor || 'GEMINI');
          this.form.patchValue({
            aiVendor: currentConfig.aiVendor || 'GEMINI',
            cheapModel: currentConfig.cheapModel || '',
            strongModel: currentConfig.strongModel || '',
          });
        } else {
          this.authService.loadMe().subscribe((user) => {
            const config = user?.config;
            const vendor = config?.aiVendor || 'GEMINI';
            this.selectedVendor.set(vendor);
            this.form.patchValue({
              aiVendor: vendor,
              cheapModel: config?.cheapModel || '',
              strongModel: config?.strongModel || '',
            });
            if (!config?.cheapModel || !config?.strongModel) {
              this.autoSelectModelsForVendor(vendor);
            }
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
    this.form.patchValue({ aiVendor: vendor });
    this.form.markAsDirty();
  }

  private autoSelectModelsForVendor(vendor: ModelVendor) {
    const models = this.availableModels();
    if (!models || !models.modelsByVendor || !models.modelsByVendor[vendor]) return;

    const vendorModels = models.modelsByVendor[vendor];
    const cheap = vendorModels.find((m) => m.tier === 'CHEAP')?.modelId || vendorModels[0]?.modelId || '';
    const strong = vendorModels.find((m) => m.tier === 'STRONG')?.modelId || vendorModels[0]?.modelId || '';

    const currentCheap = this.form.get('cheapModel')?.value;
    const currentStrong = this.form.get('strongModel')?.value;

    const cheapExists = vendorModels.some((m) => m.modelId === currentCheap && m.tier === 'CHEAP');
    const strongExists = vendorModels.some((m) => m.modelId === currentStrong && m.tier === 'STRONG');

    this.form.patchValue({
      cheapModel: cheapExists ? currentCheap : cheap,
      strongModel: strongExists ? currentStrong : strong,
    });
  }

  onSubmit() {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.toast.warning('Atenção', 'Preencha todos os campos obrigatórios.');
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
          this.toast.error('Erro ao salvar', err?.error?.message || 'Não foi possível salvar as configurações.');
        },
      });
  }
}
