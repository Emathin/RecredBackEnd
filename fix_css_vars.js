const fs = require('fs');
const cssPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.css';
let cssContent = fs.readFileSync(cssPath, 'utf8');

cssContent = cssContent.replace(/--color-text-main/g, '--color-texto-oscuro');
cssContent = cssContent.replace(/--color-text-muted/g, '--color-texto-claro');
cssContent = cssContent.replace(/#1e293b/g, '#334155');
cssContent = cssContent.replace(/#64748b/g, '#94A3B8');

fs.writeFileSync(cssPath, cssContent, 'utf8');
