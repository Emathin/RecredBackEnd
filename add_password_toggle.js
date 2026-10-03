const fs = require('fs');
const tsPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.ts';
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';

// 1. Update TS File
let tsContent = fs.readFileSync(tsPath, 'utf8');
if (!tsContent.includes('mostrarPassword')) {
    tsContent = tsContent.replace('fotoPreview = signal<string | null>(null);', 'fotoPreview = signal<string | null>(null);\n  mostrarPassword = signal(false);');
    fs.writeFileSync(tsPath, tsContent, 'utf8');
}

// 2. Update HTML File
let htmlContent = fs.readFileSync(htmlPath, 'utf8');
const oldPasswordBlock = `<label class="perfil-usuario__campo">
              <span>Contrase&ntilde;a</span>
              <input type="password" formControlName="password" placeholder="Tu contrase&ntilde;a" />
            </label>`;

const newPasswordBlock = `<label class="perfil-usuario__campo" style="display: flex; flex-direction: column; gap: 8px;">
              <span>Contrase&ntilde;a</span>
              <div class="pw-modal__input-wrapper" style="width: 100%;">
                <input [type]="mostrarPassword() ? 'text' : 'password'" formControlName="password" class="pw-modal__input" placeholder="Crea tu contrase&ntilde;a" style="background: transparent;" />
                <button type="button" class="pw-modal__ojo" (click)="mostrarPassword.set(!mostrarPassword())" [attr.aria-label]="mostrarPassword() ? 'Ocultar' : 'Mostrar'">
                  <i [class]="mostrarPassword() ? 'fa-solid fa-eye-slash' : 'fa-solid fa-eye'" aria-hidden="true" style="color: var(--color-texto-claro);"></i>
                </button>
              </div>
            </label>`;

if (htmlContent.includes('<input type="password" formControlName="password" placeholder="Crea tu contrase&ntilde;a" />')) {
  // It says "Crea tu contraseña" in the current HTML. Let's do a more robust replace using regex.
}

htmlContent = htmlContent.replace(/<label class="perfil-usuario__campo">\s*<span>Contrase&ntilde;a<\/span>\s*<input type="password" formControlName="password"[^>]*>\s*<\/label>/, newPasswordBlock);

fs.writeFileSync(htmlPath, htmlContent, 'utf8');

console.log('Password toggle added');
