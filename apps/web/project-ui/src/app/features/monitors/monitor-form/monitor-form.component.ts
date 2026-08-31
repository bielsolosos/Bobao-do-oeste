import { Component, inject, OnInit, signal } from '@angular/core';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MonitorService } from '../../../core/services/monitor.service';
import { ProductMonitorRequest, AnalysisType } from '../../../core/models/monitor.model';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-monitor-form',
  standalone: true,
  imports: [ReactiveFormsModule, UiButtonComponent, UiCardComponent],
  template: `
    <div class="mb-6 flex justify-between items-end">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEditMode ? 'Editar Monitor' : 'Novo Monitor' }}
        </h1>
        <p class="text-sm text-gray-500 mt-1">
          Configure os parâmetros da sua busca e análise de IA.
        </p>
      </div>
    </div>

    <div class="max-w-4xl">
      <app-ui-card [noPadding]="true">
        <!-- Estado de Carregamento para Edição -->
        @if (isLoading()) {
          <div class="p-12 text-center">
            <svg
              class="animate-spin mx-auto h-8 w-8 text-blue-600"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                class="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                stroke-width="4"
              ></circle>
              <path
                class="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
              ></path>
            </svg>
            <p class="mt-4 text-sm text-gray-500">Carregando dados do monitor...</p>
          </div>
        }

        @if (!isLoading()) {
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="px-6 py-6">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- Seção Básica -->
                <div class="md:col-span-2">
                  <h3
                    class="text-sm font-semibold text-gray-900 uppercase tracking-wider border-b pb-2 mb-4"
                  >
                    Informações Gerais
                  </h3>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1"
                        >Nome da Busca <span class="text-red-500">*</span></label
                      >
                      <input
                        formControlName="name"
                        type="text"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 bg-white"
                        placeholder="Ex: Macbooks M1 baratos"
                      />
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1"
                        >Plataforma Alvo <span class="text-red-500">*</span></label
                      >
                      <select
                        formControlName="vendor"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 bg-white"
                      >
                        <option value="MERCADO_LIVRE">Mercado Livre</option>
                        <option value="OLX">OLX</option>
                      </select>
                    </div>
                  </div>
                  <div class="mt-4">
                    <label class="block text-sm font-medium text-gray-700 mb-1">Descrição</label>
                    <textarea
                      formControlName="description"
                      rows="2"
                      class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 bg-white"
                      placeholder="Opcional. Breve nota sobre a busca."
                    ></textarea>
                  </div>
                </div>
                <!-- Seção de Coleta -->
                <div class="md:col-span-2 mt-2">
                  <h3
                    class="text-sm font-semibold text-gray-900 uppercase tracking-wider border-b pb-2 mb-4"
                  >
                    Regras de Coleta
                  </h3>
                  <div class="grid grid-cols-1 gap-4">
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1"
                        >Palavras-chave (separadas por vírgula)
                        <span class="text-red-500">*</span></label
                      >
                      <input
                        formControlName="searchKeywords"
                        type="text"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 font-mono text-sm bg-white"
                        placeholder="macbook air m1, macbook m1 8gb, macbook pro m1"
                      />
                      <p class="text-xs text-gray-500 mt-1">
                        Cada palavra será buscada independentemente.
                      </p>
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1"
                        >Frequência de Busca <span class="text-red-500">*</span></label
                      >
                      <select
                        formControlName="frequency"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 bg-white"
                      >
                        <option value="EVERY_MINUTE">A cada 1 minuto (Agressivo)</option>
                        <option value="EVERY_5_MINUTES">A cada 5 minutos</option>
                        <option value="EVERY_30_MINUTES">A cada 30 minutos</option>
                        <option value="HOURLY">1 vez por hora</option>
                        <option value="DAILY">1 vez por dia</option>
                      </select>
                    </div>
                  </div>
                  <div class="grid grid-cols-2 gap-4 mt-4">
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1"
                        >Preço Mínimo (R$)</label
                      >
                      <input
                        formControlName="minPrice"
                        type="number"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 bg-white"
                        placeholder="0"
                      />
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1"
                        >Preço Máximo (R$)</label
                      >
                      <input
                        formControlName="maxPrice"
                        type="number"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 bg-white"
                        placeholder="99999"
                      />
                    </div>
                  </div>
                </div>
                <!-- Seção de IA -->
                <div class="md:col-span-2 mt-2">
                  <h3
                    class="text-sm font-semibold text-gray-900 uppercase tracking-wider border-b pb-2 mb-4"
                  >
                    Inteligência Artificial & Filtros
                  </h3>
                  <div class="grid grid-cols-1 gap-4">
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1"
                        >Estratégia de Análise (Gemini) <span class="text-red-500">*</span></label
                      >
                      <select
                        formControlName="analysisType"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 bg-white"
                      >
                        <option value="NONE">Desabilitada (Coleta Bruta)</option>
                        <option value="SIMPLE">Prompt Personalizado Simples</option>
                        <option value="NOTEBOOK">Extrator Especializado: Notebooks</option>
                      </select>
                    </div>
                    <!-- Campos Dinâmicos: SIMPLE -->
                    @if (analysisTypeCtrl.value === 'SIMPLE') {
                      <div
                        class="bg-blue-50/50 p-4 rounded-md border border-blue-100"
                        formGroupName="simpleFields"
                      >
                        <label class="block text-sm font-medium text-blue-900 mb-1"
                          >System Prompt / Regras de Negócio</label
                        >
                        <textarea
                          formControlName="prompt"
                          class="w-full px-3 py-2 border border-blue-200 rounded-md focus:ring-blue-500 focus:border-blue-500 font-mono text-sm bg-white"
                          rows="4"
                          placeholder="Ex: Descarte qualquer anúncio que diga 'com defeito' ou 'tela trincada'. Você deve classificar como HIGH se for 16GB."
                        ></textarea>
                      </div>
                    }
                    <!-- Campos Dinâmicos: NOTEBOOK -->
                    @if (analysisTypeCtrl.value === 'NOTEBOOK') {
                      <div
                        class="bg-purple-50/50 p-4 rounded-md border border-purple-100 grid grid-cols-2 gap-4"
                        formGroupName="notebookFields"
                      >
                        <div>
                          <label class="block text-sm font-medium text-purple-900 mb-1"
                            >Memória RAM Mínima (GB)</label
                          >
                          <input
                            formControlName="minimumRamGb"
                            type="number"
                            class="w-full px-3 py-2 border border-purple-200 rounded-md focus:ring-purple-500 focus:border-purple-500 bg-white"
                          />
                        </div>
                        <div>
                          <label class="block text-sm font-medium text-purple-900 mb-1"
                            >GPU Dedicada Obrigatória?</label
                          >
                          <select
                            formControlName="needsDedicatedGpu"
                            class="w-full px-3 py-2 border border-purple-200 rounded-md focus:ring-purple-500 focus:border-purple-500 bg-white"
                          >
                            <option [ngValue]="null">Não Importa</option>
                            <option [ngValue]="true">Sim, exigir placa de vídeo</option>
                            <option [ngValue]="false">Não precisa</option>
                          </select>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              </div>
            </div>
            <div class="bg-gray-50 px-6 py-4 border-t flex justify-end gap-3 rounded-b-xl">
              <app-ui-button
                type="button"
                variant="outline"
                (onClick)="router.navigate(['/monitors'])"
              >
                Cancelar
              </app-ui-button>
              <app-ui-button
                type="submit"
                variant="primary"
                [loading]="isSaving()"
                [disabled]="form.invalid || isLoading()"
              >
                {{ isEditMode ? 'Salvar Alterações' : 'Criar Monitor' }}
              </app-ui-button>
            </div>
          </form>
        }
      </app-ui-card>
    </div>
  `,
})
export class MonitorFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private monitorService = inject(MonitorService);
  public router = inject(Router);
  private route = inject(ActivatedRoute);
  private toast = inject(UiToastService);

  isSaving = signal(false);
  isLoading = signal(false);
  isEditMode = false;
  monitorId: string | null = null;

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    vendor: ['MERCADO_LIVRE', Validators.required],
    frequency: ['EVERY_30_MINUTES', Validators.required],
    searchKeywords: ['', Validators.required],
    minPrice: [null as number | null],
    maxPrice: [null as number | null],
    analysisType: ['NONE' as AnalysisType, Validators.required],
    simpleFields: this.fb.group({
      prompt: [''],
    }),
    notebookFields: this.fb.group({
      minimumRamGb: [8],
      needsDedicatedGpu: [null as boolean | null],
    }),
  });

  get analysisTypeCtrl() {
    return this.form.get('analysisType')!;
  }

  ngOnInit() {
    this.monitorId = this.route.snapshot.paramMap.get('id');
    if (this.monitorId) {
      this.isEditMode = true;
      this.loadMonitor(this.monitorId);
    }
  }

  loadMonitor(id: string) {
    this.isLoading.set(true);

    this.monitorService.getMonitorById(id).subscribe({
      next: (m) => {
        try {
          console.log('Monitor carregado da API:', m);

          let keywords = '';
          if (m.searchQueries && Array.isArray(m.searchQueries)) {
            keywords = m.searchQueries
              .map((q) => q?.keyword)
              .filter((k) => !!k)
              .join(', ');
          }

          let minPrice = null;
          let maxPrice = null;
          if (m.searchQueries && m.searchQueries.length > 0) {
            minPrice = (m.searchQueries[0] as any)?.minPrice || null;
            maxPrice = (m.searchQueries[0] as any)?.maxPrice || null;
          }

          this.form.patchValue({
            name: m.name || '',
            description: m.description || '',
            vendor: m.targetVendor || 'MERCADO_LIVRE',
            frequency: m.frequency || 'EVERY_30_MINUTES',
            searchKeywords: keywords,
            analysisType: m.analysisType || 'NONE',
            minPrice: minPrice,
            maxPrice: maxPrice,
          });

          // Preenche campos dinâmicos se existirem
          if (m.analysisType === 'SIMPLE' && m.expectedSpecs) {
            this.form.get('simpleFields')?.patchValue({
              prompt: m.expectedSpecs.prompt || '',
            });
          } else if (m.analysisType === 'NOTEBOOK' && m.expectedSpecs) {
            this.form.get('notebookFields')?.patchValue({
              minimumRamGb: m.expectedSpecs.minimumRamGb || 8,
              needsDedicatedGpu:
                m.expectedSpecs.needsDedicatedGpu !== undefined
                  ? m.expectedSpecs.needsDedicatedGpu
                  : null,
            });
          }

          this.isLoading.set(false);
        } catch (err) {
          console.error('Erro fatal ao processar JSON do Monitor:', err);
          this.isLoading.set(false);
          this.toast.error('Erro interno', 'Houve um erro ao processar os dados deste monitor.');
        }
      },
      error: (err) => {
        console.error('Falha na API GET Monitor:', err);
        this.isLoading.set(false);
        this.toast.error('Erro de Conexão', 'Não foi possível buscar este monitor no servidor.');
        // Para debug local mais facil, desativamos o redirect na falha, pra tela nao "piscar".
      },
    });
  }

  onSubmit() {
    if (this.form.invalid) {
      this.toast.warning('Atenção', 'Preencha todos os campos obrigatórios.');
      return;
    }

    this.isSaving.set(true);
    const v = this.form.value;

    let analysisTypeFields: any = null;
    if (v.analysisType === 'SIMPLE') {
      analysisTypeFields = { prompt: v.simpleFields?.prompt };
    } else if (v.analysisType === 'NOTEBOOK') {
      analysisTypeFields = {
        minimumRamGb: v.notebookFields?.minimumRamGb,
        needsDedicatedGpu: v.notebookFields?.needsDedicatedGpu,
      };
    }

    const keywordsArray = ((v.searchKeywords as string) || '')
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);

    const payload: ProductMonitorRequest = {
      name: v.name!,
      description: v.description || undefined,
      vendor: v.vendor as any,
      frequency: v.frequency as any,
      analysisType: v.analysisType as AnalysisType,
      analysisTypeFields,
      searchKeywords: keywordsArray,
      minPrice: v.minPrice || undefined,
      maxPrice: v.maxPrice || undefined,
    };

    const request$ = this.isEditMode
      ? this.monitorService.updateMonitor(this.monitorId!, payload)
      : this.monitorService.createMonitor(payload);

    request$.subscribe({
      next: () => {
        this.toast.success(
          'Sucesso',
          `Monitor ${this.isEditMode ? 'atualizado' : 'criado'} com sucesso!`,
        );
        this.router.navigate(['/monitors']);
      },
      error: (err) => {
        this.toast.error('Erro ao salvar', err.error?.message || 'Ocorreu um erro inesperado.');
        console.error(err);
        this.isSaving.set(false);
      },
    });
  }
}
