const fs = require('fs');
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';
let htmlContent = fs.readFileSync(htmlPath, 'utf8');

// Change max-width from 350px to 100% (or remove it so it stretches fully, maybe a generous 500px so it's not absurdly wide on big screens)
htmlContent = htmlContent.replace('style="max-width: 350px; width: 100%;"', 'style="max-width: 100%; width: 100%;"');

fs.writeFileSync(htmlPath, htmlContent, 'utf8');
