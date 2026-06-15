package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Purchase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithdrawalCodeServiceImplTest {

    private final WithdrawalCodeServiceImpl service = new WithdrawalCodeServiceImpl();

    private static final List<String> PALABRAS_VALIDAS = Arrays.asList(
        // Animales
        "GATO", "PERRO", "LEON", "TIGRE", "OSO", "PATO", "SAPO", "LOBO", "ZORRO", "MONO",
        "VACA", "TORO", "PUMA", "FOCA", "LORO", "CABRA", "CEBRA", "JIRAFA", "RATON", "CONEJO",
        "OVEJA", "CERDO", "GALLO", "PULPO", "BALLENA", "DELFIN", "TIBURON", "TORTUGA", "RANA",
        "AGUILA", "LAMA", "PANDA", "KOALA", "CISNE", "BUHO", "ERIZO",
        // Colores
        "ROJO", "AZUL", "VERDE", "AMARILLO", "ROSA", "GRIS", "MARRON", "NEGRO", "BLANCO",
        "NARANJA", "VIOLETA", "PURPURA", "CELESTE", "DORADO", "PLATEADO", "TURQUESA", "FUCSIA",
        "BEIGE", "LILA", "SALMON", "CREMA", "OLIVA", "CORAL", "ARENA",
        // Nombres
        "MATEO", "LUCAS", "SOFIA", "JUAN", "MARIA", "PEDRO", "ANA", "LUIS", "DIEGO", "PABLO",
        "TOMAS", "ENZO", "LOLA", "EMMA", "HUGO", "LEO", "VALEN", "SANTI", "BAUTI", "JOACO",
        "COTI", "MARTU", "MILI", "JULI", "FRAN", "NACHO", "TOBI", "BRUNO", "CLARA", "DANTE",
        "ELENA", "FELIPE", "GASTON", "IVAN", "LAURA", "MANUEL", "OLIVIA", "ROQUE", "SIMON",
        // Naturaleza / Objetos
        "SOL", "LUNA", "ESTRELLA", "NUBE", "RIO", "MAR", "FLOR", "ARBOL", "CASA", "MESA",
        "SILLA", "LAPIZ", "LIBRO", "TAZA", "PLATO", "MANZANA", "PLATANO", "LIMON", "QUESO",
        "PAN", "LECHE", "AGUA", "MIEL", "DULCE", "TORTA", "PIZZA", "PASTA", "ARROZ", "VIENTO",
        "FUEGO", "TIERRA", "MONTAÑA", "BOSQUE", "PIEDRA", "ISLA", "LAGO", "CIELO"
    );

    @Test
    void shouldGenerateCodeFromPredefinedList() {
        Purchase purchase = new Purchase();
        
        for (int i = 0; i < 100; i++) {
            String code = service.generateCode(purchase);
            assertNotNull(code);
            assertTrue(PALABRAS_VALIDAS.contains(code), "Generated code " + code + " is not in the predefined list");
            assertEquals(code, code.toUpperCase(), "Generated code " + code + " must be uppercase");
        }
    }

    private void assertEquals(String expected, String actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
}
