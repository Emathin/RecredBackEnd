const fs = require('fs');
const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.html';
let htmlContent = fs.readFileSync(htmlPath, 'utf8');

// Replace the form tag to include a wrapper
htmlContent = htmlContent.replace('<form [formGroup]="form" (submit)="$event.preventDefault(); submit()" class="perfil-usuario__form" style="max-width: 350px; margin: 0 auto; width: 100%;">', '<div style="display: flex; justify-content: center; width: 100%;"><form [formGroup]="form" (submit)="$event.preventDefault(); submit()" class="perfil-usuario__form" style="max-width: 350px; width: 100%;">');

// Close the wrapper div
htmlContent = htmlContent.replace('</form>\r\n      </article>', '</form>\r\n        </div>\r\n      </article>');
// Also handle Unix line endings just in case
htmlContent = htmlContent.replace('</form>\n      </article>', '</form>\n        </div>\n      </article>');

fs.writeFileSync(htmlPath, htmlContent, 'utf8');
