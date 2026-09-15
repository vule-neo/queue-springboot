import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Hourly, QueueStats, WaitEstimate } from './analytics.model';

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {

  private http = inject(HttpClient);

  today(date?: string): Observable<QueueStats[]> {
    const params = date ? new HttpParams().set('date', date) : undefined;
    return this.http.get<QueueStats[]>('/api/analytics/today', { params });
  }

  hourly(queueId: number, date?: string): Observable<Hourly[]> {
    const params = date ? new HttpParams().set('date', date) : undefined;
    return this.http.get<Hourly[]>(`/api/analytics/queues/${queueId}/hourly`, { params });
  }

  estimate(ticketId: number): Observable<WaitEstimate> {
    return this.http.get<WaitEstimate>(`/api/tickets/${ticketId}/estimate`);
  }
}
