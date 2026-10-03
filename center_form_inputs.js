const fs = require('fs');
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';
let htmlContent = fs.readFileSync(htmlPath, 'utf8');

// 1. Revert the grid inline styles
htmlContent = htmlContent.replace('<section class="perfil-usuario__grid" aria-label="Datos del usuario" style="grid-template-columns: minmax(100%, 600px); justify-content: center; max-width: 600px; margin: 0 auto;">', '<section class="perfil-usuario__grid" aria-label="Datos del usuario">');

// 2. Center the form inside its panel
htmlContent = htmlContent.replace('class="perfil-usuario__form">', 'class="perfil-usuario__form" style="max-width: 350px; margin: 0 auto; width: 100%;">');

fs.writeFileSync(htmlPath, htmlContent, 'utf8');
