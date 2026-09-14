import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth-service';

/**
 * Kaci token na svaki zahtjev i hvata 401.
 *
 * inject() se zove SAMO na vrhu funkcije - unutar catchError callback-a
 * vise nismo u injection kontekstu i puca.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();

  // Na /api/auth/** se token ne kaci - login i register ga jos nemaju.
  const jeAuthPutanja = req.url.includes('/api/auth/');

  if (token && !jeAuthPutanja) {
    // Zahtjev je nepromjenjiv - pravi se kopija sa dodanim headerom.
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(req).pipe(
    catchError((greska: HttpErrorResponse) => {
      // 401 van login putanje = token istekao ili je nevalidan.
      // Token vrijedi 2 sata, pa se ovo prije ili kasnije desi.
      if (greska.status === 401 && !jeAuthPutanja) {
        auth.logout();
      }
      return throwError(() => greska);
    }),
  );
};
