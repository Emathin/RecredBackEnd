DO $$
DECLARE
    -- IDs de la familia de prueba
    tutor_usuario_id UUID := '1d33ad05-9e32-4d05-a829-12d661d08200'::UUID; -- Yanina Domine (Usuario)
    tutor_id UUID := '572fd792-ba90-4574-aaeb-1e386d31376f'::UUID;        -- Yanina Domine (Tutor)
    rocio_id UUID := 'ae5c6f2b-7612-43de-b5df-8bf68ffe079d'::UUID;        -- Rocío Nemeth
    martin_id UUID := '7058aa34-c843-41ca-a8dc-27c496fa7413'::UUID;       -- Martín Orieta
    student_ids UUID[] := ARRAY[rocio_id, martin_id];
    
    rocio_wallet UUID := '32fc1676-aa58-4869-8fba-d6746513b4ca'::UUID;
    martin_wallet UUID := '91adac09-b51e-49f7-83a3-6877eb83908b'::UUID;
    
    -- Buffet y franjas (Instituto Parroquial San Justo / El Buffet de Alexis)
    buffet_id_val UUID := '2c4153b3-d0f9-489c-93c0-8b3ad7b89758'::UUID;
    franja_recreo1 UUID := 'aeb34a00-cee8-446f-bc4e-2352a72a3eba'::UUID;
    franja_recreo2 UUID := 'd4571525-855a-4c99-b6a0-8b104c3206d9'::UUID;
    franja_salida  UUID := '290bb69a-fa1d-4b83-af2e-59582f52aea7'::UUID;
    
    -- Productos variados de El Buffet de Alexis
    p_sandwich  UUID := '7f2fe3a4-9321-4206-a1b6-e70265ee587f'::UUID; -- $2000.00
    p_wrap      UUID := '20713a75-0f53-4a48-a6be-bd89c73a5665'::UUID; -- $2100.00
    p_ensalada  UUID := '47f0d618-b11e-47d1-89d7-6c87885697c6'::UUID; -- $2200.00
    p_jugo_nar  UUID := 'ce88e616-f15b-4045-b071-9f8a63d7caf9'::UUID; -- $950.00
    p_limonada  UUID := 'fa249229-97fa-44dd-8b1a-e4f7088e6046'::UUID; -- $1000.00
    p_agua      UUID := '685daf3f-6b8f-4484-8c0f-4c34c29ca076'::UUID; -- $950.00
    p_jorgito   UUID := 'e4d724ab-dd32-4d01-94d7-846d60a31af4'::UUID; -- $500.00
    p_capitan   UUID := 'edd9bc21-73b1-4f3f-a2fb-14ab9d0f79f3'::UUID; -- $800.00
    p_barrita   UUID := 'ace2c91d-4023-47d2-b9da-6978b44a3a27'::UUID; -- $350.00
    p_avena     UUID := 'f41739ee-7ffa-43ac-93bb-2b48f99aa5e1'::UUID; -- $800.00
    p_yogur_dur UUID := '7d477d58-48fb-44f7-903b-cb21f86bb2af'::UUID; -- $850.00
    p_yogur_ser UUID := 'a128e99b-35ac-4a65-8ded-1a2f8d48fb8e'::UUID; -- $1200.00
    p_chocola   UUID := '3ca3362f-5cce-44e9-938e-d256f4559331'::UUID; -- $600.00
    p_medialuna UUID := 'a6bb2485-777c-491f-98c6-55cee64b792a'::UUID; -- $500.00
    
    prod_ids UUID[] := ARRAY[p_sandwich, p_wrap, p_ensalada, p_jugo_nar, p_limonada, p_agua, p_jorgito, p_capitan, p_barrita, p_avena, p_yogur_dur, p_yogur_ser, p_chocola, p_medialuna];
    prod_precios NUMERIC[] := ARRAY[2000.00, 2100.00, 2200.00, 950.00, 1000.00, 950.00, 500.00, 800.00, 350.00, 800.00, 850.00, 1200.00, 600.00, 500.00];
    
    target_student_id UUID;
    target_wallet_id  UUID;
    new_compra_id UUID;
    new_promo_id  UUID;
    pago_id       UUID;
    
    target_date TIMESTAMP;
    promo_start TIMESTAMP;
    promo_end   TIMESTAMP;
    
    item_p1 UUID;
    item_p2 UUID;
    precio_p1 NUMERIC;
    precio_p2 NUMERIC;
    total_compra NUMERIC;
    
    i INT;
    rand_idx1 INT;
    rand_idx2 INT;
