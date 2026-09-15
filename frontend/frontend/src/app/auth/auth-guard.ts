import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth-service';

/**
 * Pusta na rutu samo prijavljene. Vraca UrlTree umjesto da sam navigira -
 * tako router zna da je navigacija preusmjerena, a ne otkazana.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.jePrijavljen() ? true : router.createUrlTree(['/login']);
};

/**
 * Samo menadzer/admin. Neprijavljenog salje na login, prijavljenog bez
 * prava na pocetnu. Backend svejedno vraca 403 - guard je UX, ne sigurnost.
 */
export const managerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.jePrijavljen()) { return router.createUrlTree(['/login']); }
  const u = auth.uloga();
  return u === 'MANAGER' || u === 'ADMIN' ? true : router.createUrlTree(['/']);
};
