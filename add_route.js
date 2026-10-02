const fs = require('fs');
const p = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\app.routes.ts';
let text = fs.readFileSync(p, 'utf8');

const route = `
  {
    path: 'colaborador/aceptar-invitacion/:token',
    loadComponent: () =>
      import('./features/aceptar-invitacion-colaborador/aceptar-invitacion-colaborador').then(
        (m) => m.AceptarInvitacionColaborador,
      ),
  },`;

if (!text.includes('colaborador/aceptar-invitacion')) {
    text = text.replace("path: 'invitaciones/tutor',", route.trim() + "\n  {\n    path: 'invitaciones/tutor',");
    fs.writeFileSync(p, text, 'utf8');
    console.log("Route added");
}
