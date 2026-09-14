import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Queue } from './queue.model';

@Injectable({
  providedIn: 'root',
})
export class QueueService {

  private http = inject(HttpClient);

  // Relativna putanja: proxy.conf.json je prosljedjuje na localhost:8080.
  private baseUrl = '/api/queues';

  // Vraca Observable, NE pretplacuje se. Pretplata je posao komponente -
  // servis ne zna sta ce se s podacima raditi.
  findAll(): Observable<Queue[]> {
    return this.http.get<Queue[]>(this.baseUrl);
  }
}
