import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AiLogService } from '../../core/services/ai-log.service';
import { AiAnalysisLogResponse } from '../../core/models/ai-log.model';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { UiPaginationComponent } from '../../shared/components/ui-pagination/ui-pagination.component';

@Component({
  selector: 'app-ai-logs-list',
  standalone: true,
  imports: [CommonModule, RouterModule, UiCardComponent, UiBadgeComponent, UiPaginationComponent],
  templateUrl: './ai-logs-list.component.html'
})
export class AiLogsListComponent implements OnInit {
  private aiLogService = inject(AiLogService);

  logs = signal<AiAnalysisLogResponse[]>([]);
  isLoading = signal<boolean>(true);
  selectedLog = signal<AiAnalysisLogResponse | null>(null);

  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  pageSize = signal<number>(15);

  ngOnInit() {
    this.loadLogs();
  }

  loadLogs(page: number = this.currentPage()) {
    this.isLoading.set(true);
    this.aiLogService.getAllAiLogs(page, this.pageSize()).subscribe({
      next: (res: any) => {
        this.logs.set(res.content);
        this.currentPage.set(res.number);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        console.error('Erro ao carregar logs de IA', err);
        this.isLoading.set(false);
      }
    });
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.loadLogs(0);
  }

  onPageChange(page: number) {
    this.loadLogs(page);
  }

  openDetailModal(log: AiAnalysisLogResponse) {
    this.selectedLog.set(log);
  }

  closeDetailModal() {
    this.selectedLog.set(null);
  }

  formatDuration(ms?: number): string {
    if (ms == null) return '--';
    if (ms >= 1000) return (ms / 1000).toFixed(2) + 's';
    return ms + 'ms';
  }

  formatJson(rawJson?: string): string {
    if (!rawJson) return '';
    try {
      const parsed = JSON.parse(rawJson);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return rawJson;
    }
  }
}
