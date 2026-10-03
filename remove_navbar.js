const fs = require('fs');
const tsPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.ts';
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';

let tsContent = fs.readFileSync(tsPath, 'utf8');
tsContent = tsContent.replace("import { NavbarComponent } from '../../shared/components/navbar/navbar.component';\n", '');
tsContent = tsContent.replace(', NavbarComponent]', ']');
fs.writeFileSync(tsPath, tsContent, 'utf8');

let htmlContent = fs.readFileSync(htmlPath, 'utf8');
htmlContent = htmlContent.replace('<app-navbar userName="" />\n\n', '');
fs.writeFileSync(htmlPath, htmlContent, 'utf8');

console.log("Navbar removed.");
