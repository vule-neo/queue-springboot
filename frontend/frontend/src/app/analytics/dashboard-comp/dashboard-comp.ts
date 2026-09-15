import { Component, effect, ElementRef, inject, OnDestroy, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BarController, BarElement, CategoryScale, Chart, LinearScale, Title, Tooltip } from 'chart.js';

import { AnalyticsService } from '../analytics-service';
import { QueueStats } from '../analytics.model';

// Chart.js je "tree-shakeable": registruje se samo ono sto koristimo,
// pa u bundle ne ulaze pie/line/legend koje ne trebamo.
Chart.register(BarController, BarElement, CategoryScale, LinearScale, Title, Tooltip);

/**
 * Menadzerski pregled dana: tabela po redu + grafikon izdatih po satu
 * za izabrani red. Ruta je zasticena guardom, a backend ionako vraca
 * 403 za ne-menadzera - UI je samo prikaz.
 */
@Component({
  selector: 'app-dashboard-comp',
  imports: [RouterLink],
  templateUrl: './dashboard-comp.html',
})
export class DashboardComp implements OnDestroy {

  private analytics = inject(AnalyticsService);

  // Canvas je u @if bloku, pa signal query: undefined dok se ne prikaze.
  private canvas = viewChild<ElementRef<HTMLCanvasElement>>('grafikon');
  private chart: Chart | null = null;

  datum = signal(new Date().toISOString().slice(0, 10));
  stats = signal<QueueStats[]>([]);
  izabraniRed = signal<number | null>(null);
  greska = signal<string | null>(null);

  constructor() {
    // Kad se promijeni datum ILI izabrani red, ponovo ucitaj.
    // effect() prati signale koje procita - ne treba rucno subscribe/unsubscribe.
    effect(() => {
      const d = this.datum();
      this.analytics.today(d).subscribe({
        next: s => {
          this.stats.set(s);
          // Prvi put: izaberi prvi red koji ima nesto izdato, inace prvi.
          if (this.izabraniRed() === null && s.length > 0) {
            this.izabraniRed.set((s.find(q => q.issued > 0) ?? s[0]).queueId);
          }
        },
        error: g => this.greska.set(g.error?.message ?? 'Greska'),
      });
    });

    effect(() => {
      const red = this.izabraniRed();
      const d = this.datum();
      const el = this.canvas();
      if (red === null || !el) { return; }
      this.analytics.hourly(red, d).subscribe(h => this.nacrtaj(el.nativeElement, h.map(x => x.issued)));
    });
  }

  promijeniDatum(e: Event): void {
    this.datum.set((e.target as HTMLInputElement).value);
  }

  izaberi(queueId: number): void {
    this.izabraniRed.set(queueId);
  }

  /** Sekunde -> "12 min" / "45 s" / "–" kad nema podataka. */
  trajanje(s: number | null): string {
    if (s === null) { return '–'; }
    return s >= 60 ? `${Math.round(s / 60)} min` : `${Math.round(s)} s`;
  }

  private nacrtaj(canvas: HTMLCanvasElement, poSatu: number[]): void {
    // Chart.js drzi referencu na canvas; bez destroy() bi svaki refresh
    // crtao novi grafikon preko starog (i curio memoriju).
    this.chart?.destroy();
    this.chart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: poSatu.map((_, h) => `${h}h`),
        datasets: [{ label: 'izdato', data: poSatu }],
      },
      options: {
        animation: false,
        scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
        plugins: { title: { display: true, text: `Izdati brojevi po satu - red #${this.izabraniRed()}` } },
      },
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
