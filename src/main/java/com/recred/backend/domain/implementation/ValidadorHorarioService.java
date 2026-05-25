package com.recred.backend.domain.implementation;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

public class ValidadorHorarioService {

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Argentina/Buenos_Aires");

    public void validarCompra(Alumno alumno, Set<Producto> productos, LocalDateTime momentoCompra) {

        LocalDateTime momentoLocal = ZonedDateTime.of(momentoCompra, ZoneId.systemDefault())
                .withZoneSameInstant(ZONA_LOCAL)
                .toLocalDateTime();

        for (Producto producto : productos) {
            boolean estaBloqueado = alumno.getRestriccionesHorarias().stream()
                    .anyMatch(restriccion -> restriccion.bloquea(producto, momentoLocal));

            if (estaBloqueado) {
                lanzarExcepcion(producto);
            }
        }
    }

    private void lanzarExcepcion(Producto producto) {
        String mensaje = "Las compras del producto '" + producto.getNombre() +
                "' están bloqueadas en este horario por configuración del tutor.";
        throw new RestriccionHorariaException(mensaje);
    }
}