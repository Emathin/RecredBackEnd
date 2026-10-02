const fs = require('fs');
const p = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\data-access\\services\\colaborador.service.ts';
let text = fs.readFileSync(p, 'utf8');

const newMethod = "\n  aceptarInvitacion(request: any): Promise<void> {\n    return firstValueFrom(this.http.post<void>(this.baseUrl + '/aceptar', request));\n  }\n";

text = text.replace('eliminarColaborador', newMethod.trim() + '\n\n  eliminarColaborador');
fs.writeFileSync(p, text, 'utf8');
