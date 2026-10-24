const fs = require('fs');
const path = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/tracking-pedidos/tracking-pedidos.page.css';

const darkModeCSS = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .tracking-page,
[data-theme="dark"] .tracking-page {
  background:
    radial-gradient(circle at 18% 12%, rgba(242, 183, 5, 0.08), transparent 28%),
    radial-gradient(circle at 80% 25%, rgba(74, 111, 165, 0.12), transparent 35%),
    linear-gradient(180deg, rgba(30, 41, 59, 0.65) 0%, rgba(15, 23, 42, 0) 320px),
    var(--color-fondo, #0F172A) !important;
}

:host-context([data-theme="dark"]) .tp-back,
[data-theme="dark"] .tp-back {
  background-color: var(--color-superficie, #1E293B) !important;
  border-color: var(--color-borde, rgba(255, 255, 255, 0.08)) !important;
  color: var(--color-pizarra-dark, #8FB4E6) !important;
}

:host-context([data-theme="dark"]) .tp-hero,
:host-context([data-theme="dark"]) .tp-filters,
:host-context([data-theme="dark"]) .tp-orders,
[data-theme="dark"] .tp-hero,
[data-theme="dark"] .tp-filters,
[data-theme="dark"] .tp-orders {
  background: var(--color-superficie, #1E293B) !important;
  border-color: var(--color-borde, rgba(255, 255, 255, 0.08)) !important;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.35) !important;
}

:host-context([data-theme="dark"]) .tp-hero h1,
[data-theme="dark"] .tp-hero h1 {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .tp-hero p:not(.tp-hero__eyebrow),
[data-theme="dark"] .tp-hero p:not(.tp-hero__eyebrow) {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .tp-button--ghost,
[data-theme="dark"] .tp-button--ghost {
  background-color: var(--color-superficie-alt, #334155) !important;
  border-color: var(--color-borde-fuerte, rgba(255, 255, 255, 0.16)) !important;
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .tp-metric,
[data-theme="dark"] .tp-metric {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .tp-metric strong,
[data-theme="dark"] .tp-metric strong {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .tp-metric small,
[data-theme="dark"] .tp-metric small {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .tp-metric--success .tp-metric__icon,
[data-theme="dark"] .tp-metric--success .tp-metric__icon {
  background-color: rgba(129, 178, 154, 0.2) !important;
  color: #81B29A !important;
}

:host-context([data-theme="dark"]) .tp-metric--warning,
[data-theme="dark"] .tp-metric--warning {
  border-color: rgba(242, 183, 5, 0.28) !important;
  background-color: rgba(242, 183, 5, 0.1) !important;
}

:host-context([data-theme="dark"]) .tp-metric--warning .tp-metric__icon,
[data-theme="dark"] .tp-metric--warning .tp-metric__icon {
  background-color: rgba(242, 183, 5, 0.22) !important;
  color: var(--color-dorado) !important;
}

:host-context([data-theme="dark"]) .tp-metric--danger,
[data-theme="dark"] .tp-metric--danger {
  border-color: rgba(231, 111, 81, 0.28) !important;
  background-color: rgba(231, 111, 81, 0.1) !important;
}

:host-context([data-theme="dark"]) .tp-metric--danger .tp-metric__icon,
[data-theme="dark"] .tp-metric--danger .tp-metric__icon {
  background-color: rgba(231, 111, 81, 0.22) !important;
  color: var(--color-melocoton) !important;
}

:host-context([data-theme="dark"]) .tp-hero__filter,
[data-theme="dark"] .tp-hero__filter {
  background-color: rgba(108, 142, 191, 0.15) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  color: #8FB4E6 !important;
}

:host-context([data-theme="dark"]) .tp-field input,
:host-context([data-theme="dark"]) .tp-field select,
[data-theme="dark"] .tp-field input,
[data-theme="dark"] .tp-field select {
  background-color: var(--color-superficie-alt, #334155) !important;
  border-color: var(--color-borde-fuerte, rgba(255, 255, 255, 0.16)) !important;
  color: var(--color-texto-oscuro, #F8FAFC) !important;
}

:host-context([data-theme="dark"]) .tp-field input:focus,
:host-context([data-theme="dark"]) .tp-field select:focus,
[data-theme="dark"] .tp-field input:focus,
[data-theme="dark"] .tp-field select:focus {
  border-color: var(--color-pizarra-dark, #8FB4E6) !important;
  background-color: var(--color-superficie-alt, #334155) !important;
}

:host-context([data-theme="dark"]) .tp-section-title h2,
[data-theme="dark"] .tp-section-title h2 {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .tp-section-title > span,
[data-theme="dark"] .tp-section-title > span {
  background-color: rgba(108, 142, 191, 0.18) !important;
  color: var(--color-pizarra-dark, #8FB4E6) !important;
}

:host-context([data-theme="dark"]) .tp-order,
[data-theme="dark"] .tp-order {
  background-color: var(--color-superficie-alt, #1E293B) !important;
  border-color: var(--color-borde, rgba(255, 255, 255, 0.08)) !important;
}

:host-context([data-theme="dark"]) .tp-order:hover,
:host-context([data-theme="dark"]) .tp-order--active,
[data-theme="dark"] .tp-order:hover,
[data-theme="dark"] .tp-order--active {
  border-color: var(--color-pizarra-dark, #8FB4E6) !important;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.3) !important;
}

:host-context([data-theme="dark"]) .tp-order__code,
[data-theme="dark"] .tp-order__code {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .tp-order__student,
[data-theme="dark"] .tp-order__student {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .tp-order__items,
[data-theme="dark"] .tp-order__items {
  border-color: rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .tp-order__item,
[data-theme="dark"] .tp-order__item {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .tp-order__total strong,
[data-theme="dark"] .tp-order__total strong {
  color: var(--color-dorado) !important;
}
`;

fs.appendFileSync(path, darkModeCSS, 'utf8');
console.log('Appended dark mode CSS to tracking-pedidos.page.css successfully!');
