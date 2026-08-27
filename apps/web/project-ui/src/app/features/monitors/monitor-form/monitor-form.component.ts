import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MonitorService } from '../../../core/services/monitor.service';
import { ProductMonitorRequest, AnalysisType } from '../../../core/models/monitor.model';

@Component({
  selector: 'app-monitor-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="max-w-2xl mx-auto bg-white rounded-lg shadow-sm border border-gray-100 p-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-800">Novo Monitor</h2>
        <a routerLink="/monitors" class="text-gray-500 hover:text-gray-700">Voltar</a>
      </div>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-6">
        <div class="grid grid-cols-2 gap-4">
          <div class="col-span-2">
            <label class="block text-sm font-medium text-gray-700 mb-1">Nome do Monitor</label>
            <input formControlName="name" type="text" class="w-full px-4 py-2 border border-gray-300 rounded outline-none focus:border-blue-500" placeholder="Ex: Macbooks 16GB" />
          </div>

          <div class="col-span-2">
            <label class="block text-sm font-medium text-gray-700 mb-1">Descrição</label>
            <textarea formControlName="description" class="w-full px-4 py-2 border border-gray-300 rounded outline-none focus:border-blue-500" rows="2"></textarea>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Plataforma (Vendor)</label>
            <select formControlName="vendor" class="w-full px-4 py-2 border border-gray-300 rounded outline-none focus:border-blue-500">
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="OLX">OLX</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Frequência</label>
            <select formControlName="frequency" class="w-full px-4 py-2 border border-gray-300 rounded outline-none focus:border-blue-500">
              <option value="EVERY_MINUTE">A cada minuto</option>
              <option value="EVERY_5_MINUTES">A cada 5 minutos</option>
              <option value="EVERY_30_MINUTES">A cada 30 minutos</option>
              <option value="HOURLY">A cada hora</option>
              <option value="DAILY">Diariamente</option>
              <option value="MANUAL">Manual</option>
            </select>
          </div>

          <div class="col-span-2">
            <label class="block text-sm font-medium text-gray-700 mb-1">Palavras-chave (separadas por vírgula)</label>
            <input formControlName="searchKeywords" type="text" class="w-full px-4 py-2 border border-gray-300 rounded outline-none focus:border-blue-500" placeholder="Ex: macbook pro m1, macbook air" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Preço Mínimo (Opcional)</label>
            <input formControlName="minPrice" type="number" class="w-full px-4 py-2 border border-gray-300 rounded outline-none focus:border-blue-500" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Preço Máximo (Opcional)</label>
            <input formControlName="maxPrice" type="number" class="w-full px-4 py-2 border border-gray-300 rounded outline-none focus:border-blue-500" />
          </div>

          <div class="col-span-2 mt-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">Tipo de Análise (AI)</label>
            <select formControlName="analysisType" class="w-full px-4 py-2 border border-gray-300 rounded outline-none focus:border-blue-500">
              <option value="NONE">Nenhuma Análise</option>
              <option value="SIMPLE">Prompt Simples</option>
              <option value="NOTEBOOK">Análise de Notebook Específica</option>
            </select>
          </div>
        </div>

        <!-- Campos Dinâmicos: SIMPLE -->
        <div *ngIf="analysisTypeCtrl.value === 'SIMPLE'" class="bg-blue-50 p-4 rounded mt-4" formGroupName="simpleFields">
          <label class="block text-sm font-medium text-blue-900 mb-1">Prompt de Avaliação</label>
          <textarea formControlName="prompt" class="w-full px-4 py-2 border border-blue-200 rounded outline-none focus:border-blue-500" rows="3" placeholder="Instruções para a IA..."></textarea>
        </div>

        <!-- Campos Dinâmicos: NOTEBOOK -->
        <div *ngIf="analysisTypeCtrl.value === 'NOTEBOOK'" class="bg-purple-50 p-4 rounded mt-4 grid grid-cols-2 gap-4" formGroupName="notebookFields">
          <div>
            <label class="block text-sm font-medium text-purple-900 mb-1">RAM Mínima (GB)</label>
            <input formControlName="minimumRamGb" type="number" class="w-full px-4 py-2 border border-purple-200 rounded outline-none focus:border-purple-500" />
          </div>
          <div>
            <label class="block text-sm font-medium text-purple-900 mb-1">GPU Dedicada?</label>
            <select formControlName="needsDedicatedGpu" class="w-full px-4 py-2 border border-purple-200 rounded outline-none focus:border-purple-500">
              <option [ngValue]="null">Não Importa</option>
              <option [ngValue]="true">Sim</option>
              <option [ngValue]="false">Não</option>
            </select>
          </div>
        </div>

        <button type="submit" [disabled]="form.invalid || isSaving" class="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded transition-colors disabled:opacity-50">
          {{ isSaving ? 'Salvando...' : 'Salvar Monitor' }}
        </button>
      </form>
    </div>
  `
})
export class MonitorFormComponent {
  private fb = inject(FormBuilder);
  private monitorService = inject(MonitorService);
  private router = inject(Router);

  isSaving = false;

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    vendor: ['MERCADO_LIVRE', Validators.required],
    frequency: ['EVERY_30_MINUTES', Validators.required],
    searchKeywords: ['', Validators.required], // vamos fazer o split depois
    minPrice: [null],
    maxPrice: [null],
    analysisType: ['NONE' as AnalysisType, Validators.required],
    simpleFields: this.fb.group({
      prompt: ['']
    }),
    notebookFields: this.fb.group({
      minimumRamGb: [8],
      needsDedicatedGpu: [null]
    })
  });

  get analysisTypeCtrl() {
    return this.form.get('analysisType')!;
  }

  onSubmit() {
    if (this.form.invalid) return;

    this.isSaving = true;
    const v = this.form.value;

    let analysisTypeFields: any = null;
    if (v.analysisType === 'SIMPLE') {
      analysisTypeFields = { prompt: v.simpleFields?.prompt };
    } else if (v.analysisType === 'NOTEBOOK') {
      analysisTypeFields = { 
        minimumRamGb: v.notebookFields?.minimumRamGb,
        needsDedicatedGpu: v.notebookFields?.needsDedicatedGpu
      };
    }

    const payload: ProductMonitorRequest = {
      name: v.name!,
      description: v.description || undefined,
      vendor: v.vendor as any,
      frequency: v.frequency as any,
      analysisType: v.analysisType as AnalysisType,
      analysisTypeFields,
      searchKeywords: (v.searchKeywords as string).split(',').map(s => s.trim()).filter(s => s),
      minPrice: v.minPrice || undefined,
      maxPrice: v.maxPrice || undefined
    };

    this.monitorService.createMonitor(payload).subscribe({
      next: () => {
        this.router.navigate(['/monitors']);
      },
      error: (err) => {
        alert('Erro ao salvar o monitor.');
        console.error(err);
        this.isSaving = false;
      }
    });
  }
}
