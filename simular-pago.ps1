param (
    [179059527352]$PaymentId
)

if (-not $PaymentId) {
    $PaymentId = Read-Host "Por favor, ingresa el payment_id (lo encuentras en la URL cuando Mercado Pago termina el pago)"
}

$body = @{
    type = "payment"
    data = @{
        id = $PaymentId
    }
} | ConvertTo-Json

$webhookUrl = "http://localhost:8080/api/v1/payments/webhook"

Write-Host "Simulando notificacion de Mercado Pago para el pago: $PaymentId..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri $webhookUrl -Method POST -Body $body -ContentType "application/json"
    Write-Host "¡Simulacion exitosa! El backend recibio la notificacion y debio actualizar el saldo." -ForegroundColor Green
} catch {
    Write-Host "Error al simular la notificacion: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "Presiona cualquier tecla para salir..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
