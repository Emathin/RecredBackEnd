const fs = require('fs');
const filePath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\core\\auth\\interceptors\\auth.interceptor.ts';
let content = fs.readFileSync(filePath, 'utf8');

const regex = /const esPrepararCuentaTutor =[\s\S]*?\.test\(req\.url\);/;
const replacement = `const esPrepararCuentaTutor =
    req.method === 'POST' && /\\/invitaciones\\/tutor\\/[^/]+\\/preparar-cuenta$/.test(req.url);
  const esAceptarInvitacionColaborador =
    req.method === 'POST' && req.url.endsWith('/tutores/colaboradores/aceptar');`;

content = content.replace(regex, replacement);

const ifRegex = /if \(!esApiPropia \|\| esRegistroColegioPublico \|\| esValidarInvitacionTutor \|\| esPrepararCuentaTutor\)/;
const ifReplacement = `if (!esApiPropia || esRegistroColegioPublico || esValidarInvitacionTutor || esPrepararCuentaTutor || esAceptarInvitacionColaborador)`;

content = content.replace(ifRegex, ifReplacement);

fs.writeFileSync(filePath, content, 'utf8');
