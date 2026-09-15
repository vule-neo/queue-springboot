import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { AnalyticsService } from '../../analytics/analytics-service';
import { WaitEstimate } from '../../analytics/analytics.model';
import { AuthService } from '../../auth/auth-service';
import { QueueSocketService } from '../queue-socket-service';
import { TicketService } from '../ticket-service';
import { Ticket } from '../ticket.model';

@Component({
  selector: 'app-ticket-comp',
  imports: [RouterLink],
  templateUrl: './ticket-comp.html',
  styleUrl: './ticket-comp.css',
})
export class TicketComp implements OnInit {

  private ticketService = inject(TicketService);
  private analytics = inject(AnalyticsService);
  private socket = inject(QueueSocketService);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  auth = inject(AuthService);

  queueId = 0;
  tickets = signal<Ticket[]>([]);
  zadnjiBroj = signal<string | null>(null);
  mojTicketId: number | null = null;
  procjena = signal<WaitEstimate | null>(null);
  greska = signal<string | null>(null);
  uzivo = signal(false);

  ngOnInit(): void {
    this.queueId = Number(this.route.snapshot.paramMap.get('id'));

    // Prvo ucitavanje ide preko REST-a: WebSocket salje samo PROMJENE,
    // ne zna se sta je bilo prije nego smo se prikljucili.
    this.ucitaj();

    this.socket.pratiRed(this.queueId)
        // takeUntilDestroyed otkazuje pretplatu kad komponenta nestane.
        // Bez toga bi se pri svakoj promjeni rute dodavala nova.
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(dogadjaj => {
          this.uzivo.set(true);
          this.primi(dogadjaj.ticket);
          // Svaka promjena u redu mijenja i koliko je ispred mene.
          this.osvjeziProcjenu();
        });
  }

  /** Ubaci ili zamijeni ticket u listi, bez odlaska na server. */
  private primi(ticket: Ticket): void {
    this.tickets.update(lista => {
      const i = lista.findIndex(t => t.id === ticket.id);
      if (i === -1) {
        return [...lista, ticket].sort((a, b) => a.sequenceNo - b.sequenceNo);
      }
      const kopija = [...lista];
      kopija[i] = ticket;
      return kopija;
    });
  }

  ucitaj(): void {
    this.ticketService.findByQueue(this.queueId).subscribe(t => this.tickets.set(t));
  }

  uzmiBroj(): void {
    this.greska.set(null);
    this.ticketService.uzmiBroj(this.queueId).subscribe({
      // Listu vise ne dohvatamo ponovo - stici ce kroz WebSocket.
      next: noviTicket => {
        this.zadnjiBroj.set(noviTicket.number);
        this.mojTicketId = noviTicket.id;
        this.osvjeziProcjenu();
      },
      error: g => this.greska.set(g.error?.message ?? 'Greska'),
    });
  }

  private osvjeziProcjenu(): void {
    if (this.mojTicketId === null) { return; }
    this.analytics.estimate(this.mojTicketId).subscribe(p => this.procjena.set(p));
  }

  /** "oko 12 min" / "manje od minute" / "na redu si". */
  procjenaTekst(p: WaitEstimate): string {
    if (p.status !== 'WAITING') { return 'na redu si'; }
    if (p.estimatedWaitSeconds < 60) { return 'manje od minute'; }
    return `oko ${Math.round(p.estimatedWaitSeconds / 60)} min`;
  }

  pozoviSljedeceg(): void {
    this.izvrsi(this.ticketService.pozoviSljedeceg(this.queueId));
  }

  akcija(id: number, naziv: 'pozovi' | 'obradjuj' | 'zavrsi' | 'preskoci' | 'vratiURed' | 'nijeDosao' | 'otkazi'): void {
    this.izvrsi(this.ticketService[naziv](id));
  }

  private izvrsi(poziv: Observable<Ticket>): void {
    this.greska.set(null);
    poziv.subscribe({
      next: () => { /* promjena stize kroz WebSocket */ },
      error: g => this.greska.set(g.error?.message ?? 'Greska'),
    });
  }
}
