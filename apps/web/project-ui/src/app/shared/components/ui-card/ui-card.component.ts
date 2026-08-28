import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden" [ngClass]="extraClasses">
      <!-- Header -->
      <div *ngIf="title || subtitle || hasHeaderAction" class="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
        <div>
          <h3 class="text-lg font-semibold text-gray-800">{{ title }}</h3>
          <p *ngIf="subtitle" class="text-sm text-gray-500 mt-1">{{ subtitle }}</p>
        </div>
        <div *ngIf="hasHeaderAction">
          <ng-content select="[card-action]"></ng-content>
        </div>
      </div>
      
      <!-- Body -->
      <div [ngClass]="noPadding ? '' : 'p-6'">
        <ng-content></ng-content>
      </div>

      <!-- Footer -->
      <div *ngIf="hasFooter" class="px-6 py-4 border-t border-gray-100 bg-gray-50">
        <ng-content select="[card-footer]"></ng-content>
      </div>
    </div>
  `
})
export class UiCardComponent {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() noPadding: boolean = false;
  @Input() hasHeaderAction: boolean = false;
  @Input() hasFooter: boolean = false;
  @Input() extraClasses: string = '';
}
