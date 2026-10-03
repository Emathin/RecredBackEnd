const fs = require('fs');
const tsPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.ts';
let tsContent = fs.readFileSync(tsPath, 'utf8');

// Replace mangled text with unicode escape sequences
tsContent = tsContent.replace(/¡Cuenta creada exitosamente! Ya pod.*s iniciar sesi.*n./, '¡Cuenta creada exitosamente! Ya pod\\u00E9s iniciar sesi\\u00F3n.');
fs.writeFileSync(tsPath, tsContent, 'utf8');
