const fs = require('fs');

// 1. explicacion-puntos.page.css
const expPath = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/explicacion-puntos/explicacion-puntos.page.css';
const expDark = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .card,
[data-theme="dark"] .card {
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: var(--shadow-card) !important;
}

:host-context([data-theme="dark"]) .card p,
[data-theme="dark"] .card p {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .btn-volver,
[data-theme="dark"] .btn-volver {
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .principiante,
[data-theme="dark"] .principiante {
  background: linear-gradient(135deg, rgba(0, 105, 92, 0.35), #1E293B) !important;
  color: #81B29A !important;
  border: 1px solid rgba(129, 178, 154, 0.3) !important;
}

:host-context([data-theme="dark"]) .crack,
[data-theme="dark"] .crack {
  background: linear-gradient(135deg, rgba(230, 81, 0, 0.35), #1E293B) !important;
  color: var(--color-dorado) !important;
  border: 1px solid rgba(242, 183, 5, 0.3) !important;
}

:host-context([data-theme="dark"]) .goat,
[data-theme="dark"] .goat {
  background: linear-gradient(135deg, rgba(74, 20, 140, 0.35), #1E293B) !important;
  color: #c084fc !important;
  border: 1px solid rgba(168, 85, 247, 0.3) !important;
}

:host-context([data-theme="dark"]) .puntos-rango,
[data-theme="dark"] .puntos-rango {
  background: rgba(0, 0, 0, 0.35) !important;
  color: #F8FAFC !important;
}
`;
fs.appendFileSync(expPath, expDark, 'utf8');
console.log('Updated explicacion-puntos.page.css');

// 2. favoritos.page.css
const favPath = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/favoritos/favoritos.page.css';
const favDark = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .favoritos,
[data-theme="dark"] .favoritos {
  background:
    radial-gradient(circle at 18% 12%, rgba(242, 183, 5, 0.08), transparent 28%),
    radial-gradient(circle at 80% 25%, rgba(74, 111, 165, 0.12), transparent 35%),
    linear-gradient(180deg, rgba(30, 41, 59, 0.65) 0%, rgba(15, 23, 42, 0) 240px),
    var(--color-fondo, #0F172A) !important;
}

:host-context([data-theme="dark"]) .favoritos__titulo,
[data-theme="dark"] .favoritos__titulo {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .favoritos-fav-card__quitar-btn,
[data-theme="dark"] .favoritos-fav-card__quitar-btn {
  background: rgba(30, 41, 59, 0.88) !important;
  border-color: rgba(255, 255, 255, 0.15) !important;
}
`;
fs.appendFileSync(favPath, favDark, 'utf8');
console.log('Updated favoritos.page.css');
