const fs = require('fs');
const p = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\perfil-usuario\\perfil-usuario.page.html';
let text = fs.readFileSync(p, 'utf8');

text = text.replace('(ngSubmit)="invitarColaborador()"', '(submit)="$event.preventDefault(); invitarColaborador()"');

fs.writeFileSync(p, text, 'utf8');
