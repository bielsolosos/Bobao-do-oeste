import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ui-pagination.component.html',
})
export class UiPaginationComponent {
  @Input() currentPage: number = 0; // 0-indexed
  @Input() totalPages: number = 0;
  @Input() totalElements: number = 0;
  @Input() pageSize: number = 50;
  @Input() pageSizeOptions: number[] = [10, 15, 20, 50, 100];

  @Output() pageChange = new EventEmitter<number>();
  @Output() pageSizeChange = new EventEmitter<number>();

  get isFirstPage(): boolean {
    return this.currentPage === 0;
  }

  get isLastPage(): boolean {
    return this.currentPage >= this.totalPages - 1 || this.totalPages === 0;
  }

  get visiblePages(): (number | '...')[] {
    const total = this.totalPages;
    const current = this.currentPage;

    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i);
    }

    const pages: (number | '...')[] = [0];
    const start = Math.max(1, current - 1);
    const end = Math.min(total - 2, current + 1);

    if (start > 1) pages.push('...');
    for (let i = start; i <= end; i++) pages.push(i);
    if (end < total - 2) pages.push('...');
    pages.push(total - 1);

    return pages;
  }

  onSelectPageSize(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.pageSizeChange.emit(Number(value));
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }

  onPageClick(page: number | '...') {
    if (typeof page === 'number') this.goToPage(page);
  }

  nextPage() {
    if (!this.isLastPage) {
      this.goToPage(this.currentPage + 1);
    }
  }

  prevPage() {
    if (!this.isFirstPage) {
      this.goToPage(this.currentPage - 1);
    }
  }
}
