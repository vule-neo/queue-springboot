import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Observable } from 'rxjs';

import { TicketEvent } from './ticket-event.model';

/**
 * Jedna WebSocket veza za cijelu aplikaciju, vise pretplata preko nje.
 *
 * Za razliku od HttpClient-a, ovdje ne pitamo server nista - veza stoji
 * otvorena i server salje kad se nesto desi.
 */
@Injectable({
  providedIn: 'root',
})
export class QueueSocketService {

  private client: Client;

  constructor() {
    this.client = new Client({
      // ws:// umjesto http:// - drugi protokol.
      // Idemo preko dev servera (4200), koji proxy-ja na 8080; zato
      // location.host, a ne tvrdo upisan port.
      brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,

      // Veza pukne (uspavan laptop, izgubljen wifi) - stomp se sam vraca.
      // Ovo je glavna razlika u odnosu na goli WebSocket, gdje bi to pisao sam.
      reconnectDelay: 3000,

      // Ako veza tiho umre, ovo je otkrije u par sekundi umjesto nikad.
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.client.activate();
  }

  /**
   * Pretplata na dogadjaje jednog reda.
   *
   * Vraca Observable da se ponasa isto kao HttpClient - razlika je sto
   * ovaj nikad ne zavrsi: salje dogadjaj svaki put kad se nesto desi.
   */
  pratiRed(queueId: number): Observable<TicketEvent> {
    return new Observable<TicketEvent>(pretplatnik => {
      let stompSub: StompSubscription | undefined;

      const pretplatiSe = () => {
        stompSub = this.client.subscribe(
          `/topic/queue/${queueId}`,
          (poruka: IMessage) => pretplatnik.next(JSON.parse(poruka.body)),
        );
      };

      if (this.client.connected) {
        pretplatiSe();
      } else {
        // Veza se jos otvara - pretplati se cim bude spremna.
        // Bez ovoga bi prva pretplata nakon ucitavanja stranice pala.
        this.client.onConnect = () => pretplatiSe();
      }

      // Funkcija ciscenja: Angular je zove kad komponenta nestane.
      // Bez nje bi se pretplate gomilale pri svakoj promjeni rute.
      return () => stompSub?.unsubscribe();
    });
  }
}
