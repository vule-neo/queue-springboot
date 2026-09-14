import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { QueueService } from '../queue-service';
import { Queue } from '../queue.model';

@Component({
  selector: 'app-queue-comp',
  // imports je samo za komponente/direktive/pipe-ove iz sablona.
  // RouterLink je direktiva pa ide ovdje; servisi nikad.
  imports: [RouterLink],
  templateUrl: './queue-comp.html',
  styleUrl: './queue-comp.css',
})
export class QueueComp implements OnInit {

  private queueService = inject(QueueService);

  // Podatak se DODJELJUJE polju kad stigne - ne moze se vratiti iz subscribe.
  queues = signal<Queue[]>([]);

  // ngOnInit, a ne konstruktor: Angular ga zove kad je komponenta spremna.
  ngOnInit(): void {
    this.ucitaj();
  }

  ucitaj(): void {
    this.queueService.findAll().subscribe(queues => this.queues.set(queues));
  }
}
