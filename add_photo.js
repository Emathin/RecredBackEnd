const fs = require('fs');
const tsPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.ts';
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';
const cssPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.css';

let tsContent = fs.readFileSync(tsPath, 'utf8');
let htmlContent = fs.readFileSync(htmlPath, 'utf8');
let cssContent = fs.readFileSync(cssPath, 'utf8');

// Update TS to add photo logic
if (!tsContent.includes('fotoPreview')) {
    tsContent = tsContent.replace('cargando = signal(false);', `cargando = signal(false);\n  fotoPreview = signal<string | null>(null);`);
    const newMethod = `
  onFotoSeleccionada(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => this.fotoPreview.set(e.target?.result as string);
      reader.readAsDataURL(file);
    }
  }

  async submit`;
    tsContent = tsContent.replace('async submit', newMethod);
    fs.writeFileSync(tsPath, tsContent, 'utf8');
}

// Update HTML to add logo and photo selector
htmlContent = `<div class="page-colaborador">
  <div class="card-colaborador">
    
    <div class="logo-container">
      <img src="assets/images/campana.svg" alt="Recred Logo" class="logo-campana" onerror="this.src='logoHeader.png'" />
      <span class="logo-text">RECRED</span>
    </div>

    <div class="header-colaborador">
      <div class="avatar-upload-container">
        <label for="fotoUpload" class="avatar-upload-label" [class.has-foto]="fotoPreview()">
          @if (fotoPreview()) {
            <img [src]="fotoPreview()" alt="Tu foto" class="avatar-preview" />
          } @else {
            <div class="avatar-placeholder">
              <i class="fa-solid fa-camera"></i>
              <span>Subir foto</span>
            </div>
          }
        </label>
        <input type="file" id="fotoUpload" accept="image/*" (change)="onFotoSeleccionada($event)" hidden />
      </div>
      
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
fs.writeFileSync(htmlPath, htmlContent, 'utf8');

// Update CSS
cssContent = cssContent + `

.logo-container {
  display: flex;
  align-items: center;
  gap: 8px;
  position: absolute;
  top: 1.5rem;
  left: 1.5rem;
}

.card-colaborador {
  position: relative;
  padding-top: 4.5rem;
}

.logo-campana {
  width: 24px;
  height: 24px;
}

.logo-text {
  font-weight: 800;
  font-size: 1.125rem;
  color: var(--color-dorado, #F2B705);
  letter-spacing: 0.5px;
}

.avatar-upload-container {
  display: flex;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.avatar-upload-label {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background-color: var(--color-superficie-alt, #f8fafc);
  border: 2px dashed var(--color-borde-fuerte, #cbd5e1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s ease;
}

.avatar-upload-label:hover {
  border-color: var(--color-pizarra, #4b6a9b);
  background-color: var(--color-foco, rgba(75, 106, 155, 0.1));
}

.avatar-upload-label.has-foto {
  border: 2px solid var(--color-pizarra, #4b6a9b);
}

.avatar-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: var(--color-texto-claro, #94a3b8);
  gap: 4px;
}

.avatar-placeholder i {
  font-size: 1.5rem;
  color: var(--color-texto-claro, #94a3b8);
  margin-bottom: 0;
}

.avatar-placeholder span {
  font-size: 0.7rem;
  font-weight: 600;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
`;
fs.writeFileSync(cssPath, cssContent, 'utf8');

console.log('Update finished');
