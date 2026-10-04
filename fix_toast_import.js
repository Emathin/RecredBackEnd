const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredFrontEnd\\\\src\\\\app\\\\features\\\\programar-recarga\\\\programar-recarga.component.ts';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(/import \{ ToastService \} from '\.\.\/\.\.\/core\/services\/toast\.service';/, "import { ToastService } from '../../shared/services/toast.service';");

fs.writeFileSync(file, c);
console.log('Fixed ToastService import');
