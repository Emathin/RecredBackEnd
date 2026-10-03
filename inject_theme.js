const fs = require('fs');
const tsPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\aceptar-invitacion-colaborador\\aceptar-invitacion-colaborador.ts';
let tsContent = fs.readFileSync(tsPath, 'utf8');

tsContent = tsContent.replace("import { ToastService } from '../../shared/services/toast.service';", "import { ToastService } from '../../shared/services/toast.service';\nimport { ThemeService } from '../../core/services/theme.service';");
tsContent = tsContent.replace("private readonly toastService = inject(ToastService);", "private readonly toastService = inject(ToastService);\n  private readonly themeService = inject(ThemeService);");

fs.writeFileSync(tsPath, tsContent, 'utf8');
console.log("ThemeService injected");
