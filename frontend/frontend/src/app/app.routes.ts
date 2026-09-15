import { Routes } from '@angular/router';

import { authGuard, managerGuard } from './auth/auth-guard';
import { DashboardComp } from './analytics/dashboard-comp/dashboard-comp';
import { LoginComp } from './auth/login-comp/login-comp';
import { QueueComp } from './queue/queue-comp/queue-comp';
import { DisplayComp } from './ticket/display-comp/display-comp';
import { TicketComp } from './ticket/ticket-comp/ticket-comp';

export const routes: Routes = [
  { path: 'login', component: LoginComp },

  { path: '', component: QueueComp, canActivate: [authGuard] },
  { path: 'queues/:id', component: TicketComp, canActivate: [authGuard] },
  { path: 'analytics', component: DashboardComp, canActivate: [managerGuard] },

  // Javni ekran visi na zidu - niko se tu ne prijavljuje, pa nema guarda.
  { path: 'display/:id', component: DisplayComp },

  { path: '**', redirectTo: '' },
];
