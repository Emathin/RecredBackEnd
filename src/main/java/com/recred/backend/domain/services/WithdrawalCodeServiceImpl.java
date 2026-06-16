package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Purchase;
import java.util.Random;

public class WithdrawalCodeServiceImpl implements WithdrawalCodeService {

    private static final Random RANDOM = new Random();

    private static final String[] PALABRAS = {
        "GATO", "PERRO", "LEON", "TIGRE", "OSO", "PATO", "SAPO", "LOBO", "ZORRO", "MONO",
        "VACA", "TORO", "PUMA", "FOCA", "LORO", "CABRA", "CEBRA", "JIRAFA", "RATON", "CONEJO",
        "OVEJA", "CERDO", "GALLO", "PULPO", "BALLENA", "DELFIN", "TIBURON", "TORTUGA", "RANA",
        "AGUILA", "LAMA", "PANDA", "KOALA", "CISNE", "BUHO", "ERIZO",
        "ROJO", "AZUL", "VERDE", "AMARILLO", "ROSA", "GRIS", "MARRON", "NEGRO", "BLANCO",
        "NARANJA", "VIOLETA", "PURPURA", "CELESTE", "DORADO", "PLATEADO", "TURQUESA", "FUCSIA",
        "BEIGE", "LILA", "SALMON", "CREMA", "OLIVA", "CORAL", "ARENA",
        "MATEO", "LUCAS", "SOFIA", "JUAN", "MARIA", "PEDRO", "ANA", "LUIS", "DIEGO", "PABLO",
        "TOMAS", "ENZO", "LOLA", "EMMA", "HUGO", "LEO", "VALEN", "SANTI", "BAUTI", "JOACO",
        "COTI", "MARTU", "MILI", "JULI", "FRAN", "NACHO", "TOBI", "BRUNO", "CLARA", "DANTE",
        "ELENA", "FELIPE", "GASTON", "IVAN", "LAURA", "MANUEL", "OLIVIA", "ROQUE", "SIMON",
        "SOL", "LUNA", "ESTRELLA", "NUBE", "RIO", "MAR", "FLOR", "ARBOL", "CASA", "MESA",
        "SILLA", "LAPIZ", "LIBRO", "TAZA", "PLATO", "MANZANA", "PLATANO", "LIMON", "QUESO",
        "PAN", "LECHE", "AGUA", "MIEL", "DULCE", "TORTA", "PIZZA", "PASTA", "ARROZ", "VIENTO",
        "FUEGO", "TIERRA", "MONTAÑA", "BOSQUE", "PIEDRA", "ISLA", "LAGO", "CIELO"
    };

    @Override
    public String generateCode(Purchase purchase) {
        int index = RANDOM.nextInt(PALABRAS.length);
        return PALABRAS[index];
    }

    public WithdrawalCodeServiceImpl() {
    }
}

