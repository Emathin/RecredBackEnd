const fs = require('fs');

const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\infrastructure\\\\mercadopago\\\\MercadoPagoAdapter.java';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(
    /\.backUrl\(properties\.getSuccessUrl\(\)\.replace\("http:\/\/localhost", "https:\/\/127\.0\.0\.1"\)\)/,
    '.backUrl("https://recred.netlify.app/pago-exitoso")'
);

c = c.replace(
    /\.backUrl\(properties\.getSuccessUrl\(\)\)/,
    '.backUrl("https://recred.netlify.app/pago-exitoso")'
);

fs.writeFileSync(file, c);
console.log('Fixed backUrl properly');
