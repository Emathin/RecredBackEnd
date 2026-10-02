const fs = require('fs');
const p = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\perfil-usuario\\perfil-usuario.page.ts';
let text = fs.readFileSync(p, 'utf8');

const searchStr = "this.toastService.mostrar(\Invitaci\ufffdn enviada a \\, 'success');";
const replaceStr = "this.toastService.mostrar(\Invitaci\u00f3n enviada a \. IMPORTANTE: El colaborador no aparecer\u00e1 en la lista hasta que la acepte.\, 'success', 8000);";

if (text.includes("this.toastService.mostrar(\Invitaci")) {
    text = text.replace(/this\.toastService\.mostrar\(\Invitaci.*?,\s*'success'\);/, replaceStr);
}

fs.writeFileSync(p, text, 'utf8');
