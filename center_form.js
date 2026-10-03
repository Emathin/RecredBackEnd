const fs = require('fs');
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';
let htmlContent = fs.readFileSync(htmlPath, 'utf8');

// Center the grid content by forcing a single centered column on desktop
htmlContent = htmlContent.replace('<section class="perfil-usuario__grid" aria-label="Datos del usuario">', '<section class="perfil-usuario__grid" aria-label="Datos del usuario" style="grid-template-columns: minmax(100%, 600px); justify-content: center; max-width: 600px; margin: 0 auto;">');

fs.writeFileSync(htmlPath, htmlContent, 'utf8');
