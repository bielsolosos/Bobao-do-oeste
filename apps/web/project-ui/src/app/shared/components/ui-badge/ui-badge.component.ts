import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'high' | 'medium' | 'low';

@Component({
  selector: 'app-ui-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span [ngClass]="getClasses()" class="inline-flex items-center justify-center font-medium border">
      <ng-content></ng-content>
    </span>
  `
})
export class UiBadgeComponent {
  @Input() variant: BadgeVariant = 'neutral';
  @Input() rounded: 'full' | 'sm' | 'md' = 'full';
  @Input() size: 'xs' | 'sm' | 'md' = 'xs';

  getClasses(): string {
    let classes = '';

    switch (this.size) {
      case 'xs': classes += 'px-2 py-0.5 text-[10px] '; break;
      case 'sm': classes += 'px-2.5 py-1 text-xs '; break;
      case 'md': classes += 'px-3 py-1.5 text-sm '; break;
    }

    switch (this.rounded) {
      case 'full': classes += 'rounded-full '; break;
      case 'sm': classes += 'rounded-sm '; break;
      case 'md': classes += 'rounded-md '; break;
    }

    switch (this.variant) {
      case 'success': classes += 'bg-green-100 text-green-800 border-green-200'; break;
      case 'warning': classes += 'bg-yellow-100 text-yellow-800 border-yellow-200'; break;
      case 'danger': classes += 'bg-red-100 text-red-800 border-red-200'; break;
      case 'info': classes += 'bg-blue-100 text-blue-800 border-blue-200'; break;
      case 'neutral': classes += 'bg-gray-100 text-gray-700 border-gray-200'; break;
      case 'high': classes += 'bg-emerald-100 text-emerald-800 border-emerald-300 shadow-sm'; break;
      case 'medium': classes += 'bg-amber-100 text-amber-800 border-amber-300 shadow-sm'; break;
      case 'low': classes += 'bg-orange-100 text-orange-800 border-orange-200'; break;
    }

    return classes;
  }
}
