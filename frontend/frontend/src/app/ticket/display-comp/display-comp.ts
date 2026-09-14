import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { QueueSocketService } from '../queue-socket-service';
import { TicketService } from '../ticket-service';
import { Ticket } from '../ticket.model';

/**
 * Javni ekran u cekaonici. Isti topic kao TicketComp, drugi prikaz:
 * veliki tekuci broj i kratka lista sljedecih.
 */
@Component({
  selector: 'app-display-comp',
  imports: [],
  templateUrl: './display-comp.html',
})
export class DisplayComp implements OnInit {

  private ticketService = inject(TicketService);
  private socket = inject(QueueSocketService);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  queueId = 0;
  tickets = signal<Ticket[]>([]);

  // Trenutno se poziva: zadnji koji je usao u CALLED.
  trenutni = computed(() => {
    const pozvani = this.tickets().filter(t => t.status === 'CALLED' && t.calledAt !== null);
    if (pozvani.length === 0) return null;
    return pozvani.sort((a, b) => (a.calledAt! < b.calledAt! ? 1 : -1))[0];
  });

  sljedeci = computed(() =>
    this.tickets().filter(t => t.status === 'WAITING').slice(0, 5));

  ngOnInit(): void {
    this.queueId = Number(this.route.snapshot.paramMap.get('id'));

    this.ticketService.findByQueue(this.queueId)
        .subscribe(t => this.tickets.set(t));

    this.socket.pratiRed(this.queueId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(dogadjaj => this.primi(dogadjaj.ticket));
  }

  private primi(ticket: Ticket): void {
    this.tickets.update(lista => {
      const i = lista.findIndex(t => t.id === ticket.id);
      if (i === -1) return [...lista, ticket].sort((a, b) => a.sequenceNo - b.sequenceNo);
      const kopija = [...lista];
      kopija[i] = ticket;
      return kopija;
    });
  }
}
