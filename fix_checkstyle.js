const fs = require('fs');

const file1 = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\domain\\\\services\\\\ExternalPaymentService.java';
let c1 = fs.readFileSync(file1, 'utf8');
c1 = c1.replace(
    /UUID studentId, BigDecimal amount, Integer frequency, String frequencyType, String payerEmail\);/,
    `UUID studentId, BigDecimal amount, Integer frequency, String frequencyType,\n            String payerEmail);`
);
fs.writeFileSync(file1, c1);

const file2 = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\infrastructure\\\\mercadopago\\\\MercadoPagoAdapter.java';
let c2 = fs.readFileSync(file2, 'utf8');
c2 = c2.replace(
    /UUID studentId, BigDecimal amount, Integer frequency, String frequencyType, String payerEmail\) \{/,
    `UUID studentId, BigDecimal amount, Integer frequency,\n            String frequencyType, String payerEmail) {`
);
fs.writeFileSync(file2, c2);

console.log('Fixed line lengths');
