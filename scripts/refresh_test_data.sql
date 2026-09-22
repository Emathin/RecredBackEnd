DO $$
DECLARE
    -- IDs de la familia de prueba (Yanina, Rocío y Martín)
    tutor_usuario_id UUID := '06aa9324-45fa-43e2-9be8-b7f143fc53ba'::UUID;
    tutor_id UUID := '572fd792-ba90-4574-aaeb-1e386d31376f'::UUID;
    rocio_id UUID := 'ae5c6f2b-7612-43de-b5df-8bf68ffe079d'::UUID;
    martin_id UUID := '7058aa34-c843-41ca-a8dc-27c496fa7413'::UUID;
    student_ids UUID[] := ARRAY[rocio_id, martin_id];
    
    -- Variables auxiliares
    target_student_id UUID;
    rocio_wallet UUID;
    martin_wallet UUID;
    buffet_id_val UUID;
    franja_id_val UUID;
    prod_id_val UUID;
    
    new_compra_id UUID;
    new_retiro_id UUID;
    new_promo_id UUID;
    pago_id UUID;
    
    target_date TIMESTAMP;
    promo_start TIMESTAMP;
    promo_end TIMESTAMP;
    i INT;
    j INT;
BEGIN
    -- 1. CORRECCIÓN DE DATOS CORRUPTOS
    -- Prevenir el Error 500 ObjectNotFoundException por promociones borradas
    UPDATE compras_detalles SET promocion_id = NULL WHERE promocion_id NOT IN (SELECT id FROM promotions);

    -- 2. AMPLIAR FRANJAS HORARIAS
    -- Prevenir que el cron job cancele los pedidos pendientes de inmediato
    UPDATE franjas_horarias SET hora_fin = '23:59:00', hora_inicio = '23:00:00';

    -- Obtener IDs necesarios
    SELECT id INTO buffet_id_val FROM buffets LIMIT 1;
    SELECT id INTO franja_id_val FROM franjas_horarias LIMIT 1;
    SELECT id INTO prod_id_val FROM productos LIMIT 1;
    SELECT id INTO rocio_wallet FROM billeteras WHERE alumno_id = rocio_id;
    SELECT id INTO martin_wallet FROM billeteras WHERE alumno_id = martin_id;

    -- 3. GENERAR HISTORIAL DE COMPRAS PASADAS CLONANDO DATOS (Últimos 10 días)
    FOREACH target_student_id IN ARRAY student_ids LOOP
        FOR i IN 1..5 LOOP
            target_date := CURRENT_TIMESTAMP - (i || ' days')::interval;
            new_compra_id := gen_random_uuid();
            
            INSERT INTO compras (
                id, tipo, creado_en, estado, fecha_hora, medio_pago, monto_total, alumno_id, buffet_id, padre_id
            ) VALUES (
                new_compra_id, 'PRESENCIAL', target_date, 'ENTREGADO', target_date, 'CREDITOS', 2500.00, target_student_id, buffet_id_val, tutor_id
            );
            
            INSERT INTO compras_detalles (
                id, cantidad, precio_unitario, subtotal, compra_id, producto_id
            ) VALUES (
                gen_random_uuid(), 1, 2500.00, 2500.00, new_compra_id, prod_id_val
            );
        END LOOP;
    END LOOP;

    -- 4. GENERAR PEDIDOS PENDIENTES FUTUROS (De hoy a 7 días)
    FOREACH target_student_id IN ARRAY student_ids LOOP
        FOR i IN 0..7 LOOP
            target_date := CURRENT_TIMESTAMP + (i || ' days')::interval;
            new_compra_id := gen_random_uuid();
            
            INSERT INTO compras (
                id, tipo, creado_en, estado, fecha_hora, medio_pago, monto_total, alumno_id, buffet_id, padre_id
            ) VALUES (
                new_compra_id, 'ANTICIPADA', CURRENT_TIMESTAMP, 'PENDIENTE', CURRENT_TIMESTAMP, 'CREDITOS', 1500.00, target_student_id, buffet_id_val, tutor_id
            );
            
            INSERT INTO compras_detalles (
                id, cantidad, precio_unitario, subtotal, compra_id, producto_id
            ) VALUES (
                gen_random_uuid(), 1, 1500.00, 1500.00, new_compra_id, prod_id_val
            );
            
            new_retiro_id := gen_random_uuid();
            INSERT INTO retiros_programados (
                id, creado_en, estado, fecha_programada, compra_id, franja_id
            ) VALUES (
                new_retiro_id, CURRENT_TIMESTAMP, 'PROGRAMADO', CAST(target_date AS DATE), new_compra_id, franja_id_val
            );
        END LOOP;
    END LOOP;

    -- 5. GENERAR PROMOCIONES DEL KIOSQUERO (Para los próximos 7 días, 2 por día)
    FOR i IN 1..7 LOOP
        target_date := CURRENT_TIMESTAMP + (i || ' days')::interval;
        
        FOR j IN 1..2 LOOP
            promo_start := CAST(CAST(target_date AS DATE) AS TIMESTAMP) + interval '8 hours';
            promo_end := CAST(CAST(target_date AS DATE) AS TIMESTAMP) + interval '20 hours';
            new_promo_id := gen_random_uuid();
            
            INSERT INTO promotions (
                id, created_at, discount_percentage, end_date, image_url, name, start_date, status, updated_at, buffet_id
            ) VALUES (
                new_promo_id, CURRENT_TIMESTAMP, 15.00, promo_end, '', 'Promo Especial ' || j || ' - ' || CAST(target_date AS DATE), promo_start, 'ACTIVE', CURRENT_TIMESTAMP, buffet_id_val
            );
            
            INSERT INTO promotions_products (promotion_id, product_id)
            VALUES (new_promo_id, prod_id_val);
        END LOOP;
    END LOOP;

    -- 6. GENERAR HISTORIAL DE RECARGAS Y TRANSFERENCIAS
    -- Para Rocío
    FOR i IN 1..5 LOOP
        target_date := CURRENT_TIMESTAMP - (i || ' days')::interval;
        
        pago_id := gen_random_uuid();
        INSERT INTO pagos_mercado_pago (
            id, creado_en, aprobado_en, estado, moneda, monto, billetera_id, usuario_id, mercado_pago_payment_id
        ) VALUES (
            pago_id, target_date, target_date, 'APROBADO', 'ARS', 5000.00, rocio_wallet, tutor_usuario_id, 'MP_SIMULADO_' || floor(random()*1000000)::text
        );
        
        INSERT INTO movimientos_billetera (
            id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id, pago_mercado_pago_id
        ) VALUES (
            gen_random_uuid(), target_date, 'Carga mediante MercadoPago', 5000.00, 'CARGA', rocio_wallet, tutor_usuario_id, pago_id
        );

        INSERT INTO movimientos_billetera (
            id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id
        ) VALUES (
            gen_random_uuid(), target_date + interval '1 hour', 'Transferencia recibida de Martín Orieta', 1500.00, 'AJUSTE', rocio_wallet, tutor_usuario_id
        );
    END LOOP;
    
    -- Para Martín
    FOR i IN 1..5 LOOP
        target_date := CURRENT_TIMESTAMP - (i || ' days')::interval;
        
        pago_id := gen_random_uuid();
        INSERT INTO pagos_mercado_pago (
            id, creado_en, aprobado_en, estado, moneda, monto, billetera_id, usuario_id, mercado_pago_payment_id
        ) VALUES (
            pago_id, target_date, target_date, 'APROBADO', 'ARS', 4000.00, martin_wallet, tutor_usuario_id, 'MP_SIMULADO_' || floor(random()*1000000)::text
        );
        
        INSERT INTO movimientos_billetera (
            id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id, pago_mercado_pago_id
        ) VALUES (
            gen_random_uuid(), target_date, 'Carga mediante MercadoPago', 4000.00, 'CARGA', martin_wallet, tutor_usuario_id, pago_id
        );

        INSERT INTO movimientos_billetera (
            id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id
        ) VALUES (
            gen_random_uuid(), target_date + interval '1 hour', 'Transferencia recibida de Rocío Nemeth', 1000.00, 'AJUSTE', martin_wallet, tutor_usuario_id
        );
    END LOOP;
    
END $$;
