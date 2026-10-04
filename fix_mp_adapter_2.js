const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\infrastructure\\\\mercadopago\\\\MercadoPagoAdapter.java';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(
    /public String createScheduledTopUpPreapproval\([\s\S]*?UUID studentId, BigDecimal amount, Integer frequency, String frequencyType\) \{/,
    `public String createScheduledTopUpPreapproval(
            UUID studentId, BigDecimal amount, Integer frequency, String frequencyType, String payerEmail) {`
);

c = c.replace(
    /\.backUrl\(properties\.getSuccessUrl\(\)\)/,
    `.backUrl(properties.getSuccessUrl())
                    .payerEmail(payerEmail)`
);

c = c.replace(/\.status\("authorized"\)/, `/* .status("authorized") REMOVED to avoid API errors */`);

fs.writeFileSync(file, c);
console.log('Fixed MercadoPagoAdapter');
