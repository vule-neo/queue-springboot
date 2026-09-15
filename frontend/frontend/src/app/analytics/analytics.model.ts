// Oblici odgovora sa /api/analytics/** i /api/tickets/{id}/estimate.

export interface QueueStats {
  queueId: number;
  prefix: string;
  day: string;
  issued: number;
  waiting: number;
  inProgress: number;
  completed: number;
  cancelled: number;
  noShow: number;
  avgWaitSeconds: number | null;     // null dok niko nije pozvan
  avgServiceSeconds: number | null;  // null dok niko nije zavrsen
}

export interface Hourly {
  hour: number;
  issued: number;
}

export interface WaitEstimate {
  ticketId: number;
  number: string;
  status: string;
  ahead: number;
  avgServiceSeconds: number;
  estimatedWaitSeconds: number;
  basis: 'HISTORY' | 'DEFAULT';
}
