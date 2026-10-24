const fs = require('fs');

// 1. home-alumno.page.css
const homeAlumnoPath = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/home-alumno/home-alumno.page.css';
const homeAlumnoDark = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .home-alumno,
[data-theme="dark"] .home-alumno {
  background:
    radial-gradient(circle at 18% 12%, rgba(242, 183, 5, 0.08), transparent 28%),
    radial-gradient(circle at 80% 25%, rgba(74, 111, 165, 0.12), transparent 35%),
    linear-gradient(180deg, rgba(30, 41, 59, 0.65) 0%, rgba(15, 23, 42, 0) 260px),
    var(--color-fondo, #0F172A) !important;
}
`;
fs.appendFileSync(homeAlumnoPath, homeAlumnoDark, 'utf8');
console.log('1. Updated home-alumno.page.css');

// 2. perfil-header.component.css
const perfilHeaderPath = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/home-alumno/components/perfil-header/perfil-header.component.css';
const perfilHeaderDark = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .perfil-header,
[data-theme="dark"] .perfil-header {
  border-color: rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.35) !important;
}

:host-context([data-theme="dark"]) .perfil-header--fondo-nubes,
[data-theme="dark"] .perfil-header--fondo-nubes {
  background:
    linear-gradient(135deg, rgba(30, 41, 59, 0.95), rgba(15, 23, 42, 0.92)),
    #1E293B !important;
  box-shadow:
    var(--shadow-card),
    inset 0 0 60px rgba(56, 189, 248, 0.12),
    inset 0 0 20px rgba(56, 189, 248, 0.08) !important;
}

:host-context([data-theme="dark"]) .perfil-header--fondo-minecraft,
[data-theme="dark"] .perfil-header--fondo-minecraft {
  background:
    linear-gradient(135deg, rgba(20, 40, 20, 0.95), rgba(30, 41, 59, 0.92)),
    #1E293B !important;
  box-shadow:
    var(--shadow-card),
    inset 0 0 60px rgba(74, 222, 128, 0.12) !important;
}

:host-context([data-theme="dark"]) .perfil-header--fondo-dragonballz,
[data-theme="dark"] .perfil-header--fondo-dragonballz {
  background:
    linear-gradient(135deg, rgba(45, 25, 15, 0.95), rgba(30, 41, 59, 0.92)),
    #1E293B !important;
  box-shadow:
    var(--shadow-card),
    inset 0 0 60px rgba(251, 146, 60, 0.12) !important;
}

:host-context([data-theme="dark"]) .perfil-header--fondo-gato,
[data-theme="dark"] .perfil-header--fondo-gato {
  background:
    linear-gradient(135deg, rgba(45, 20, 30, 0.95), rgba(30, 41, 59, 0.92)),
    #1E293B !important;
  box-shadow:
    var(--shadow-card),
    inset 0 0 60px rgba(244, 114, 182, 0.12) !important;
}

:host-context([data-theme="dark"]) .perfil-header--fondo-messi,
[data-theme="dark"] .perfil-header--fondo-messi {
  background:
    linear-gradient(135deg, rgba(15, 30, 60, 0.95), rgba(30, 41, 59, 0.92)),
    #1E293B !important;
  box-shadow:
    var(--shadow-card),
    inset 0 0 60px rgba(96, 165, 250, 0.12) !important;
}

:host-context([data-theme="dark"]) .perfil-header__nombre,
[data-theme="dark"] .perfil-header__nombre {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .perfil-header__saludo,
[data-theme="dark"] .perfil-header__saludo {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .perfil-header__subtitulo,
[data-theme="dark"] .perfil-header__subtitulo {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .perfil-header__avatar,
[data-theme="dark"] .perfil-header__avatar {
  box-shadow:
    0 0 0 4px #1E293B,
    0 0 0 7px var(--color-dorado),
    0 14px 28px rgba(0, 0, 0, 0.4) !important;
}

:host-context([data-theme="dark"]) .perfil-header__saldo,
[data-theme="dark"] .perfil-header__saldo {
  color: #1e293b !important;
}

:host-context([data-theme="dark"]) .perfil-header__saldo-valor,
[data-theme="dark"] .perfil-header__saldo-valor {
  color: #1e293b !important;
}

:host-context([data-theme="dark"]) .perfil-header__saldo-label,
[data-theme="dark"] .perfil-header__saldo-label {
  color: #1e293b !important;
}

:host-context([data-theme="dark"]) .perfil-header__editar,
[data-theme="dark"] .perfil-header__editar {
  background-color: rgba(255, 255, 255, 0.1) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: #8FB4E6 !important;
}

:host-context([data-theme="dark"]) .perfil-header__editar:hover,
[data-theme="dark"] .perfil-header__editar:hover {
  background-color: rgba(255, 255, 255, 0.18) !important;
}

:host-context([data-theme="dark"]) .perfil-header__menu,
[data-theme="dark"] .perfil-header__menu {
  background-color: #1E293B !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.5) !important;
}

:host-context([data-theme="dark"]) .perfil-header__opcion:hover,
[data-theme="dark"] .perfil-header__opcion:hover {
  background-color: var(--color-superficie-alt, #334155) !important;
}

:host-context([data-theme="dark"]) .perfil-header__opcion-label,
[data-theme="dark"] .perfil-header__opcion-label {
  color: #F8FAFC !important;
}
`;
fs.appendFileSync(perfilHeaderPath, perfilHeaderDark, 'utf8');
console.log('2. Updated perfil-header.component.css');

// 3. puntos-recompensa-bar.component.css
const puntosPath = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/home-alumno/components/puntos-recompensa-bar/puntos-recompensa-bar.component.css';
const puntosDark = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .recompensas-bar,
[data-theme="dark"] .recompensas-bar {
  background-color: var(--color-superficie, #1E293B) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3) !important;
}

:host-context([data-theme="dark"]) .recompensas-bar:hover,
[data-theme="dark"] .recompensas-bar:hover {
  border-color: var(--color-dorado) !important;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.4) !important;
}

:host-context([data-theme="dark"]) .recompensas-bar__points,
[data-theme="dark"] .recompensas-bar__points {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .progress-container,
[data-theme="dark"] .progress-container {
  background-color: rgba(255, 255, 255, 0.1) !important;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}

:host-context([data-theme="dark"]) .recompensas-bar__footer,
[data-theme="dark"] .recompensas-bar__footer {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .mensaje-motivacional,
[data-theme="dark"] .mensaje-motivacional {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .badge-crack,
[data-theme="dark"] .badge-crack {
  background-color: rgba(242, 183, 5, 0.22) !important;
  color: var(--color-dorado) !important;
}

:host-context([data-theme="dark"]) .badge-principiante,
[data-theme="dark"] .badge-principiante {
  background-color: rgba(129, 178, 154, 0.22) !important;
  color: #81B29A !important;
}

:host-context([data-theme="dark"]) .badge-goat,
[data-theme="dark"] .badge-goat {
  background-color: rgba(124, 58, 237, 0.25) !important;
  color: #a78bfa !important;
}
`;
fs.appendFileSync(puntosPath, puntosDark, 'utf8');
console.log('3. Updated puntos-recompensa-bar.component.css');

// 4. acciones-grid.component.css
const gridPath = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/home-alumno/components/acciones-grid/acciones-grid.component.css';
const gridDark = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .acciones-grid__titulo,
[data-theme="dark"] .acciones-grid__titulo {
  color: #F8FAFC !important;
}
`;
fs.appendFileSync(gridPath, gridDark, 'utf8');
console.log('4. Updated acciones-grid.component.css');

// 5. accion-tile.component.css
const tilePath = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/home-alumno/components/accion-tile/accion-tile.component.css';
const tileDark = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .accion-tile,
[data-theme="dark"] .accion-tile {
  background-color: var(--color-superficie, #1E293B) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  box-shadow: var(--shadow-card) !important;
}

:host-context([data-theme="dark"]) .accion-tile:hover,
[data-theme="dark"] .accion-tile:hover {
  border-color: rgba(255, 255, 255, 0.18) !important;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.4) !important;
}

:host-context([data-theme="dark"]) .accion-tile--menta,
[data-theme="dark"] .accion-tile--menta {
  background: linear-gradient(180deg, rgba(129, 178, 154, 0.12) 0%, #1E293B 70%) !important;
  border-color: rgba(129, 178, 154, 0.28) !important;
}

:host-context([data-theme="dark"]) .accion-tile--mandarina,
[data-theme="dark"] .accion-tile--mandarina {
  background: linear-gradient(180deg, rgba(244, 162, 97, 0.12) 0%, #1E293B 70%) !important;
  border-color: rgba(244, 162, 97, 0.28) !important;
}

:host-context([data-theme="dark"]) .accion-tile--melocoton,
[data-theme="dark"] .accion-tile--melocoton {
  background: linear-gradient(180deg, rgba(231, 111, 81, 0.12) 0%, #1E293B 70%) !important;
  border-color: rgba(231, 111, 81, 0.28) !important;
}

:host-context([data-theme="dark"]) .accion-tile--pizarra,
[data-theme="dark"] .accion-tile--pizarra {
  background: linear-gradient(180deg, rgba(108, 142, 191, 0.14) 0%, #1E293B 70%) !important;
  border-color: rgba(108, 142, 191, 0.28) !important;
}

:host-context([data-theme="dark"]) .accion-tile--dorado,
[data-theme="dark"] .accion-tile--dorado {
  background: linear-gradient(180deg, rgba(242, 183, 5, 0.14) 0%, #1E293B 70%) !important;
  border-color: rgba(242, 183, 5, 0.28) !important;
}

:host-context([data-theme="dark"]) .accion-tile__label,
[data-theme="dark"] .accion-tile__label {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .accion-tile__descripcion,
[data-theme="dark"] .accion-tile__descripcion {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .accion-tile__flecha,
[data-theme="dark"] .accion-tile__flecha {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .accion-tile--menta .accion-tile__flecha,
[data-theme="dark"] .accion-tile--menta .accion-tile__flecha {
  background-color: rgba(129, 178, 154, 0.2) !important;
  color: #81B29A !important;
}

:host-context([data-theme="dark"]) .accion-tile--mandarina .accion-tile__flecha,
[data-theme="dark"] .accion-tile--mandarina .accion-tile__flecha {
  background-color: rgba(244, 162, 97, 0.2) !important;
  color: #f4a261 !important;
}

:host-context([data-theme="dark"]) .accion-tile--melocoton .accion-tile__flecha,
[data-theme="dark"] .accion-tile--melocoton .accion-tile__flecha {
  background-color: rgba(231, 111, 81, 0.2) !important;
  color: var(--color-melocoton) !important;
}

:host-context([data-theme="dark"]) .accion-tile__badge,
[data-theme="dark"] .accion-tile__badge {
  background-color: rgba(15, 23, 42, 0.8) !important;
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .accion-tile__ola,
[data-theme="dark"] .accion-tile__ola {
  opacity: 0.25 !important;
}
`;
fs.appendFileSync(tilePath, tileDark, 'utf8');
console.log('5. Updated accion-tile.component.css');

// 6. pedido-recreo-card.component.css
const pedidoPath = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/home-alumno/components/pedido-recreo-card/pedido-recreo-card.component.css';
const pedidoDark = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .pedido-recreo__pedido,
:host-context([data-theme="dark"]) .pedido-recreo__recreo,
[data-theme="dark"] .pedido-recreo__pedido,
[data-theme="dark"] .pedido-recreo__recreo {
  background-color: var(--color-superficie, #1E293B) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  box-shadow: var(--shadow-card) !important;
}

:host-context([data-theme="dark"]) .pedido-recreo__titulo,
[data-theme="dark"] .pedido-recreo__titulo {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pedido-recreo__total-valor,
[data-theme="dark"] .pedido-recreo__total-valor {
  color: var(--color-dorado) !important;
}

:host-context([data-theme="dark"]) .pedido-recreo__retira,
[data-theme="dark"] .pedido-recreo__retira {
  background-color: rgba(108, 142, 191, 0.16) !important;
  color: #8FB4E6 !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .pedido-recreo__chip,
[data-theme="dark"] .pedido-recreo__chip {
  background-color: var(--color-superficie-alt, #334155) !important;
  color: #F8FAFC !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .pedido-recreo__cta,
[data-theme="dark"] .pedido-recreo__cta {
  color: #1e293b !important;
}

:host-context([data-theme="dark"]) .pedido-recreo__horario,
[data-theme="dark"] .pedido-recreo__horario {
  background-color: rgba(129, 178, 154, 0.16) !important;
  border-color: rgba(129, 178, 154, 0.28) !important;
}

:host-context([data-theme="dark"]) .pedido-recreo__hora,
:host-context([data-theme="dark"]) .pedido-recreo__separador,
[data-theme="dark"] .pedido-recreo__hora,
[data-theme="dark"] .pedido-recreo__separador {
  color: #81B29A !important;
}
`;
fs.appendFileSync(pedidoPath, pedidoDark, 'utf8');
console.log('6. Updated pedido-recreo-card.component.css');
