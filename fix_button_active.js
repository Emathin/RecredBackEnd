const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredFrontEnd\\\\src\\\\app\\\\features\\\\home-tutor\\\\components\\\\alumno-card\\\\alumno-card.component.html';
let c = fs.readFileSync(file, 'utf8');

const target = `<a class="alumno-card__btn alumno-card__btn--menta" (click)="programarRecarga()" tabindex="0">`;
const replacement = `<a class="alumno-card__btn alumno-card__btn--menta" (click)="navegar('/programar-recarga')"
      [class.active]="isActive('/programar-recarga')" tabindex="0">`;

if (c.includes(target)) {
    c = c.replace(target, replacement);
    fs.writeFileSync(file, c);
    console.log('Fixed active class in HTML');
} else {
    console.log('Target not found!');
}
