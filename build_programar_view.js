const fs = require('fs');
const path = require('path');

const root = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredFrontEnd\\\\src\\\\app';

// 1. Copy CSS
const srcCss = path.join(root, 'features', 'acreditar-mercado-pago', 'acreditar-mercado-pago.page.css');
const dstCss = path.join(root, 'features', 'programar-recarga', 'programar-recarga.component.css');
let css = fs.readFileSync(srcCss, 'utf8');
fs.writeFileSync(dstCss, css);

// 2. Write TS
const tsContent = `import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AlumnoContextoService } from '../../core/services/alumno-contexto.service';
import { AlumnosService } from '../../data-access/services/alumnos.service';
import { Alumno } from '../../data-access/models/alumno.model';

@Component({
  selector: 'app-programar-recarga',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './programar-recarga.component.html',
  styleUrls: ['./programar-recarga.component.css']
})
export class ProgramarRecargaComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly contextoService = inject(AlumnoContextoService);
  private readonly alumnosService = inject(AlumnosService);

  readonly cargando = signal(false);
  readonly alumno = signal<Alumno | null>(null);

  ngOnInit() {
    const alumnoId = this.contextoService.alumnoSeleccionado()?.id || localStorage.getItem('alumnoId');
    if (alumnoId) {
      this.alumnosService.getAlumnoPorId(alumnoId).subscribe({
        next: (al) => this.alumno.set(al),
        error: () => this.volver()
      });
    } else {
      this.volver();
    }
  }

  volver() {
    this.router.navigate(['/']);
  }

  selectMonto(monto: number) {
    const input = document.getElementById('monto') as HTMLInputElement;
    if (input) input.value = monto.toString();
  }

  onSubmit(event: Event) {
    event.preventDefault();
    const input = document.getElementById('monto') as HTMLInputElement;
    const montoStr = input ? input.value : '0';
    const monto = Number(montoStr);
    if (!monto || monto < 1) return;

    this.cargando.set(true);
    const token = localStorage.getItem('token');
    const alumnoId = this.alumno()?.id;

    fetch('http://localhost:8080/api/v1/payments/schedule-topup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
        body: JSON.stringify({ studentId: alumnoId, amount: monto, frequency: 1, frequencyType: 'months' })
    }).then(res => res.json()).then(data => { 
        if(data.paymentUrl) { window.location.href = data.paymentUrl; } else { this.cargando.set(false); }
    }).catch(err => {
        console.error(err);
        this.cargando.set(false);
    });
  }
}
`;
fs.writeFileSync(path.join(root, 'features', 'programar-recarga', 'programar-recarga.component.ts'), tsContent);

