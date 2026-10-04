const fs = require('fs');

const file = 'C:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\home-tutor\\components\\alumno-card\\alumno-card.component.html';
let content = fs.readFileSync(file, 'utf8');
const target = '<span class="alumno-card__btn-label">Acreditar</span>\r\n    </a>';
const target2 = '<span class="alumno-card__btn-label">Acreditar</span>\n    </a>';

if (!content.includes('programarRecarga()')) {
    if (content.includes(target) || content.includes(target2)) {
        const replaceStr = target.includes('\r\n') ? target : target2;
        content = content.replace(replaceStr, replaceStr + '\n\n    <a class="alumno-card__btn alumno-card__btn--menta" (click)="programarRecarga()" tabindex="0">\n      <span class="alumno-card__btn-icono" aria-hidden="true">\n        <i class="fa-solid fa-calendar-plus"></i>\n      </span>\n      <span class="alumno-card__btn-label">Programar</span>\n    </a>');
        fs.writeFileSync(file, content);
        console.log('HTML Modificado exitosamente.');
    } else {
        console.log('No encontre el target en el HTML.');
    }
} else {
    console.log('El boton programar ya existe.');
}
