import { Ticket } from './ticket.model';

export interface TicketEvent {
  tip: 'IZDAT' | 'PROMIJENJEN';
  queueId: number;
  ticket: Ticket;
  kada: string;
}
