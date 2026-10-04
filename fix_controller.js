const fs = require('fs');

const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\infrastructure\\\\web\\\\TopUpController.java';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(
    /@RequestBody com\.recred\.backend\.application\.ScheduleWalletTopUpRequest request/,
    `@RequestBody(required = false) java.util.Map<String, Object> payload`
);

c = c.replace(
    /com\.recred\.backend\.application\.ScheduleWalletTopUpResponse response =[\s\S]*?scheduleWalletTopUp\.execute\(cognitoSub, request\);/,
    `System.out.println("Payload received: " + payload);
        if (payload == null) {
            throw new IllegalArgumentException("Payload is null! Body was empty or missing Content-Type.");
        }
        
        java.util.UUID studentId = java.util.UUID.fromString((String) payload.get("studentId"));
        java.math.BigDecimal amount = new java.math.BigDecimal(payload.get("amount").toString());
        Integer frequency = (Integer) payload.get("frequency");
        String frequencyType = (String) payload.get("frequencyType");
        
        com.recred.backend.application.ScheduleWalletTopUpRequest request = 
                new com.recred.backend.application.ScheduleWalletTopUpRequest(studentId, amount, frequency, frequencyType);

        com.recred.backend.application.ScheduleWalletTopUpResponse response = 
                scheduleWalletTopUp.execute(cognitoSub, request);`
);

fs.writeFileSync(file, c);
console.log('Fixed TopUpController');
