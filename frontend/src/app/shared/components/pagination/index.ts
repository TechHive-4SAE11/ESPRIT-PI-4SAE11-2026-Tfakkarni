import { Component, Input, Output, EventEmitter, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ZardButtonComponent } from '../button';
import { ZardIconComponent } from '../icon';

@Component({
  selector: 'z-pagination',
  standalone: true,
  imports: [CommonModule, ZardButtonComponent, ZardIconComponent],
  template: `
    <nav class="flex items-center justify-between px-4 py-3 sm:px-6" aria-label="Pagination">
      <div class="hidden sm:block">
        <p class="text-sm text-slate-700 dark:text-slate-300">
          Showing
          <span class="font-medium">{{ startItem() }}</span>
          to
          <span class="font-medium">{{ endItem() }}</span>
          of
          <span class="font-medium">{{ totalItems }}</span>
          results
        </p>
      </div>
      <div class="flex flex-1 justify-between sm:justify-end gap-2">
        <button
          z-button
          zType="outline"
          zSize="sm"
          [disabled]="currentPage === 0"
          (click)="onPrevious()"
          class="flex items-center gap-1"
        >
          <z-icon zType="chevron-left" class="h-4 w-4" />
          <span>Previous</span>
        </button>
        
        <div class="hidden sm:flex gap-1">
          @for (page of visiblePages(); track page) {
            @if (page === -1) {
              <span class="px-3 py-2 text-sm text-slate-500">...</span>
            } @else {
              <button
                z-button
                [zType]="page === currentPage ? 'default' : 'outline'"
                zSize="sm"
                (click)="onPageChange(page)"
              >
                {{ page + 1 }}
              </button>
            }
          }
        </div>
        
        <button
          z-button
          zType="outline"
          zSize="sm"
          [disabled]="currentPage >= totalPages - 1"
          (click)="onNext()"
          class="flex items-center gap-1"
        >
          <span>Next</span>
          <z-icon zType="chevron-right" class="h-4 w-4" />
        </button>
      </div>
    </nav>
  `,
})
export class ZardPaginationComponent {
  @Input() currentPage = 0;
  @Input() totalPages = 1;
  @Input() totalItems = 0;
  @Input() pageSize = 10;
  @Output() pageChange = new EventEmitter<number>();

  startItem = computed(() => this.currentPage * this.pageSize + 1);
  endItem = computed(() => Math.min((this.currentPage + 1) * this.pageSize, this.totalItems));

  visiblePages = computed(() => {
    const pages: number[] = [];
    const maxVisible = 7;
    const halfVisible = Math.floor(maxVisible / 2);

    if (this.totalPages <= maxVisible) {
      // Show all pages
      for (let i = 0; i < this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Always show first page
      pages.push(0);

      let start = Math.max(1, this.currentPage - halfVisible);
      let end = Math.min(this.totalPages - 2, this.currentPage + halfVisible);

      if (this.currentPage <= halfVisible) {
        end = maxVisible - 2;
      } else if (this.currentPage >= this.totalPages - halfVisible - 1) {
        start = this.totalPages - maxVisible + 1;
      }

      // Add ellipsis before if needed
      if (start > 1) {
        pages.push(-1); // -1 represents ellipsis
      }

      // Add middle pages
      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      // Add ellipsis after if needed
      if (end < this.totalPages - 2) {
        pages.push(-1);
      }

      // Always show last page
      pages.push(this.totalPages - 1);
    }

    return pages;
  });

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }

  onPrevious(): void {
    if (this.currentPage > 0) {
      this.pageChange.emit(this.currentPage - 1);
    }
  }

  onNext(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.pageChange.emit(this.currentPage + 1);
    }
  }
}
