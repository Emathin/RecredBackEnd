const fs = require('fs');
const tsPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.ts';
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';
const cssPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.css';

const tsContent = `import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ColaboradorService } from '../../data-access/services/colaborador.service';
import { ToastService } from '../../shared/services/toast.service';

@Component({
  selector: 'app-aceptar-invitacion-colaborador',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './aceptar-invitacion-colaborador.html',
  styleUrl: './aceptar-invitacion-colaborador.css',
})
export class AceptarInvitacionColaborador implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly colaboradorService = inject(ColaboradorService);
  private readonly toastService = inject(ToastService);

  token = '';
  cargando = signal(false);

  form = new FormGroup({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    apellido: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    dni: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    telefono: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(6)] }),
  });

  ngOnInit() {
    this.token = this.route.snapshot.paramMap.get('token') || '';
  }

  async submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    
    this.cargando.set(true);
    try {
      await this.colaboradorService.aceptarInvitacion({
        token: this.token,
        ...this.form.value
      });
      this.toastService.mostrar('¡Cuenta creada exitosamente! Ya podés iniciar sesión.', 'success', 8000);
      this.router.navigate(['/seleccion-tipo-cuenta']);
    } catch (err: any) {
      const msg = err?.error?.message || err.message || 'Error al procesar la invitación';
      this.toastService.mostrar(msg, 'error');
    } finally {
      this.cargando.set(false);
    }
  }
}
`;

const htmlContent = `<div class="page-colaborador">
  <div class="card-colaborador">
    <div class="header-colaborador">
      <i class="fa-solid fa-user-plus"></i>
      <h1>Registro de Colaborador</h1>
      <p>Completá tus datos para aceptar la invitación y empezar a colaborar.</p>
    </div>

    <form [formGroup]="form" (submit)="$event.preventDefault(); submit()" class="form-colaborador">
      
      <div class="form-group">
        <label>Nombre</label>
        <input type="text" formControlName="nombre" placeholder="Tu nombre" />
      </div>

      <div class="form-group">
        <label>Apellido</label>
        <input type="text" formControlName="apellido" placeholder="Tu apellido" />
      </div>

      <div class="form-group">
        <label>DNI</label>
        <input type="text" formControlName="dni" placeholder="Tu DNI" />
      </div>

      <div class="form-group">
        <label>Teléfono</label>
        <input type="tel" formControlName="telefono" placeholder="Tu teléfono" />
      </div>

      <div class="form-group">
        <label>Contraseña</label>
        <input type="password" formControlName="password" placeholder="Tu contraseña" />
      </div>

      <button type="submit" class="btn-submit" [disabled]="cargando()">
        @if (cargando()) {
          Enviando...
        } @else {
          Registrarme
        }
      </button>

    </form>
  </div>
</div>
`;

const cssContent = `.page-colaborador {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: var(--color-surface, #f9fafb);
  padding: 1rem;
}

.card-colaborador {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.header-colaborador {
  text-align: center;
  margin-bottom: 2rem;
}

.header-colaborador i {
  font-size: 3rem;
  color: var(--color-primary, #10b981);
  margin-bottom: 1rem;
}

.header-colaborador h1 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: var(--color-text, #111827);
}

.header-colaborador p {
  color: #6b7280;
  font-size: 0.875rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #374151;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 1rem;
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-primary, #10b981);
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.btn-submit {
  width: 100%;
  background-color: var(--color-primary, #10b981);
  color: white;
  padding: 0.75rem;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  margin-top: 1rem;
  transition: background-color 0.2s;
}

.btn-submit:hover {
  background-color: var(--color-primary-dark, #059669);
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
`;

fs.writeFileSync(tsPath, tsContent, 'utf8');
fs.writeFileSync(htmlPath, htmlContent, 'utf8');
fs.writeFileSync(cssPath, cssContent, 'utf8');
console.log("Component files updated.");
