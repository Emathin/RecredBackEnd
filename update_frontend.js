const fs = require('fs');
const path = require('path');

const FRONTEND_DIR = 'C:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend';
const ALUMNO_CARD_HTML = path.join(FRONTEND_DIR, 'src', 'app', 'features', 'home-tutor', 'components', 'alumno-card', 'alumno-card.component.html');
const ALUMNO_CARD_TS = path.join(FRONTEND_DIR, 'src', 'app', 'features', 'home-tutor', 'components', 'alumno-card', 'alumno-card.component.ts');
const PAYMENT_SERVICE = path.join(FRONTEND_DIR, 'src', 'app', 'data-access', 'services', 'wallet', 'wallet.service.ts'); // Let's guess the name, or we can search

function modifyHtml() {
  if (fs.existsSync(ALUMNO_CARD_HTML)) {
    let content = fs.readFileSync(ALUMNO_CARD_HTML, 'utf8');
    if (!content.includes('programarRecarga()')) {
        const insertPosition = content.lastIndexOf('</ion-card>');
        if (insertPosition !== -1) {
            const newContent = content.slice(0, insertPosition) + 
            `\n    <!-- Boton Programar Recarga -->
    <ion-button expand="block" fill="outline" color="secondary" (click)="programarRecarga()" class="ion-margin-horizontal ion-margin-bottom">
      <ion-icon slot="start" name="calendar-outline"></ion-icon>
      Programar Recarga
    </ion-button>\n  ` + content.slice(insertPosition);
            fs.writeFileSync(ALUMNO_CARD_HTML, newContent);
            console.log('HTML Modificado exitosamente.');
        } else {
            console.log('No encontre donde insertar en HTML.');
        }
    } else {
        console.log('El boton ya existia en HTML.');
    }
  } else {
    console.log('No se encontro alumno-card.component.html en: ' + ALUMNO_CARD_HTML);
  }
}

function modifyTs() {
  if (fs.existsSync(ALUMNO_CARD_TS)) {
    let content = fs.readFileSync(ALUMNO_CARD_TS, 'utf8');
    
    // Si no está importado AlertController, lo agregamos
    if (!content.includes('AlertController')) {
        content = content.replace(/from '@ionic\/angular';/, ', AlertController } from \'@ionic/angular\';');
        if (!content.includes('AlertController')) {
            content = "import { AlertController } from '@ionic/angular';\n" + content;
        }
    }
    
    if (!content.includes('programarRecarga()')) {
        // Find constructor to inject AlertController if not there
        if (content.includes('constructor(')) {
            if (!content.includes('private alertController')) {
                content = content.replace('constructor(', 'constructor(\n    private alertController: AlertController,');
            }
        }
        
        // Add methods
        const methods = `
  async programarRecarga() {
    const alert = await this.alertController.create({
      header: 'Programar Recarga Mensual',
      message: 'Ingresa el monto a recargar cada mes.',
      inputs: [
        {
          name: 'monto',
          type: 'number',
          placeholder: 'Ej: 5000',
          min: 1
        }
      ],
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Continuar',
          handler: (data) => {
            if (data.monto && data.monto > 0) {
              this.iniciarRecargaProgramada(data.monto);
            }
          }
        }
      ]
    });
    await alert.present();
  }

  private iniciarRecargaProgramada(monto: number) {
    // LLamada HTTP manual con fetch para evitar dependencias cruzadas
    const token = localStorage.getItem('token'); // Asumiendo que el token está en localStorage
    fetch('http://localhost:8080/api/v1/payments/schedule-topup', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify({
            studentId: this.alumno.id,
            amount: monto,
            frequency: 1,
            frequencyType: 'months'
        })
    }).then(res => res.json())
      .then(data => {
          if(data.paymentUrl) {
              window.location.href = data.paymentUrl;
          }
      })
      .catch(err => console.error(err));
  }
`;
        // Insert before last closing brace
        const insertPosition = content.lastIndexOf('}');
        content = content.slice(0, insertPosition) + methods + '\n' + content.slice(insertPosition);
        fs.writeFileSync(ALUMNO_CARD_TS, content);
        console.log('TS Modificado exitosamente.');
    } else {
        console.log('El TS ya tenia programarRecarga.');
    }
  } else {
    console.log('No se encontro alumno-card.component.ts en: ' + ALUMNO_CARD_TS);
  }
}

modifyHtml();
modifyTs();
