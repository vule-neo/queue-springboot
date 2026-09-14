import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../auth-service';

@Component({
  selector: 'app-login-comp',
  // ReactiveFormsModule JESTE u imports - direktive formGroup/formControlName
  // se koriste u sablonu. Servisi i dalje ne idu ovdje.
  imports: [ReactiveFormsModule],
  templateUrl: './login-comp.html',
})
export class LoginComp {

  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  greska = signal<string | null>(null);
  radi = signal(false);

  // Ista pravila kao @Valid na backendu - ali frontend validacija je samo
  // udobnost. Prava provjera je na serveru, ovu svako moze zaobici.
  forma = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  posalji(): void {
    if (this.forma.invalid) {
      this.forma.markAllAsTouched();
      return;
    }

    this.greska.set(null);
    this.radi.set(true);

    const { email, password } = this.forma.getRawValue();

    this.auth.login(email, password).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: () => {
        // Backend namjerno ne kaze sta je pogresno - ni mi ne pogadjamo.
        this.greska.set('Pogresan email ili lozinka');
        this.radi.set(false);
      },
    });
  }

  registruj(): void {
    if (this.forma.invalid) {
      this.forma.markAllAsTouched();
      return;
    }

    this.greska.set(null);
    this.radi.set(true);

    const { email, password } = this.forma.getRawValue();

    this.auth.register(email, password).subscribe({
      // Registracija ne vraca token - odmah se prijavljujemo.
      next: () => this.posalji(),
      error: () => {
        this.greska.set('Registracija nije uspjela (email mozda vec postoji)');
        this.radi.set(false);
      },
    });
  }
}
