const fs = require('fs');
const path = require('path');

const FRONTEND_DIR = 'C:\\Users\\emman\\OneDrive\\Desktop\\Final TPI\\RecredFrontEnd';
const ALUMNO_CARD_TS = path.join(FRONTEND_DIR, 'src', 'app', 'features', 'home-tutor', 'components', 'alumno-card', 'alumno-card.component.ts');
const ALUMNO_CARD_HTML = path.join(FRONTEND_DIR, 'src', 'app', 'features', 'home-tutor', 'components', 'alumno-card', 'alumno-card.component.html');

function modTs() {
    if (!fs.existsSync(ALUMNO_CARD_TS)) {
        console.log('No existe ' + ALUMNO_CARD_TS);
        return;
    }
    let content = fs.readFileSync(ALUMNO_CARD_TS, 'utf8');
    if (!content.includes('AlertController')) {
        content = content.replace(/from '@ionic\/angular';/, ', AlertController } from \'@ionic/angular\';');
        if (!content.includes('AlertController')) {
            content = "import { AlertController } from '@ionic/angular';\n" + content;
        }
    }
    if (!content.includes('programarRecarga()')) {
        if (content.includes('constructor(') && !content.includes('private alertController')) {
            content = content.replace('constructor(', 'constructor(\n    private alertController: AlertController,');
        }
        const methods = `
  async programarRecarga() {
    const alert = await this.alertController.create({
      header: 'Programar Recarga Mensual',
      message: 'Ingresa el monto a recargar cada mes.',
      inputs: [{ name: 'monto', type: 'number', placeholder: 'Ej: 5000', min: 1 }],
      buttons: [ { text: 'Cancelar', role: 'cancel' }, { text: 'Continuar', handler: (data) => { if (data.monto && data.monto > 0) { this.iniciarRecargaProgramada(data.monto); } } } ]
    });
    await alert.present();
  }

  private iniciarRecargaProgramada(monto: number) {
    const token = localStorage.getItem('token');
    fetch('http://localhost:8080/api/v1/payments/schedule-topup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
        body: JSON.stringify({ studentId: this.alumno.id, amount: monto, frequency: 1, frequencyType: 'months' })
    }).then(res => res.json()).then(data => { if(data.paymentUrl) { window.location.href = data.paymentUrl; } }).catch(err => console.error(err));
  }
`;
        const pos = content.lastIndexOf('}');
        content = content.slice(0, pos) + methods + content.slice(pos);
        fs.writeFileSync(ALUMNO_CARD_TS, content);
        console.log('TS Modificado');
    } else {
        console.log('TS ya tenia programarRecarga()');
    }
}

function modHtml() {
    if (!fs.existsSync(ALUMNO_CARD_HTML)) {
        console.log('No existe ' + ALUMNO_CARD_HTML);
        return;
    }
    let content = fs.readFileSync(ALUMNO_CARD_HTML, 'utf8');
    const t1 = '<span class="alumno-card__btn-label">Acreditar</span>\r\n    </a>';
    const t2 = '<span class="alumno-card__btn-label">Acreditar</span>\n    </a>';
    if (!content.includes('programarRecarga()')) {
        const target = content.includes(t1) ? t1 : t2;
        if (content.includes(target)) {
            content = content.replace(target, target + '\n\n    <a class="alumno-card__btn alumno-card__btn--menta" (click)="programarRecarga()" tabindex="0">\n      <span class="alumno-card__btn-icono" aria-hidden="true">\n        <i class="fa-solid fa-calendar-plus"></i>\n      </span>\n      <span class="alumno-card__btn-label">Programar</span>\n    </a>');
            fs.writeFileSync(ALUMNO_CARD_HTML, content);
            console.log('HTML Modificado');
        } else {
            console.log('Target no encontrado en HTML');
        }
    } else {
        console.log('HTML ya tenia programarRecarga()');
    }
}

modTs();
modHtml();
