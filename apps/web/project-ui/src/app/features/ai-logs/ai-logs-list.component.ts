import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, Subject } from 'rxjs';
import { RouterModule } from '@angular/router';
import { AiLogService } from '../../core/services/ai-log.service';
import { AiAnalysisLogResponse } from '../../core/models/ai-log.model';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { UiPaginationComponent } from '../../shared/components/ui-pagination/ui-pagination.component';
import { UiStatePanelComponent } from '../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { UiDialogComponent } from '../../shared/components/ui-dialog/ui-dialog.component';
import { UiCodePanelComponent } from '../../shared/components/ui-code-panel/ui-code-panel.component';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';

type ListState = 'loading' | 'error' | 'ready';

@Component({
  selector: 'app-ai-logs-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    UiCardComponent,
    UiBadgeComponent,
    UiPaginationComponent,
    UiStatePanelComponent,
    UiPageHeaderComponent,
    UiDialogComponent,
    UiCodePanelComponent,
    UiButtonComponent,
  ],
  templateUrl: './ai-logs-list.component.html',
})
export class AiLogsListComponent implements OnInit {
  private aiLogService = inject(AiLogService);
  private destroyRef = inject(DestroyRef);
  private keywordSearch = new Subject<void>();

  state = signal<ListState>('loading');
  logs = signal<AiAnalysisLogResponse[]>([]);
  selectedLog = signal<AiAnalysisLogResponse | null>(null);

  statusFilter = signal<'all' | 'SUCCESS' | 'ERROR'>('all');
  keyword = signal('');

  page = signal(0);
  pageSize = signal(15);

  totalElements = signal(0);
  totalPages = signal(0);

  constructor() {
    this.keywordSearch
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadLogs());
  }

  ngOnInit() {
    this.loadLogs();
  }

  loadLogs() {
    this.state.set('loading');
    const status = this.statusFilter();
    this.aiLogService
      .getAllAiLogs(this.page(), this.pageSize(), {
        status: status === 'all' ? undefined : status,
        q: this.keyword(),
      })
      .subscribe({
        next: (res) => {
          this.logs.set(res.content ?? []);
          this.page.set(res.number ?? 0);
          this.totalElements.set(res.totalElements ?? 0);
          this.totalPages.set(res.totalPages ?? 0);
          this.state.set('ready');
        },
        error: () => this.state.set('error'),
      });
  }

  onStatus(event: Event) {
    this.statusFilter.set((event.target as HTMLSelectElement).value as 'all' | 'SUCCESS' | 'ERROR');
    this.page.set(0);
    this.loadLogs();
  }

  onKeyword(event: Event) {
    this.keyword.set((event.target as HTMLInputElement).value);
    this.page.set(0);
    this.keywordSearch.next();
  }

  onPageChange(page: number) {
    this.page.set(page);
    this.loadLogs();
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.page.set(0);
    this.loadLogs();
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
      return JSON.stringify(JSON.parse(rawJson), null, 2);
    } catch {
      return rawJson;
    }
  }
}
