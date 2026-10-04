const fs = require('fs');
const path = require('path');

const frontendPath = '../RecredFrontEnd';

// 1. Fix perfil-usuario.page.ts
const perfilTsPath = path.join(frontendPath, 'src/app/features/perfil-usuario/perfil-usuario.page.ts');
let content = fs.readFileSync(perfilTsPath, 'utf8');

const importResolution = `import { Colaboradores } from './components/colaboradores/colaboradores';
import { ColaboradorService, ColaboradorDTO } from '../../data-access/services/colaborador.service';
import { AlumnosService } from '../../data-access/services/alumnos.service';`;

const regexPerfil = /<<<<<<< HEAD\r?\nimport { Colaboradores }.*?\r?\n=======\r?\nimport { ColaboradorService.*?\r?\nimport { AlumnosService.*?\r?\n>>>>>>> [a-f0-9]+/s;
content = content.replace(regexPerfil, importResolution);
fs.writeFileSync(perfilTsPath, content, 'utf8');

// 2. Fix alumno-card.component.html
const alumnoHtmlPath = path.join(frontendPath, 'src/app/features/home-tutor/components/alumno-card/alumno-card.component.html');
content = fs.readFileSync(alumnoHtmlPath, 'utf8');

const cardResolution = `    @if (!esColaborador) {
    <a class="alumno-card__btn alumno-card__btn--azul" (click)="navegar('/programar-recarga')" (keyup.enter)="navegar('/programar-recarga')"
      [class.active]="isActive('/programar-recarga')" tabindex="0">
      <span class="alumno-card__btn-icono" aria-hidden="true">
        <i class="fa-solid fa-calendar-plus"></i>
      </span>
      <span class="alumno-card__btn-label">Programar</span>
    </a>
    }

    @if (!esColaborador) {
    <a class="alumno-card__btn alumno-card__btn--azul" (click)="navegar('/restricciones-horarias')" (keyup.enter)="navegar('/restricciones-horarias')" tabindex="0"
      [class.active]="isActive('/restricciones-horarias')">`;

const regexCard = /<<<<<<< HEAD\r?\n    <a class="alumno-card__btn alumno-card__btn--menta" \(click\)="navegar\('\/programar-recarga'\)".*?>>>>>>> [a-f0-9]+\r?\n      \[class\.active\]="isActive\('\/restricciones-horarias'\)">/s;
content = content.replace(regexCard, cardResolution);
fs.writeFileSync(alumnoHtmlPath, content, 'utf8');