// 3. Write HTML
const htmlContent = `<div class="acreditar-page">
  <main class="acreditar-page__content">
    @if (cargando() && !alumno()) {
    <div class="acreditar-page__loading">
      <i class="fa-solid fa-spinner fa-spin fa-2x" aria-hidden="true"></i>
      <p>Cargando información...</p>
    </div>
    } @else {
    <header class="acreditar-page__header">
      <h1 class="acreditar-page__titulo">Programar para {{ alumno()?.nombre }}</h1>
    </header>
    <div class="acreditar-page__grid">
      <section class="acreditar-page__form-section">
        <h2 class="acreditar-page__subtitle">¿Cuánto querés recargar mensualmente a {{ alumno()?.nombre }}?</h2>
        <form class="acreditar-page__form" (submit)="onSubmit($event)" novalidate>
          <div class="acreditar-page__input-group">
            <label for="monto">Monto a cargar por mes ($)</label>
            <div class="acreditar-page__input-wrapper">
              <input #montoInput type="number" id="monto" name="monto" min="100" step="100" placeholder="Ej: 5000" />
              <div class="acreditar-page__mp-logo" aria-hidden="true">
                <svg viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg" class="acreditar-page__mp-svg">
                  <circle cx="18" cy="18" r="16" fill="#00AEEF" />
                  <path d="M12 18c0-3.3 2.7-6 6-6s6 2.7 6 6-2.7 6-6 6-6-2.7-6-6z" fill="#FFF" />
                  <path d="M18 14c-2.2 0-4 1.8-4 4s1.8 4 4 4 4-1.8 4-4-1.8-4-4-4zm-1 5.5v-3l2.5 1.5-2.5 1.5z"
                    fill="#00AEEF" />
                  <path d="M14 20s1.5-1.5 4-1.5 4 1.5 4 1.5" stroke="#00AEEF" stroke-width="1.5"
                    stroke-linecap="round" />
                </svg>
              </div>
            </div>
          </div>
          <div class="acreditar-page__presets-section">
            <h4 class="acreditar-page__presets-title">Suscripciones sugeridas</h4>
            <div class="acreditar-page__presets-grid">
              <button type="button" class="acreditar-page__preset-btn acreditar-page__preset-btn--menta"
                (click)="montoInput.value = '5000'; selectMonto(5000)">
                <span class="acreditar-page__preset-icono"><i class="fa-solid fa-plus"></i></span>
                <span class="acreditar-page__preset-label">$5.000 /mes</span>
              </button>
              <button type="button" class="acreditar-page__preset-btn acreditar-page__preset-btn--menta"
                (click)="montoInput.value = '10000'; selectMonto(10000)">
                <span class="acreditar-page__preset-icono"><i class="fa-solid fa-plus"></i></span>
                <span class="acreditar-page__preset-label">$10.000 /mes</span>
              </button>
              <button type="button" class="acreditar-page__preset-btn acreditar-page__preset-btn--menta"
                (click)="montoInput.value = '20000'; selectMonto(20000)">
                <span class="acreditar-page__preset-icono"><i class="fa-solid fa-plus"></i></span>
                <span class="acreditar-page__preset-label">$20.000 /mes</span>
              </button>
              <button type="button" class="acreditar-page__preset-btn acreditar-page__preset-btn--menta"
                (click)="montoInput.value = '50000'; selectMonto(50000)">
                <span class="acreditar-page__preset-icono"><i class="fa-solid fa-plus"></i></span>
                <span class="acreditar-page__preset-label">$50.000 /mes</span>
              </button>
            </div>
          </div>

          <div class="acreditar-page__actions">
            <button type="submit" class="acreditar-page__btn acreditar-page__btn--primary"
              [disabled]="cargando()">
              @if (cargando()) {
              <i class="fa-solid fa-spinner fa-spin" aria-hidden="true"></i> Procesando...
              } @else {
              <i class="fa-solid fa-credit-card" aria-hidden="true" style="margin-right: 8px;"></i>
              Suscribirse en Mercado Pago
              }
            </button>
            <button type="button" class="acreditar-page__btn acreditar-page__btn--secondary"
              (click)="volver()" [disabled]="cargando()">
              Cancelar
            </button>
          </div>
        </form>
      </section>

      <section class="acreditar-page__history-section">
        <h3>Suscripciones activas de {{ alumno()?.nombre }}</h3>
        <p class="acreditar-page__history-description">
          Tus recargas programadas.
        </p>
        <div class="acreditar-page__history-container">
          <div class="acreditar-page__history-empty">
            <i class="fa-solid fa-calendar-check" aria-hidden="true"></i>
            <p>No se registran suscripciones previas para {{ alumno()?.nombre }}.</p>
          </div>
        </div>
      </section>

    </div>
    }
  </main>
</div>
`;
fs.writeFileSync(path.join(root, 'features', 'programar-recarga', 'programar-recarga.component.html'), htmlContent);

// 4. Update app.routes.ts
const routesFile = path.join(root, 'app.routes.ts');
let routesContent = fs.readFileSync(routesFile, 'utf8');
if (!routesContent.includes('programar-recarga')) {
  const insertStr = `
              {
                path: 'programar-recarga',
                canActivate: [alumnoContextoGuard],
                loadComponent: () =>
                  import('./features/programar-recarga/programar-recarga.component').then((m) => m.ProgramarRecargaComponent)
              },`;
  routesContent = routesContent.replace(
    /{[\s]*path:\s*'acreditar-mercado-pago'[\s\S]*?},/,
    match => match + insertStr
  );
  fs.writeFileSync(routesFile, routesContent);
}

// 5. Update alumno-card.component.ts
const cardTsFile = path.join(root, 'features', 'home-tutor', 'components', 'alumno-card', 'alumno-card.component.ts');
let cardTs = fs.readFileSync(cardTsFile, 'utf8');
cardTs = cardTs.replace(/programarRecarga\(\) \{[\s\S]*?iniciarRecargaProgramada\(monto: number\) \{[\s\S]*?\}\s*\}/, "programarRecarga() { this.navegar('/programar-recarga'); }");
fs.writeFileSync(cardTsFile, cardTs);

console.log('DONE');
