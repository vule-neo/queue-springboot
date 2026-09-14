// Oblik TicketResponse sa backenda.
export interface Ticket {
  id: number;
  queueId: number;
  number: string;      // "B001" - string, ne broj
  sequenceNo: number;
  issuedDate: string;
  status: string;
  createdAt: string;
  // Null dok se ne dogode. createdAt -> calledAt je cekanje,
  // calledAt -> completedAt je trajanje obrade.
  calledAt: string | null;
  completedAt: string | null;
}
