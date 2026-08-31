import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-ui-pagination',
  standalone: true,
  imports: [],
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

  onSelectPageSize(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.pageSizeChange.emit(Number(value));
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
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
