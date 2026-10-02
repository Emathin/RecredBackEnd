const fs = require('fs');
const p = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\app.routes.ts';
let text = fs.readFileSync(p, 'utf8');

text = text.replace(/\{\s*\{\s*path: 'colaborador\/aceptar-invitacion\/:token'/g, "{\n    path: 'colaborador/aceptar-invitacion/:token'");

fs.writeFileSync(p, text, 'utf8');
