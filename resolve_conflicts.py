import os

frontend_path = "../RecredFrontEnd"

# 1. Fix perfil-usuario.page.ts
perfil_ts_path = os.path.join(frontend_path, "src/app/features/perfil-usuario/perfil-usuario.page.ts")
with open(perfil_ts_path, 'r', encoding='utf-8') as f:
    content = f.read()

import_resolution = """import { Colaboradores } from './components/colaboradores/colaboradores';
import { ColaboradorService, ColaboradorDTO } from '../../data-access/services/colaborador.service';
import { AlumnosService } from '../../data-access/services/alumnos.service';"""

import re
content = re.sub(r'<<<<<<< HEAD\nimport { Colaboradores }.*?\n=======\nimport { ColaboradorService.*?\nimport { AlumnosService.*?\n>>>>>>> [a-f0-9]+', import_resolution, content, flags=re.DOTALL)

with open(perfil_ts_path, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. Fix alumno-card.component.html
alumno_html_path = os.path.join(frontend_path, "src/app/features/home-tutor/components/alumno-card/alumno-card.component.html")
with open(alumno_html_path, 'r', encoding='utf-8') as f:
    content = f.read()

card_resolution = """    @if (!esColaborador) {
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
      [class.active]="isActive('/restricciones-horarias')">"""

content = re.sub(r'<<<<<<< HEAD\n    <a class="alumno-card__btn alumno-card__btn--menta" \(click\)="navegar\(\'/programar-recarga\'\)".*?>>>>>>> [a-f0-9]+\n      \[class\.active\]="isActive\(\'/restricciones-horarias\'\)">', card_resolution, content, flags=re.DOTALL)

with open(alumno_html_path, 'w', encoding='utf-8') as f:
    f.write(content)
