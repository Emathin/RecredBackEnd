const fs = require('fs');
const p = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\perfil-usuario\\perfil-usuario.page.ts';
let text = fs.readFileSync(p, 'utf8');
text = text.replace(/this\.toastService\.mostrar\(`Invitaci[^`]+`,\s*'success',\s*10000\);/g, "this.toastService.mostrar(`Invitaci\\u00F3n enviada a ${email}. IMPORTANTE: El usuario no aparecer\\u00E1 en la lista hasta que acepte.`, 'success', 10000);");
fs.writeFileSync(p, text, 'utf8');
