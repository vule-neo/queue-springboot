import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { AuthResponse } from './auth.model';

const KLJUC_TOKEN = 'qms.token';
const KLJUC_EMAIL = 'qms.email';
const KLJUC_ULOGA = 'qms.uloga';

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private http = inject(HttpClient);
  private router = inject(Router);

  // localStorage prezivljava refresh stranice - bez toga bi te svaki
  // F5 izbacio na login.
  private tokenSig = signal<string | null>(localStorage.getItem(KLJUC_TOKEN));
  private ulogaSig = signal<string | null>(localStorage.getItem(KLJUC_ULOGA));
  private emailSig = signal<string | null>(localStorage.getItem(KLJUC_EMAIL));

  token = this.tokenSig.asReadonly();
  uloga = this.ulogaSig.asReadonly();
  email = this.emailSig.asReadonly();

  jePrijavljen = computed(() => this.tokenSig() !== null);

  // Salterske akcije vidi samo osoblje. Ovo je samo UI - pravu provjeru
  // radi backend; sakriveno dugme nije sigurnost.
  jeOsoblje = computed(() => {
    const u = this.ulogaSig();
    return u === 'EMPLOYEE' || u === 'MANAGER' || u === 'ADMIN';
  });

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password })
        .pipe(tap(odgovor => this.spremi(odgovor)));
  }

  register(email: string, password: string): Observable<unknown> {
    return this.http.post('/api/auth/register', { email, password });
  }

  logout(): void {
    localStorage.removeItem(KLJUC_TOKEN);
    localStorage.removeItem(KLJUC_EMAIL);
    localStorage.removeItem(KLJUC_ULOGA);
    this.tokenSig.set(null);
    this.emailSig.set(null);
    this.ulogaSig.set(null);
    this.router.navigateByUrl('/login');
  }

  private spremi(odgovor: AuthResponse): void {
    localStorage.setItem(KLJUC_TOKEN, odgovor.token);
    localStorage.setItem(KLJUC_EMAIL, odgovor.email);
    localStorage.setItem(KLJUC_ULOGA, odgovor.role);
    this.tokenSig.set(odgovor.token);
    this.emailSig.set(odgovor.email);
    this.ulogaSig.set(odgovor.role);
  }
}
