import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  effect,
  inject,
  input,
  viewChild,
} from '@angular/core';
import * as echarts from 'echarts';
import { ECharts, EChartsCoreOption } from 'echarts';

//TODO rever junto do utilitário que tem dos charts para construir esse cara direito
//Básicamente é um wrapper do Echarts para fazer um loading e fallback de erro caso tenha.
//Insere o Echarts de forma genérica e usa as opções vindas do utils.
@Component({
  selector: 'app-ui-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative w-full overflow-hidden" [style.height]="height()">
      @if (loading()) {
        <div
          class="absolute inset-0 z-10 flex items-center justify-center bg-surface/60 backdrop-blur-[1px]"
        >
          <div
            class="h-6 w-6 animate-spin rounded-full border-2 border-brand-amber-strong border-t-transparent"
          ></div>
        </div>
      }
      <div #chartContainer class="h-full w-full"></div>
    </div>
  `,
})
export class UiChartComponent implements AfterViewInit, OnDestroy {
  private elementRef = inject(ElementRef);
  chartContainer = viewChild<ElementRef<HTMLDivElement>>('chartContainer');

  options = input<EChartsCoreOption | null>(null);
  loading = input<boolean>(false);
  height = input<string>('280px');

  private chartInstance: ECharts | null = null;
  private resizeObserver: ResizeObserver | null = null;

  constructor() {
    effect(() => {
      const opts = this.options();
      if (this.chartInstance && opts) {
        this.chartInstance.setOption(opts, true);
      }
    });
  }

  ngAfterViewInit(): void {
    const container = this.chartContainer()?.nativeElement;
    if (!container) return;

    this.chartInstance = echarts.init(container, null, {
      renderer: 'svg',
    });

    const currentOpts = this.options();
    if (currentOpts) {
      this.chartInstance.setOption(currentOpts);
    }

    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = new ResizeObserver(() => {
        this.chartInstance?.resize();
      });
      this.resizeObserver.observe(container);
    }
  }

  ngOnDestroy(): void {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
      this.resizeObserver = null;
    }
    if (this.chartInstance) {
      this.chartInstance.dispose();
      this.chartInstance = null;
    }
  }
}
