const fs = require('fs');
const path = 'c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/app/features/home-kiosquero/home-kiosquero.page.css';

const darkModeCSS = `

/* ==========================================================================
   Dark Theme Specific Overrides (:host-context([data-theme="dark"]))
   ========================================================================== */

:host-context([data-theme="dark"]) .home-kiosquero,
[data-theme="dark"] .home-kiosquero {
  background:
    radial-gradient(circle at 18% 12%, rgba(242, 183, 5, 0.08), transparent 28%),
    radial-gradient(circle at 80% 25%, rgba(74, 111, 165, 0.12), transparent 35%),
    linear-gradient(180deg, rgba(30, 41, 59, 0.65) 0%, rgba(15, 23, 42, 0) 300px),
    var(--color-fondo, #0F172A) !important;
}

/* Profile card header */
:host-context([data-theme="dark"]) .pv__profile-card,
[data-theme="dark"] .pv__profile-card {
  background: var(--color-superficie, #1E293B) !important;
  border-color: var(--color-borde, rgba(255, 255, 255, 0.08)) !important;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.35) !important;
}

:host-context([data-theme="dark"]) .pv__avatar,
[data-theme="dark"] .pv__avatar {
  background: #0f172a !important;
  border-color: var(--color-dorado) !important;
  box-shadow: 0 10px 24px rgba(242, 183, 5, 0.2) !important;
}

:host-context([data-theme="dark"]) .pv__avatar span,
[data-theme="dark"] .pv__avatar span {
  color: var(--color-dorado) !important;
}

:host-context([data-theme="dark"]) .pv__greeting,
[data-theme="dark"] .pv__greeting {
  color: var(--color-texto-claro, #94A3B8) !important;
}

:host-context([data-theme="dark"]) .pv__profile-identity h1,
[data-theme="dark"] .pv__profile-identity h1 {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__profile-identity p:last-child,
[data-theme="dark"] .pv__profile-identity p:last-child {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__profile-report,
[data-theme="dark"] .pv__profile-report {
  background-color: rgba(108, 142, 191, 0.18) !important;
  color: #8FB4E6 !important;
}

:host-context([data-theme="dark"]) .pv__profile-report:hover,
[data-theme="dark"] .pv__profile-report:hover {
  background-color: rgba(108, 142, 191, 0.28) !important;
}

:host-context([data-theme="dark"]) .pv__date-field input,
[data-theme="dark"] .pv__date-field input {
  background-color: var(--color-superficie-alt, #334155) !important;
  border-color: var(--color-borde-fuerte, rgba(255, 255, 255, 0.16)) !important;
  color: var(--color-texto-oscuro, #F8FAFC) !important;
}

:host-context([data-theme="dark"]) .pv__notice,
[data-theme="dark"] .pv__notice {
  background-color: var(--color-superficie, #1E293B) !important;
  border-color: var(--color-borde, rgba(255, 255, 255, 0.08)) !important;
  color: var(--color-texto-oscuro, #F8FAFC) !important;
}

:host-context([data-theme="dark"]) .pv__notice--error,
[data-theme="dark"] .pv__notice--error {
  background-color: rgba(231, 111, 81, 0.15) !important;
  border-color: rgba(231, 111, 81, 0.35) !important;
  color: #fca5a5 !important;
}

/* Summary Panel: Resumen del Día */
:host-context([data-theme="dark"]) .pv__summary-panel,
[data-theme="dark"] .pv__summary-panel {
  background:
    radial-gradient(circle at 76% 5%, rgba(129, 178, 154, 0.18), transparent 32%),
    linear-gradient(135deg, #1e3a8a 0%, #0f172a 100%) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 20px 44px rgba(0, 0, 0, 0.4) !important;
}

:host-context([data-theme="dark"]) .pv__summary-grid,
[data-theme="dark"] .pv__summary-grid {
  background: rgba(0, 0, 0, 0.28) !important;
  border: 1px solid rgba(255, 255, 255, 0.06) !important;
}

:host-context([data-theme="dark"]) .pv__summary-card+.pv__summary-card,
[data-theme="dark"] .pv__summary-card+.pv__summary-card {
  border-left-color: rgba(255, 255, 255, 0.12) !important;
}

:host-context([data-theme="dark"]) .pv__summary-card:first-child,
[data-theme="dark"] .pv__summary-card:first-child {
  background:
    radial-gradient(circle at 16% 22%, rgba(242, 183, 5, 0.18), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02)) !important;
}

:host-context([data-theme="dark"]) .pv__summary-card small,
[data-theme="dark"] .pv__summary-card small {
  color: rgba(255, 255, 255, 0.72) !important;
}

:host-context([data-theme="dark"]) .pv__summary-card strong,
[data-theme="dark"] .pv__summary-card strong {
  color: #ffffff !important;
}

:host-context([data-theme="dark"]) .pv__summary-icon,
[data-theme="dark"] .pv__summary-icon {
  background-color: rgba(255, 255, 255, 0.14) !important;
  color: #ffffff !important;
}

:host-context([data-theme="dark"]) .pv__summary-card:first-child .pv__summary-icon,
[data-theme="dark"] .pv__summary-card:first-child .pv__summary-icon {
  background-color: rgba(242, 183, 5, 0.25) !important;
  color: var(--color-dorado) !important;
}

/* Operation Panel: Pedidos de hoy */
:host-context([data-theme="dark"]) .pv__panel,
[data-theme="dark"] .pv__panel {
  background: var(--color-superficie, #1E293B) !important;
  border-color: var(--color-borde, rgba(255, 255, 255, 0.08)) !important;
  box-shadow: var(--shadow-card) !important;
}

:host-context([data-theme="dark"]) .pv__section-title h2,
[data-theme="dark"] .pv__section-title h2 {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__section-title>span,
[data-theme="dark"] .pv__section-title>span {
  background-color: rgba(108, 142, 191, 0.18) !important;
  color: var(--color-pizarra-dark, #8FB4E6) !important;
}

:host-context([data-theme="dark"]) .pv__operation-grid,
[data-theme="dark"] .pv__operation-grid {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .pv__operation-card+.pv__operation-card,
[data-theme="dark"] .pv__operation-card+.pv__operation-card {
  border-left-color: rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .pv__operation-card:hover,
[data-theme="dark"] .pv__operation-card:hover {
  background-color: rgba(255, 255, 255, 0.04) !important;
}

:host-context([data-theme="dark"]) .pv__operation-card strong,
[data-theme="dark"] .pv__operation-card strong {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__operation-label,
[data-theme="dark"] .pv__operation-label {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__operation-icon,
[data-theme="dark"] .pv__operation-icon {
  background-color: rgba(129, 178, 154, 0.2) !important;
  color: #81B29A !important;
}

:host-context([data-theme="dark"]) .pv__operation-card--warning:hover,
[data-theme="dark"] .pv__operation-card--warning:hover {
  background-color: rgba(242, 183, 5, 0.08) !important;
}

:host-context([data-theme="dark"]) .pv__operation-card--warning .pv__operation-icon,
[data-theme="dark"] .pv__operation-card--warning .pv__operation-icon {
  background-color: rgba(242, 183, 5, 0.22) !important;
  color: var(--color-dorado) !important;
}

:host-context([data-theme="dark"]) .pv__operation-card--danger:hover,
[data-theme="dark"] .pv__operation-card--danger:hover {
  background-color: rgba(231, 111, 81, 0.08) !important;
}

:host-context([data-theme="dark"]) .pv__operation-card--danger .pv__operation-icon,
[data-theme="dark"] .pv__operation-card--danger .pv__operation-icon {
  background-color: rgba(231, 111, 81, 0.22) !important;
  color: var(--color-melocoton) !important;
}

:host-context([data-theme="dark"]) .pv__operation-arrow,
[data-theme="dark"] .pv__operation-arrow {
  color: #94A3B8 !important;
}

/* Button: Entregar Compra Anticipada */
:host-context([data-theme="dark"]) .pv__verify-btn,
[data-theme="dark"] .pv__verify-btn {
  background: linear-gradient(135deg, rgba(129, 178, 154, 0.12), rgba(30, 41, 59, 0.85)) !important;
  border-color: rgba(129, 178, 154, 0.28) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25) !important;
}

:host-context([data-theme="dark"]) .pv__verify-btn:hover,
[data-theme="dark"] .pv__verify-btn:hover {
  background: linear-gradient(135deg, rgba(129, 178, 154, 0.22), rgba(30, 41, 59, 0.95)) !important;
  border-color: rgba(129, 178, 154, 0.45) !important;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.35) !important;
}

:host-context([data-theme="dark"]) .pv__verify-btn-icon,
[data-theme="dark"] .pv__verify-btn-icon {
  background-color: rgba(129, 178, 154, 0.22) !important;
  color: #81B29A !important;
}

:host-context([data-theme="dark"]) .pv__verify-btn-body strong,
[data-theme="dark"] .pv__verify-btn-body strong {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__verify-btn-body small,
[data-theme="dark"] .pv__verify-btn-body small {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__verify-btn-arrow,
[data-theme="dark"] .pv__verify-btn-arrow {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: #81B29A !important;
}

:host-context([data-theme="dark"]) .pv__verify-btn:hover .pv__verify-btn-arrow,
[data-theme="dark"] .pv__verify-btn:hover .pv__verify-btn-arrow {
  background-color: #81B29A !important;
  color: #0f172a !important;
}

/* Featured Side Actions: Venta, Cargar producto, Diagnóstico comercial, Cierre diario */
:host-context([data-theme="dark"]) .pv__featured-card,
[data-theme="dark"] .pv__featured-card {
  background: var(--color-superficie, #1E293B) !important;
  border-color: var(--color-borde, rgba(255, 255, 255, 0.08)) !important;
  box-shadow: var(--shadow-card) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card:hover,
[data-theme="dark"] .pv__featured-card:hover {
  border-color: var(--color-borde-fuerte, rgba(255, 255, 255, 0.18)) !important;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.35) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card--dorado,
[data-theme="dark"] .pv__featured-card--dorado {
  background: linear-gradient(135deg, rgba(242, 183, 5, 0.14), var(--color-superficie, #1E293B)) !important;
  border-color: rgba(242, 183, 5, 0.28) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card--menta,
[data-theme="dark"] .pv__featured-card--menta {
  background: linear-gradient(135deg, rgba(129, 178, 154, 0.14), var(--color-superficie, #1E293B)) !important;
  border-color: rgba(129, 178, 154, 0.28) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card--pizarra,
[data-theme="dark"] .pv__featured-card--pizarra {
  background: linear-gradient(135deg, rgba(108, 142, 191, 0.16), var(--color-superficie, #1E293B)) !important;
  border-color: rgba(108, 142, 191, 0.28) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card--melocoton,
[data-theme="dark"] .pv__featured-card--melocoton {
  background: linear-gradient(135deg, rgba(231, 111, 81, 0.14), var(--color-superficie, #1E293B)) !important;
  border-color: rgba(231, 111, 81, 0.28) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card strong,
[data-theme="dark"] .pv__featured-card strong {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__featured-card small,
[data-theme="dark"] .pv__featured-card small {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__featured-icon,
[data-theme="dark"] .pv__featured-icon {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card--dorado .pv__featured-icon,
[data-theme="dark"] .pv__featured-card--dorado .pv__featured-icon {
  color: var(--color-dorado) !important;
  background-color: rgba(242, 183, 5, 0.18) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card--menta .pv__featured-icon,
[data-theme="dark"] .pv__featured-card--menta .pv__featured-icon {
  color: #81B29A !important;
  background-color: rgba(129, 178, 154, 0.18) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card--pizarra .pv__featured-icon,
[data-theme="dark"] .pv__featured-card--pizarra .pv__featured-icon {
  color: #8FB4E6 !important;
  background-color: rgba(108, 142, 191, 0.18) !important;
}

:host-context([data-theme="dark"]) .pv__featured-card--melocoton .pv__featured-icon,
[data-theme="dark"] .pv__featured-card--melocoton .pv__featured-icon {
  color: var(--color-melocoton) !important;
  background-color: rgba(231, 111, 81, 0.18) !important;
}

:host-context([data-theme="dark"]) .pv__featured-arrow,
[data-theme="dark"] .pv__featured-arrow {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__featured-card:hover .pv__featured-arrow,
[data-theme="dark"] .pv__featured-card:hover .pv__featured-arrow {
  color: #F8FAFC !important;
  background-color: rgba(255, 255, 255, 0.18) !important;
}

/* Secondary Quick Actions */
:host-context([data-theme="dark"]) .pv__quick-action,
[data-theme="dark"] .pv__quick-action {
  background-color: var(--color-superficie, #1E293B) !important;
  border-color: var(--color-borde, rgba(255, 255, 255, 0.08)) !important;
  box-shadow: var(--shadow-card) !important;
}

:host-context([data-theme="dark"]) .pv__quick-action:hover,
[data-theme="dark"] .pv__quick-action:hover {
  border-color: var(--color-borde-fuerte, rgba(255, 255, 255, 0.16)) !important;
  box-shadow: var(--shadow-elevated) !important;
}

:host-context([data-theme="dark"]) .pv__quick-action strong,
[data-theme="dark"] .pv__quick-action strong {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__quick-action small,
[data-theme="dark"] .pv__quick-action small {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__quick-icon,
[data-theme="dark"] .pv__quick-icon {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: var(--color-pizarra-dark, #8FB4E6) !important;
}

:host-context([data-theme="dark"]) .pv__quick-action--menta .pv__quick-icon,
[data-theme="dark"] .pv__quick-action--menta .pv__quick-icon {
  background-color: rgba(129, 178, 154, 0.18) !important;
  color: #81B29A !important;
}

:host-context([data-theme="dark"]) .pv__quick-action--dorado .pv__quick-icon,
[data-theme="dark"] .pv__quick-action--dorado .pv__quick-icon {
  background-color: rgba(242, 183, 5, 0.18) !important;
  color: var(--color-dorado) !important;
}

:host-context([data-theme="dark"]) .pv__quick-action--melocoton .pv__quick-icon,
[data-theme="dark"] .pv__quick-action--melocoton .pv__quick-icon {
  background-color: rgba(231, 111, 81, 0.18) !important;
  color: var(--color-melocoton) !important;
}

:host-context([data-theme="dark"]) .pv__quick-action--violeta .pv__quick-icon,
[data-theme="dark"] .pv__quick-action--violeta .pv__quick-icon {
  background-color: rgba(124, 58, 237, 0.22) !important;
  color: #a78bfa !important;
}

:host-context([data-theme="dark"]) .pv__quick-arrow,
[data-theme="dark"] .pv__quick-arrow {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__quick-action--menta .pv__quick-arrow,
[data-theme="dark"] .pv__quick-action--menta .pv__quick-arrow {
  background-color: #81B29A !important;
  color: #0f172a !important;
}

:host-context([data-theme="dark"]) .pv__quick-action--dorado .pv__quick-arrow,
[data-theme="dark"] .pv__quick-action--dorado .pv__quick-arrow {
  background-color: var(--color-dorado) !important;
  color: #0f172a !important;
}

:host-context([data-theme="dark"]) .pv__quick-action--melocoton .pv__quick-arrow,
[data-theme="dark"] .pv__quick-action--melocoton .pv__quick-arrow {
  background-color: var(--color-melocoton) !important;
  color: #ffffff !important;
}

:host-context([data-theme="dark"]) .pv__quick-action--violeta .pv__quick-arrow,
[data-theme="dark"] .pv__quick-action--violeta .pv__quick-arrow {
  background-color: #8b5cf6 !important;
  color: #ffffff !important;
}

/* Stock Panel: Stock crítico */
:host-context([data-theme="dark"]) .pv__stock-panel,
[data-theme="dark"] .pv__stock-panel {
  background:
    linear-gradient(180deg, rgba(231, 111, 81, 0.08), var(--color-superficie, #1E293B)),
    var(--color-superficie, #1E293B) !important;
  border-color: rgba(231, 111, 81, 0.28) !important;
  box-shadow: var(--shadow-card) !important;
}

:host-context([data-theme="dark"]) .pv__stock-panel .pv__section-title h2,
[data-theme="dark"] .pv__stock-panel .pv__section-title h2 {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__stock-title-icon,
[data-theme="dark"] .pv__stock-title-icon {
  background-color: rgba(231, 111, 81, 0.2) !important;
  color: #f87171 !important;
}

:host-context([data-theme="dark"]) .pv__stock-link,
[data-theme="dark"] .pv__stock-link {
  background-color: var(--color-pizarra) !important;
  color: #ffffff !important;
}

:host-context([data-theme="dark"]) .pv__stock-link:hover,
[data-theme="dark"] .pv__stock-link:hover {
  background-color: var(--color-pizarra-dark, #8FB4E6) !important;
}

:host-context([data-theme="dark"]) .pv__stock-metric,
[data-theme="dark"] .pv__stock-metric {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .pv__stock-metric strong,
[data-theme="dark"] .pv__stock-metric strong {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__stock-metric small,
[data-theme="dark"] .pv__stock-metric small {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__stock-metric:first-child,
[data-theme="dark"] .pv__stock-metric:first-child {
  border-color: rgba(231, 111, 81, 0.3) !important;
  background-color: rgba(231, 111, 81, 0.12) !important;
}

:host-context([data-theme="dark"]) .pv__stock-metric:first-child>span,
[data-theme="dark"] .pv__stock-metric:first-child>span {
  background-color: rgba(231, 111, 81, 0.22) !important;
  color: #f87171 !important;
}

:host-context([data-theme="dark"]) .pv__stock-metric--warning,
[data-theme="dark"] .pv__stock-metric--warning {
  border-color: rgba(242, 183, 5, 0.3) !important;
  background-color: rgba(242, 183, 5, 0.12) !important;
}

:host-context([data-theme="dark"]) .pv__stock-metric--warning>span,
[data-theme="dark"] .pv__stock-metric--warning>span {
  background-color: rgba(242, 183, 5, 0.22) !important;
  color: var(--color-dorado) !important;
}

:host-context([data-theme="dark"]) .pv__stock-group,
[data-theme="dark"] .pv__stock-group {
  background-color: rgba(15, 23, 42, 0.5) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

:host-context([data-theme="dark"]) .pv__stock-group-header h3,
[data-theme="dark"] .pv__stock-group-header h3 {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__stock-group-header small,
[data-theme="dark"] .pv__stock-group-header small {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__product-item,
[data-theme="dark"] .pv__product-item {
  background-color: var(--color-superficie-alt, #334155) !important;
}

:host-context([data-theme="dark"]) .pv__product-item strong,
[data-theme="dark"] .pv__product-item strong {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__product-item small,
[data-theme="dark"] .pv__product-item small {
  color: #94A3B8 !important;
}

:host-context([data-theme="dark"]) .pv__ok-state,
[data-theme="dark"] .pv__ok-state {
  background-color: rgba(129, 178, 154, 0.12) !important;
  border-color: rgba(129, 178, 154, 0.25) !important;
}

:host-context([data-theme="dark"]) .pv__ok-state strong,
[data-theme="dark"] .pv__ok-state strong {
  color: #F8FAFC !important;
}

:host-context([data-theme="dark"]) .pv__ok-state p,
[data-theme="dark"] .pv__ok-state p {
  color: #94A3B8 !important;
}

/* Primary Action */
:host-context([data-theme="dark"]) .pv__primary-action,
[data-theme="dark"] .pv__primary-action {
  background:
    radial-gradient(circle at 76% 18%, rgba(242, 183, 5, 0.2), transparent 22%),
    linear-gradient(135deg, #1e3a8a, #0f172a) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.35) !important;
}

:host-context([data-theme="dark"]) .pv__primary-illustration,
[data-theme="dark"] .pv__primary-illustration {
  background-color: rgba(30, 41, 59, 0.85) !important;
  color: var(--color-pizarra-dark, #8FB4E6) !important;
}
`;

fs.appendFileSync(path, darkModeCSS, 'utf8');
console.log('Appended dark mode CSS to home-kiosquero.page.css successfully!');
