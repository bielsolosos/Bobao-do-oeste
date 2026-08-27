import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  template: `
    <div class="bg-white rounded-lg shadow-sm p-6 border border-gray-100">
      <h2 class="text-2xl font-semibold text-gray-800 mb-4">Visão Geral</h2>
      <p class="text-gray-600">
        Bem-vindo ao BI Engine. Use o menu lateral para acessar seus Monitores de Produtos.
      </p>
    </div>
  `
})
export class DashboardHomeComponent {}
