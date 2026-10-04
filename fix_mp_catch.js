const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\infrastructure\\\\mercadopago\\\\MercadoPagoAdapter.java';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(/        } catch \(Exception e\) \{/,
`        } catch (com.mercadopago.exceptions.MPApiException e) {
            String errorMsg = e.getApiResponse().getContent();
            System.err.println("Preapproval MPApiException content: " + errorMsg);
            throw new RuntimeException("Error MP: " + errorMsg, e);
        } catch (Exception e) {`);

fs.writeFileSync(file, c);
console.log("Updated catch block");
