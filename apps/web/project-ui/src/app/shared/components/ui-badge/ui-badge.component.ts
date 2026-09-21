import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeVariant =
  'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'high' | 'medium' | 'low' | 'brand';

@Component({
  selector: 'app-ui-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      [ngClass]="getClasses()"
      class="inline-flex items-center justify-center border font-medium"
    >
      <ng-content></ng-content>
    </span>
  `,
})
export class UiBadgeComponent {
  @Input() variant: BadgeVariant = 'neutral';
  @Input() rounded: 'full' | 'sm' | 'md' = 'full';
  @Input() size: 'xs' | 'sm' | 'md' = 'sm';

  getClasses(): string {
    let classes = '';

    switch (this.size) {
      case 'xs':
        classes += 'px-2 py-0.5 text-xs ';
        break;
      case 'sm':
        classes += 'px-2.5 py-1 text-xs ';
        break;
      case 'md':
        classes += 'px-3 py-1.5 text-sm ';
        break;
    }

    switch (this.rounded) {
      case 'full':
        classes += 'rounded-full ';
        break;
      case 'sm':
        classes += 'rounded-sm ';
        break;
      case 'md':
        classes += 'rounded-md ';
        break;
    }

    switch (this.variant) {
      case 'success':
        classes += 'bg-emerald-100 text-emerald-800 border-emerald-200';
        break;
      case 'warning':
        classes += 'bg-amber-100 text-amber-900 border-amber-200';
        break;
      case 'danger':
        classes += 'bg-red-100 text-red-800 border-red-200';
        break;
      case 'info':
        classes += 'bg-cyan-100 text-cyan-900 border-cyan-200';
        break;
      case 'neutral':
        classes += 'bg-brand-950/5 text-brand-950/70 border-brand-950/10';
        break;
      case 'high':
        classes += 'bg-emerald-100 text-emerald-800 border-emerald-300';
        break;
      case 'medium':
        classes += 'bg-amber-100 text-amber-900 border-amber-300';
        break;
      case 'low':
        classes += 'bg-orange-100 text-orange-900 border-orange-200';
        break;
      case 'brand':
        classes += 'bg-brand-amber/15 text-brand-amber-strong border-brand-amber/30';
        break;
    }

    return classes;
  }
}
