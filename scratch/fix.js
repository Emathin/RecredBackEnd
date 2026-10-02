const fs = require('fs');
const p = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\perfil-usuario\\perfil-usuario.page.ts';
let text = fs.readFileSync(p, 'utf8');

text = text.replace(/this\.toastService\.mostrar\(Invitaci[^,]+,\s*'success',\s*8000\);/g, "this.toastService.mostrar('Perfil actualizado correctamente.', 'success');");

text = text.replace(/this\.toastService\.mostrar\(Invitaci[^]+,\s*'success'\);/, "this.toastService.mostrar(Invitación enviada a . IMPORTANTE: El colaborador no aparecerá en la lista hasta que la acepte., 'success', 8000);");

fs.writeFileSync(p, text, 'utf8');
