const fs = require('fs');
const tsPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.ts';
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';

const tsContent = `import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ColaboradorService } from '../../data-access/services/colaborador.service';
import { ToastService } from '../../shared/services/toast.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-aceptar-invitacion-colaborador',
  standalone: true,
  imports: [ReactiveFormsModule, NavbarComponent],
  templateUrl: './aceptar-invitacion-colaborador.html',
  styleUrl: '../perfil-usuario/perfil-usuario.page.css',
})
export class AceptarInvitacionColaborador implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly colaboradorService = inject(ColaboradorService);
  private readonly toastService = inject(ToastService);

  token = '';
  cargando = signal(false);
  fotoPreview = signal<string | null>(null);

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

  onFotoSeleccionada(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => this.fotoPreview.set(e.target?.result as string);
      reader.readAsDataURL(file);
    }
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

const htmlContent = `<app-navbar userName="" />

<main class="perfil-usuario">
  <div class="perfil-usuario__container">
    
    <header class="perfil-usuario__header">
      <div class="perfil-usuario__avatar-wrapper" aria-label="Foto de perfil">
        @if (fotoPreview()) {
          <img class="perfil-usuario__avatar perfil-usuario__avatar--foto" [src]="fotoPreview()!" alt="Tu foto" />
        } @else {
          <div class="perfil-usuario__avatar" aria-hidden="true" style="font-size: 1.5rem;">
            <i class="fa-solid fa-camera"></i>
          </div>
        }
        <button type="button" class="perfil-usuario__avatar-editar" (click)="fotoUploadInput.click()">
          <i class="fa-solid fa-pen" aria-hidden="true"></i>
        </button>
        <input #fotoUploadInput type="file" accept="image/*" class="perfil-usuario__input-foto"
          aria-hidden="true" tabindex="-1" (change)="onFotoSeleccionada($event)" />
      </div>
      
      <div class="perfil-usuario__heading">
        <p class="perfil-usuario__eyebrow">CUENTA RECRED</p>
        <h1 class="perfil-usuario__titulo">Registro de Colaborador</h1>
        <p class="perfil-usuario__subtitulo">
          Complet&aacute; tus datos para aceptar la invitaci&oacute;n y empezar a colaborar.
        </p>
      </div>
    </header>

    <section class="perfil-usuario__grid" aria-label="Datos del usuario">
      <article class="perfil-usuario__panel">
        <h2 class="perfil-usuario__panel-titulo">Invitaci&oacute;n</h2>
        
        <dl class="perfil-usuario__datos">
          <div>
            <dt>Rol asignado</dt>
            <dd>Tutor Colaborador</dd>
          </div>
          <div>
            <dt>Estado</dt>
            <dd>Pendiente de registro</dd>
          </div>
        </dl>

        <div style="margin-top: 1.5rem; color: var(--color-texto-claro); font-size: 0.9rem; line-height: 1.5;">
          Al registrarte, podr&aacute;s gestionar y ver la informaci&oacute;n de los hijos a los que te hayan dado acceso.
        </div>
      </article>

      <article class="perfil-usuario__panel">
        <h2 class="perfil-usuario__panel-titulo">Perfil completo</h2>
        <form [formGroup]="form" (submit)="$event.preventDefault(); submit()" class="perfil-usuario__form">
          <fieldset class="perfil-usuario__fieldset" [disabled]="cargando()">
            
            <label class="perfil-usuario__campo">
              <span>Nombre</span>
              <input type="text" formControlName="nombre" placeholder="Tu nombre" />
            </label>

            <label class="perfil-usuario__campo">
              <span>Apellido</span>
              <input type="text" formControlName="apellido" placeholder="Tu apellido" />
            </label>

            <label class="perfil-usuario__campo">
              <span>DNI</span>
              <input type="text" formControlName="dni" placeholder="Tu DNI" />
            </label>

            <label class="perfil-usuario__campo">
              <span>Tel&eacute;fono</span>
              <input type="tel" formControlName="telefono" placeholder="Tu tel&eacute;fono" />
            </label>

            <label class="perfil-usuario__campo">
              <span>Contrase&ntilde;a</span>
              <input type="password" formControlName="password" placeholder="Tu contrase&ntilde;a" />
            </label>

            <div class="perfil-usuario__acciones">
              <button type="button" class="perfil-usuario__boton perfil-usuario__boton--secundario" (click)="form.reset()" [disabled]="cargando()">
                Descartar
              </button>
              <button type="submit" class="perfil-usuario__boton perfil-usuario__boton--primario" [disabled]="cargando() || form.invalid">
                @if (cargando()) {
                  Guardando...
                } @else {
                  Guardar cambios
                }
              </button>
            </div>

          </fieldset>
        </form>
      </article>
    </section>

  </div>
</main>
`;

fs.writeFileSync(tsPath, tsContent, 'utf8');
fs.writeFileSync(htmlPath, htmlContent, 'utf8');
console.log("Updated to match Perfil style");
