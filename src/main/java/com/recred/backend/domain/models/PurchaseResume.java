package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseResume {

    private UUID idAlumno;
    private UUID idProducto;
    private String nombreProducto;
    private BigDecimal precio;
    private LocalDateTime date;

    public PurchaseResume(
            UUID idAlumno, UUID idProducto,
            String nombreProducto, BigDecimal precio, LocalDateTime date) {
        this.idAlumno = idAlumno;
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.precio = precio;
        this.date = date;
    }

    @Override
    public String toString() {
        return "Resumen de compra{idProducto='" + idProducto + "'," +
                " nombreProducto='" + nombreProducto + "', precio="
                + precio + "' fecha='" + date + "'}";
    }
}
