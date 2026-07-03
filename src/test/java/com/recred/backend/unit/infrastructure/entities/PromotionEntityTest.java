package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.infrastructure.entities.PromotionEntity;
import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromotionEntityTest {

    @Test
    void imageUrlShouldMapToTextColumn() throws NoSuchFieldException {
        Column column = PromotionEntity.class
                .getDeclaredField("imageUrl")
                .getAnnotation(Column.class);

        assertEquals("image_url", column.name());
        assertEquals("TEXT", column.columnDefinition());
    }
}
