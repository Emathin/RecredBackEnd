const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredFrontEnd\\\\src\\\\app\\\\features\\\\home-tutor\\\\components\\\\alumno-card\\\\alumno-card.component.ts';
let c = fs.readFileSync(file, 'utf8');

c = c.replace("import { AlertController } from '@ionic/angular';\\n", "");
c = c.replace("private alertController: AlertController,", "");

const oldMethod = `  async programarRecarga() {
    const alert = await this.alertController.create({
      header: 'Programar Recarga Mensual',
      message: 'Ingresa el monto a recargar cada mes.',
      inputs: [{ name: 'monto', type: 'number', placeholder: 'Ej: 5000', min: 1 }],
      buttons: [ { text: 'Cancelar', role: 'cancel' }, { text: 'Continuar', handler: (data: any) => { if (data.monto && data.monto > 0) { this.iniciarRecargaProgramada(data.monto); } } } ]
    });
    await alert.present();
  }`;

const newMethod = `  programarRecarga() {
    const montoStr = window.prompt('Ingresa el monto a recargar cada mes (Ej: 5000):');
    if (montoStr) {
      const monto = Number(montoStr);
      if (monto > 0) {
        this.iniciarRecargaProgramada(monto);
      }
    }
  }`;

c = c.replace(/async programarRecarga\(\) \{[\s\S]*?await alert\.present\(\);\s*\}/, newMethod);

fs.writeFileSync(file, c);
console.log('Fixed TS file');