BEGIN
    -- 1. CORRECCIÓN DE DATOS CORRUPTOS Y PREVENCIÓN DE ERRORES 500
    UPDATE compras_detalles SET promocion_id = NULL WHERE promocion_id NOT IN (SELECT id FROM promotions);

    -- 2. AMPLIAR FRANJAS HORARIAS
    -- Permite que los pedidos no expiren automáticamente y pasen la validación de tiempo en demo
    UPDATE franjas_horarias 
    SET hora_fin = '23:59:59', hora_inicio = '23:00:00', minutos_corte = 0;

    -- 3. ASEGURAR STOCK SUFICIENTE EN EL BUFFET DE ALEXIS
    UPDATE inventarios_buffet 
    SET stock_actual = 250 
    WHERE buffet_id = buffet_id_val;

    -- 4. PUNTOS DE RECOMPENSA SALUDABLE PARA LOS ALUMNOS
    UPDATE alumnos SET puntos_recompensa = 240 WHERE id = rocio_id;
    UPDATE alumnos SET puntos_recompensa = 160 WHERE id = martin_id;

    -- 5. PRESUPUESTOS PARA EL MES EN CURSO (OCTUBRE 2026)
    -- Rocío: $50,000 MENSUAL
    UPDATE presupuestos_alumnos 
    SET monto_limite = 50000.00, periodo = 'MENSUAL', activo = true 
    WHERE alumno_id = rocio_id;

    -- Martín: $45,000 MENSUAL
    UPDATE presupuestos_alumnos 
    SET monto_limite = 45000.00, periodo = 'MENSUAL', activo = true 
    WHERE alumno_id = martin_id;

    -- 6. GENERAR HISTORIAL DE COMPRAS PASADAS EN OCTUBRE (Últimos 21 días hasta hoy)
    FOREACH target_student_id IN ARRAY student_ids LOOP
        target_wallet_id := CASE WHEN target_student_id = rocio_id THEN rocio_wallet ELSE martin_wallet END;

        FOR i IN 1..21 LOOP
            -- Saltear algunos días para simular fines de semana
            IF (i % 7 != 0) THEN
                target_date := CURRENT_TIMESTAMP - (i || ' days')::interval - ((i * 17 % 6) || ' hours')::interval;
                
                -- Seleccionar dos productos al azar de diferentes categorías
                rand_idx1 := 1 + ((i * 3) % array_length(prod_ids, 1));
                rand_idx2 := 1 + ((i * 5 + 2) % array_length(prod_ids, 1));
                item_p1 := prod_ids[rand_idx1];
                precio_p1 := prod_precios[rand_idx1];
                item_p2 := prod_ids[rand_idx2];
                precio_p2 := prod_precios[rand_idx2];
                total_compra := precio_p1 + precio_p2;

                new_compra_id := gen_random_uuid();
                
                -- Compra entregada
                INSERT INTO compras (
                    id, tipo, creado_en, estado, fecha_hora, medio_pago, monto_total, alumno_id, buffet_id, padre_id, codigo_verificacion
                ) VALUES (
                    new_compra_id, 
                    CASE WHEN i % 2 = 0 THEN 'ANTICIPADA' ELSE 'PRESENCIAL' END, 
                    target_date, 
                    'ENTREGADO', 
                    target_date, 
                    'CREDITOS', 
                    total_compra, 
                    target_student_id, 
                    buffet_id_val, 
                    tutor_id,
                    'RET-' || (1000 + (i * 37 % 8999))::text
                );
                
                -- Detalles de la compra
                INSERT INTO compras_detalles (
                    id, cantidad, precio_unitario, subtotal, compra_id, producto_id
                ) VALUES (
                    gen_random_uuid(), 1, precio_p1, precio_p1, new_compra_id, item_p1
                );

                INSERT INTO compras_detalles (
                    id, cantidad, precio_unitario, subtotal, compra_id, producto_id
                ) VALUES (
                    gen_random_uuid(), 1, precio_p2, precio_p2, new_compra_id, item_p2
                );

                -- Si era anticipada, retiro registrado como RETIRADO
                IF (i % 2 = 0) THEN
                    INSERT INTO retiros_programados (
                        id, creado_en, estado, fecha_programada, compra_id, franja_id
                    ) VALUES (
                        gen_random_uuid(), target_date, 'RETIRADO', CAST(target_date AS DATE), new_compra_id, franja_recreo1
                    );
                END IF;

                -- Movimiento en la billetera
                INSERT INTO movimientos_billetera (
                    id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id, compra_id
                ) VALUES (
                    gen_random_uuid(), target_date, 'Compra en buffet escolar', total_compra, 'COMPRA', target_wallet_id, tutor_usuario_id, new_compra_id
                );
            END IF;
        END LOOP;
    END LOOP;

    -- 7. GENERAR PEDIDOS ACTIVOS Y PROGRAMADOS (Para HOY, MAÑANA, y el SÁBADO DE EXPOSICIÓN)
    -- Pedido 1: Para HOY - LISTO para retirar (con código RET-2055)
    new_compra_id := gen_random_uuid();
    INSERT INTO compras (
        id, tipo, creado_en, estado, fecha_hora, medio_pago, monto_total, alumno_id, buffet_id, padre_id, codigo_verificacion
    ) VALUES (
        new_compra_id, 'ANTICIPADA', CURRENT_TIMESTAMP - interval '2 hours', 'LISTO', CURRENT_TIMESTAMP, 'CREDITOS', 3050.00, rocio_id, buffet_id_val, tutor_id, 'RET-2055'
    );
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 2100.00, 2100.00, new_compra_id, p_wrap);
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 950.00, 950.00, new_compra_id, p_agua);
    INSERT INTO retiros_programados (id, creado_en, estado, fecha_programada, compra_id, franja_id)
    VALUES (gen_random_uuid(), CURRENT_TIMESTAMP, 'LISTO', CURRENT_DATE, new_compra_id, franja_recreo1);

    -- Pedido 2: Para HOY - EN PREPARACIÓN (con código RET-1042)
    new_compra_id := gen_random_uuid();
    INSERT INTO compras (
        id, tipo, creado_en, estado, fecha_hora, medio_pago, monto_total, alumno_id, buffet_id, padre_id, codigo_verificacion
    ) VALUES (
        new_compra_id, 'ANTICIPADA', CURRENT_TIMESTAMP - interval '1 hour', 'EN_PREPARACION', CURRENT_TIMESTAMP, 'CREDITOS', 2950.00, martin_id, buffet_id_val, tutor_id, 'RET-1042'
    );
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 2000.00, 2000.00, new_compra_id, p_sandwich);
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 950.00, 950.00, new_compra_id, p_jugo_nar);
    INSERT INTO retiros_programados (id, creado_en, estado, fecha_programada, compra_id, franja_id)
    VALUES (gen_random_uuid(), CURRENT_TIMESTAMP, 'EN_PREPARACION', CURRENT_DATE, new_compra_id, franja_recreo2);

    -- Pedido 3: Para MAÑANA (Viernes 23) - PROGRAMADO
    new_compra_id := gen_random_uuid();
    INSERT INTO compras (
        id, tipo, creado_en, estado, fecha_hora, medio_pago, monto_total, alumno_id, buffet_id, padre_id, codigo_verificacion
    ) VALUES (
        new_compra_id, 'ANTICIPADA', CURRENT_TIMESTAMP, 'PENDIENTE', CURRENT_TIMESTAMP, 'CREDITOS', 2700.00, rocio_id, buffet_id_val, tutor_id, 'RET-3108'
    );
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 2200.00, 2200.00, new_compra_id, p_ensalada);
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 500.00, 500.00, new_compra_id, p_jorgito);
    INSERT INTO retiros_programados (id, creado_en, estado, fecha_programada, compra_id, franja_id)
    VALUES (gen_random_uuid(), CURRENT_TIMESTAMP, 'PROGRAMADO', CURRENT_DATE + 1, new_compra_id, franja_recreo1);

    -- Pedidos 4 y 5: Para el SÁBADO (24 oct - DÍA DE LA EXPOSICIÓN)
    -- Pedido para Rocío el sábado (con código EXP-701)
    new_compra_id := gen_random_uuid();
    INSERT INTO compras (
        id, tipo, creado_en, estado, fecha_hora, medio_pago, monto_total, alumno_id, buffet_id, padre_id, codigo_verificacion
    ) VALUES (
        new_compra_id, 'ANTICIPADA', CURRENT_TIMESTAMP, 'PENDIENTE', CURRENT_TIMESTAMP, 'CREDITOS', 3150.00, rocio_id, buffet_id_val, tutor_id, 'EXP-701'
    );
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 2200.00, 2200.00, new_compra_id, p_ensalada);
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 950.00, 950.00, new_compra_id, p_agua);
    INSERT INTO retiros_programados (id, creado_en, estado, fecha_programada, compra_id, franja_id)
    VALUES (gen_random_uuid(), CURRENT_TIMESTAMP, 'PROGRAMADO', DATE '2026-10-24', new_compra_id, franja_recreo1);

    -- Pedido para Martín el sábado (con código EXP-702)
    new_compra_id := gen_random_uuid();
    INSERT INTO compras (
        id, tipo, creado_en, estado, fecha_hora, medio_pago, monto_total, alumno_id, buffet_id, padre_id, codigo_verificacion
    ) VALUES (
        new_compra_id, 'ANTICIPADA', CURRENT_TIMESTAMP, 'PENDIENTE', CURRENT_TIMESTAMP, 'CREDITOS', 3000.00, martin_id, buffet_id_val, tutor_id, 'EXP-702'
    );
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 2000.00, 2000.00, new_compra_id, p_sandwich);
    INSERT INTO compras_detalles (id, cantidad, precio_unitario, subtotal, compra_id, producto_id)
    VALUES (gen_random_uuid(), 1, 1000.00, 1000.00, new_compra_id, p_limonada);
    INSERT INTO retiros_programados (id, creado_en, estado, fecha_programada, compra_id, franja_id)
    VALUES (gen_random_uuid(), CURRENT_TIMESTAMP, 'PROGRAMADO', DATE '2026-10-24', new_compra_id, franja_recreo2);

    -- 8. GENERAR PROMOCIONES VIGENTES (Desde 20 de octubre hasta 31 de octubre)
    promo_start := CURRENT_TIMESTAMP - interval '3 days';
    promo_end   := CURRENT_TIMESTAMP + interval '10 days';

    -- Promo 1: Recreo Saludable
    new_promo_id := gen_random_uuid();
    INSERT INTO promotions (
        id, created_at, discount_percentage, end_date, image_url, name, start_date, status, updated_at, buffet_id
    ) VALUES (
        new_promo_id, CURRENT_TIMESTAMP, 15.00, promo_end, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500', 'Combo Recreo Saludable -15%', promo_start, 'ACTIVE', CURRENT_TIMESTAMP, buffet_id_val
    );
    INSERT INTO promotions_products (promotion_id, product_id) VALUES (new_promo_id, p_wrap);
    INSERT INTO promotions_products (promotion_id, product_id) VALUES (new_promo_id, p_jugo_nar);

    -- Promo 2: Merienda Express
    new_promo_id := gen_random_uuid();
    INSERT INTO promotions (
        id, created_at, discount_percentage, end_date, image_url, name, start_date, status, updated_at, buffet_id
    ) VALUES (
        new_promo_id, CURRENT_TIMESTAMP, 20.00, promo_end, 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500', 'Promo Merienda Express -20%', promo_start, 'ACTIVE', CURRENT_TIMESTAMP, buffet_id_val
    );
    INSERT INTO promotions_products (promotion_id, product_id) VALUES (new_promo_id, p_chocola);
    INSERT INTO promotions_products (promotion_id, product_id) VALUES (new_promo_id, p_jorgito);

    -- Promo 3: Frutas y Yogures
    new_promo_id := gen_random_uuid();
    INSERT INTO promotions (
        id, created_at, discount_percentage, end_date, image_url, name, start_date, status, updated_at, buffet_id
    ) VALUES (
        new_promo_id, CURRENT_TIMESTAMP, 25.00, promo_end, 'https://images.unsplash.com/photo-1488477181946-6428a0291777?w=500', 'Descuento Lácteos y Yogures -25%', promo_start, 'ACTIVE', CURRENT_TIMESTAMP, buffet_id_val
    );
    INSERT INTO promotions_products (promotion_id, product_id) VALUES (new_promo_id, p_yogur_dur);
    INSERT INTO promotions_products (promotion_id, product_id) VALUES (new_promo_id, p_yogur_ser);

    -- 9. GENERAR HISTORIAL DE RECARGAS MERCADO PAGO Y TRANSFERENCIAS EN OCTUBRE
    -- Rocío
    FOR i IN 1..3 LOOP
        target_date := CURRENT_TIMESTAMP - ((i * 5) || ' days')::interval;
        pago_id := gen_random_uuid();
        INSERT INTO pagos_mercado_pago (
            id, creado_en, aprobado_en, estado, moneda, monto, billetera_id, usuario_id, mercado_pago_payment_id
        ) VALUES (
            pago_id, target_date, target_date, 'APROBADO', 'ARS', 10000.00, rocio_wallet, tutor_usuario_id, 'MP_OCT_' || gen_random_uuid()::text
        );
        INSERT INTO movimientos_billetera (
            id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id, pago_mercado_pago_id
        ) VALUES (
            gen_random_uuid(), target_date, 'Carga mediante MercadoPago', 10000.00, 'CARGA', rocio_wallet, tutor_usuario_id, pago_id
        );
    END LOOP;

    INSERT INTO movimientos_billetera (
        id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id
    ) VALUES (
        gen_random_uuid(), CURRENT_TIMESTAMP - interval '3 days', 'Transferencia recibida de Martín Orieta', 1500.00, 'AJUSTE', rocio_wallet, tutor_usuario_id
    );

    -- Martín
    FOR i IN 1..3 LOOP
        target_date := CURRENT_TIMESTAMP - ((i * 5 + 1) || ' days')::interval;
        pago_id := gen_random_uuid();
        INSERT INTO pagos_mercado_pago (
            id, creado_en, aprobado_en, estado, moneda, monto, billetera_id, usuario_id, mercado_pago_payment_id
        ) VALUES (
            pago_id, target_date, target_date, 'APROBADO', 'ARS', 8000.00, martin_wallet, tutor_usuario_id, 'MP_OCT_' || gen_random_uuid()::text
        );
        INSERT INTO movimientos_billetera (
            id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id, pago_mercado_pago_id
        ) VALUES (
            gen_random_uuid(), target_date, 'Carga mediante MercadoPago', 8000.00, 'CARGA', martin_wallet, tutor_usuario_id, pago_id
        );
    END LOOP;

    INSERT INTO movimientos_billetera (
        id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id
    ) VALUES (
        gen_random_uuid(), CURRENT_TIMESTAMP - interval '2 days', 'Transferencia recibida de Rocío Nemeth', 1000.00, 'AJUSTE', martin_wallet, tutor_usuario_id
    );

    -- 10. AJUSTAR SALDOS FINALES DE LAS BILLETERAS
    UPDATE billeteras SET saldo_creditos = 28500.00, credito_habilitado = true, limite_credito = 10000.00, saldo_deuda_interno = 0.00 WHERE id = rocio_wallet;
    UPDATE billeteras SET saldo_creditos = 19400.00, credito_habilitado = true, limite_credito = 10000.00, saldo_deuda_interno = 0.00 WHERE id = martin_wallet;

END $$;
