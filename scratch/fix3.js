const fs = require('fs');
const p = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\perfil-usuario\\perfil-usuario.page.ts';
let text = fs.readFileSync(p, 'utf8');
const lines = text.split('\n');
for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('this.toastService.mostrar') && lines[i].includes('')) {
        lines[i] = "      this.toastService.mostrar(Invitación enviada a \. IMPORTANTE: El colaborador no aparecerá en la lista hasta que la acepte., 'success', 8000);";
    }
}
fs.writeFileSync(p, lines.join('\n'), 'utf8');
