const fs = require('fs');
const tsPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\perfil-usuario\\perfil-usuario.page.ts';
let tsContent = fs.readFileSync(tsPath, 'utf8');

tsContent = tsContent.replace('this.colaboradores.set(colabs);', 'this.colaboradores.set(colabs.filter(c => c.activa !== false));');
fs.writeFileSync(tsPath, tsContent, 'utf8');
