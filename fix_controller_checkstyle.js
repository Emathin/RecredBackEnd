const fs = require('fs');

const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\infrastructure\\\\web\\\\TopUpController.java';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(
    /throw new IllegalArgumentException\("Payload is null! Body was empty or missing Content-Type\."\);/,
    'throw new IllegalArgumentException(\n                    "Payload is null! Body was empty.");'
);

c = c.replace(
    /new com\.recred\.backend\.application\.ScheduleWalletTopUpRequest\(studentId, amount, frequency, frequencyType\);/,
    'new com.recred.backend.application.ScheduleWalletTopUpRequest(\n                        studentId, amount, frequency, frequencyType);'
);

fs.writeFileSync(file, c);
console.log('Fixed TopUpController line length');
