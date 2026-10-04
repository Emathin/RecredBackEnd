const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\infrastructure\\\\web\\\\TopUpController.java';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(
    /Integer frequency = \(Integer\) payload\.get\("frequency"\);/,
    'Integer frequency = Integer.valueOf(payload.get("frequency").toString());'
);

fs.writeFileSync(file, c);
console.log('Fixed Double cast issue');
