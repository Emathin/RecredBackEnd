const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredBackEnd\\\\src\\\\main\\\\java\\\\com\\\\recred\\\\backend\\\\infrastructure\\\\mercadopago\\\\MercadoPagoAdapter.java';
let c = fs.readFileSync(file, 'utf8');

const targetCatch = `        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al crear la suscripción de recarga en Mercado Pago", e);
        }`;

const replacementCatch = `        } catch (com.mercadopago.exceptions.MPApiException e) {
            String errorMsg = e.getApiResponse().getContent();
            System.err.println("Preapproval MPApiException content: " + errorMsg);
            throw new RuntimeException("Error al crear la suscripcion en MP: " + errorMsg, e);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear la suscripcion en MP", e);
        }`;

c = c.replace(targetCatch, replacementCatch);
fs.writeFileSync(file, c);
console.log('Added MPApiException block');
