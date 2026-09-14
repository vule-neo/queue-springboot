import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Ticket } from './ticket.model';

@Injectable({
  providedIn: 'root',
})
export class TicketService {

  private http = inject(HttpClient);

  findByQueue(queueId: number): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`/api/queues/${queueId}/tickets`);
  }

  // POST u Angularu uvijek trazi tijelo kao drugi argument.
  // Backend ga ne cita - queueId je u putanji, broj racuna server.
  uzmiBroj(queueId: number): Observable<Ticket> {
    return this.http.post<Ticket>(`/api/queues/${queueId}/tickets`, {});
  }

  // ---------- salterske akcije ----------

  pozoviSljedeceg(queueId: number): Observable<Ticket> {
    return this.http.post<Ticket>(`/api/queues/${queueId}/next`, {});
  }

  private akcija(ticketId: number, naziv: string): Observable<Ticket> {
    return this.http.post<Ticket>(`/api/tickets/${ticketId}/${naziv}`, {});
  }

  pozovi(id: number)  { return this.akcija(id, 'call'); }
  obradjuj(id: number) { return this.akcija(id, 'serve'); }
  zavrsi(id: number)   { return this.akcija(id, 'complete'); }
  preskoci(id: number) { return this.akcija(id, 'skip'); }
  vratiURed(id: number) { return this.akcija(id, 'requeue'); }
  nijeDosao(id: number) { return this.akcija(id, 'no-show'); }
  otkazi(id: number)    { return this.akcija(id, 'cancel'); }
}
