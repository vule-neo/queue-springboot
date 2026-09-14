import { Routes } from '@angular/router';

import { authGuard } from './auth/auth-guard';
import { LoginComp } from './auth/login-comp/login-comp';
import { QueueComp } from './queue/queue-comp/queue-comp';
import { DisplayComp } from './ticket/display-comp/display-comp';
import { TicketComp } from './ticket/ticket-comp/ticket-comp';

export const routes: Routes = [
  { path: 'login', component: LoginComp },

  { path: '', component: QueueComp, canActivate: [authGuard] },
  { path: 'queues/:id', component: TicketComp, canActivate: [authGuard] },

  // Javni ekran visi na zidu - niko se tu ne prijavljuje, pa nema guarda.
  { path: 'display/:id', component: DisplayComp },

  { path: '**', redirectTo: '' },
];
