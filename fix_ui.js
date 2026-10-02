const fs = require('fs');
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';
const cssPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.css';

const htmlContent = `<div class="page-colaborador">
  <div class="card-colaborador">
    <div class="header-colaborador">
      <i class="fa-solid fa-user-plus"></i>
      <h1>Registro de Colaborador</h1>
      <p>Complet&aacute; tus datos para aceptar la invitaci&oacute;n y empezar a colaborar.</p>
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
        <label>Tel&eacute;fono</label>
        <input type="tel" formControlName="telefono" placeholder="Tu tel&eacute;fono" />
      </div>

      <div class="form-group">
        <label>Contrase&ntilde;a</label>
        <input type="password" formControlName="password" placeholder="Tu contrase&ntilde;a" />
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
</div>`;

const cssContent = `.page-colaborador {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: var(--color-fondo, #f4f6f9);
  padding: 1rem;
  font-family: var(--font-principal, 'Inter', sans-serif);
}

.card-colaborador {
  background-color: var(--color-superficie, #ffffff);
  padding: 2.5rem;
  border-radius: 18px;
  box-shadow: var(--shadow-card, 0 4px 12px rgba(0, 0, 0, 0.05));
  width: 100%;
  max-width: 420px;
  border: 1px solid var(--color-borde, #e2e8f0);
}

.header-colaborador {
  text-align: center;
  margin-bottom: 2rem;
}

.header-colaborador i {
  font-size: 3rem;
  color: var(--color-pizarra, #4b6a9b);
  margin-bottom: 1rem;
}

.header-colaborador h1 {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  color: var(--color-text-main, #1e293b);
}

.header-colaborador p {
  color: var(--color-text-muted, #64748b);
  font-size: 0.9rem;
  line-height: 1.5;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  color: var(--color-pizarra, #4b6a9b);
}

.form-group input {
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border: 1px solid var(--color-borde-fuerte, #cbd5e1);
  border-radius: 12px;
  font-size: 1rem;
  background-color: var(--color-superficie, #ffffff);
  color: var(--color-text-main, #1e293b);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-pizarra, #4b6a9b);
  box-shadow: 0 0 0 4px var(--color-foco, rgba(75, 106, 155, 0.15));
}

.btn-submit {
  width: 100%;
  height: 44px;
  background-color: var(--color-pizarra, #4b6a9b);
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  margin-top: 1rem;
  transition: background-color 0.2s, transform 0.1s;
  box-shadow: 0 4px 12px rgba(75, 106, 155, 0.32);
}

.btn-submit:hover:not(:disabled) {
  background-color: var(--color-pizarra-dark, #3a537a);
  transform: translateY(-1px);
}

.btn-submit:active:not(:disabled) {
  transform: translateY(1px);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}
`;

fs.writeFileSync(htmlPath, htmlContent, 'utf8');
fs.writeFileSync(cssPath, cssContent, 'utf8');
