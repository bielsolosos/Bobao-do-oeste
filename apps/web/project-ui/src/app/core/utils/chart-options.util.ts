import { EChartsCoreOption } from 'echarts';
import {
  TierMetricsResponse,
  PriceDistributionResponse,
  BrandDistributionResponse,
  TimelineMetricsResponse,
} from '../models/metrics.model';

interface TooltipParam {
  name: string;
  value: number;
  color?: string;
}

export class ChartOptionsUtil {
  static buildDonutOptions(data: TierMetricsResponse): EChartsCoreOption {
    const analyzedTotal = (data.high || 0) + (data.medium || 0) + (data.low || 0);
    const hasData = analyzedTotal > 0;

    return {
      tooltip: {
        trigger: 'item',
        formatter: (paramData: unknown) => {
          const params = paramData as TooltipParam;
          const val = Number(params.value) || 0;
          const percent = analyzedTotal > 0 ? ((val / analyzedTotal) * 100).toFixed(1) : '0';
          return `<div class="font-sans text-xs">
            <span class="inline-block w-2.5 h-2.5 rounded-full mr-1.5" style="background-color: ${params.color ?? '#F59E0B'};"></span>
            <strong>${params.name}</strong>: ${val} (${percent}%)
          </div>`;
        },
      },
      legend: {
        bottom: '0%',
        left: 'center',
        icon: 'circle',
        itemWidth: 8,
        itemHeight: 8,
        textStyle: {
          fontSize: 11,
          color: '#475569',
        },
      },
      series: [
        {
          name: 'Relevância',
          type: 'pie',
          radius: ['45%', '72%'],
          center: ['50%', '42%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#ffffff',
            borderWidth: 2,
          },
          label: {
            show: false,
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 12,
              fontWeight: 'bold',
              formatter: '{b}: {c}',
            },
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.15)',
            },
          },
          data: hasData
            ? [
                { value: data.high, name: 'Alta Relevância', itemStyle: { color: '#10B981' } },
                { value: data.medium, name: 'Média Relevância', itemStyle: { color: '#F59E0B' } },
                { value: data.low, name: 'Baixa Relevância', itemStyle: { color: '#F43F5E' } },
              ].filter((item) => item.value > 0)
            : [{ value: 1, name: 'Nenhum item analisado', itemStyle: { color: '#E2E8F0' } }],
        },
      ],
    };
  }

  static buildPriceHistogramOptions(data: PriceDistributionResponse): EChartsCoreOption {
    const buckets = data.buckets || [];
    const labels = buckets.map((b) => b.label);
    const values = buckets.map((b) => b.count);

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (paramData: unknown) => {
          const list = Array.isArray(paramData)
            ? (paramData as TooltipParam[])
            : [paramData as TooltipParam];
          const p = list[0];
          return `<div class="font-sans text-xs">
            <strong>${p.name}</strong><br/>
            Anúncios: <strong>${p.value}</strong>
          </div>`;
        },
      },
      grid: {
        top: '12%',
        left: '3%',
        right: '4%',
        bottom: '8%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: labels,
        axisLine: { lineStyle: { color: '#CBD5E1' } },
        axisLabel: {
          color: '#64748B',
          fontSize: 11,
          interval: 0,
        },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
        axisLabel: { color: '#64748B', fontSize: 11 },
      },
      series: [
        {
          name: 'Anúncios',
          type: 'bar',
          barWidth: '40%',
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: '#F59E0B' },
                { offset: 1, color: '#D97706' },
              ],
            },
          },
          data: values,
        },
      ],
    };
  }

  static buildBrandBarOptions(data: BrandDistributionResponse): EChartsCoreOption {
    const items = data.brands || [];
    const labels = items.map((b) => b.brand).reverse();
    const values = items.map((b) => b.count).reverse();

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (paramData: unknown) => {
          const list = Array.isArray(paramData)
            ? (paramData as TooltipParam[])
            : [paramData as TooltipParam];
          const p = list[0];
          return `<div class="font-sans text-xs">
            Marca: <strong>${p.name}</strong><br/>
            Anúncios: <strong>${p.value}</strong>
          </div>`;
        },
      },
      grid: {
        top: '8%',
        left: '3%',
        right: '6%',
        bottom: '8%',
        containLabel: true,
      },
      xAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
        axisLabel: { color: '#64748B', fontSize: 11 },
      },
      yAxis: {
        type: 'category',
        data: labels,
        axisLine: { lineStyle: { color: '#CBD5E1' } },
        axisLabel: { color: '#334155', fontSize: 11, fontWeight: 'bold' },
      },
      series: [
        {
          name: 'Anúncios',
          type: 'bar',
          barWidth: '45%',
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 1,
              y2: 0,
              colorStops: [
                { offset: 0, color: '#6366F1' },
                { offset: 1, color: '#4F46E5' },
              ],
            },
          },
          data: values,
        },
      ],
    };
  }

  static buildTimelineOptions(data: TimelineMetricsResponse): EChartsCoreOption {
    const points = data.points || [];
    const labels = points.map((p) => {
      const parts = p.date.split('-');
      return parts.length === 3 ? `${parts[2]}/${parts[1]}` : p.date;
    });
    const values = points.map((p) => p.count);

    return {
      tooltip: {
        trigger: 'axis',
        formatter: (paramData: unknown) => {
          const list = Array.isArray(paramData)
            ? (paramData as TooltipParam[])
            : [paramData as TooltipParam];
          const p = list[0];
          return `<div class="font-sans text-xs">
            Data: <strong>${p.name}</strong><br/>
            Anúncios Capturados: <strong>${p.value}</strong>
          </div>`;
        },
      },
      grid: {
        top: '12%',
        left: '3%',
        right: '4%',
        bottom: '8%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: labels,
        axisLine: { lineStyle: { color: '#CBD5E1' } },
        axisLabel: { color: '#64748B', fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
        axisLabel: { color: '#64748B', fontSize: 11 },
      },
      series: [
        {
          name: 'Capturas',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: {
            color: '#F59E0B',
            width: 3,
          },
          itemStyle: {
            color: '#F59E0B',
            borderWidth: 2,
            borderColor: '#FFFFFF',
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(245, 158, 11, 0.35)' },
                { offset: 1, color: 'rgba(245, 158, 11, 0.02)' },
              ],
            },
          },
          data: values,
        },
      ],
    };
  }
}
