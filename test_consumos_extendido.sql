\set alumno_id '''7058aa34-c843-41ca-a8dc-27c496fa7413'''

-- Productos Saludables
\set prod_agua '''685daf3f-6b8f-4484-8c0f-4c34c29ca076'''
\set prod_yogur '''14ec9c89-cb63-4871-8307-c4086d3d6539'''
\set prod_wrap '''20713a75-0f53-4a48-a6be-bd89c73a5665'''

-- Productos No Saludables
\set prod_alfajor '''e4d724ab-dd32-4d01-94d7-846d60a31af4'''
\set prod_coca '''a23fe876-e0f7-4c15-a3a7-ad0fe9c5fa80'''
\set prod_papas '''df9d857c-a01a-4d56-8ced-eefa656692e8'''

-- No borramos compras previas por las foreign keys

-- Día 1: Wrap + Agua (Saludable)
WITH nueva_compra AS (
    INSERT INTO compras (id, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago, tipo)
    VALUES (gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 3050.00, now() - interval '1 day', 'CREDITOS', 'PRESENCIAL')
    RETURNING id
)
INSERT INTO compras_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT gen_random_uuid(), id, :prod_wrap::uuid, 1, 2100.00, 2100.00 FROM nueva_compra
UNION ALL
SELECT gen_random_uuid(), id, :prod_agua::uuid, 1, 950.00, 950.00 FROM nueva_compra;

-- Día 2: Alfajor + Coca (No Saludable)
WITH nueva_compra AS (
    INSERT INTO compras (id, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago, tipo)
    VALUES (gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 1700.00, now() - interval '2 days', 'CREDITOS', 'PRESENCIAL')
    RETURNING id
)
INSERT INTO compras_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT gen_random_uuid(), id, :prod_alfajor::uuid, 1, 500.00, 500.00 FROM nueva_compra
UNION ALL
SELECT gen_random_uuid(), id, :prod_coca::uuid, 1, 1200.00, 1200.00 FROM nueva_compra;

-- Día 3: Wrap + Papas + Coca (Mixto pesado)
WITH nueva_compra AS (
    INSERT INTO compras (id, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago, tipo)
    VALUES (gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 3900.00, now() - interval '3 days', 'CREDITOS', 'PRESENCIAL')
    RETURNING id
)
INSERT INTO compras_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT gen_random_uuid(), id, :prod_wrap::uuid, 1, 2100.00, 2100.00 FROM nueva_compra
UNION ALL
SELECT gen_random_uuid(), id, :prod_coca::uuid, 1, 1200.00, 1200.00 FROM nueva_compra
UNION ALL
SELECT gen_random_uuid(), id, :prod_papas::uuid, 1, 600.00, 600.00 FROM nueva_compra;

-- Día 4: Solo agua
WITH nueva_compra AS (
    INSERT INTO compras (id, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago, tipo)
    VALUES (gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 950.00, now() - interval '4 days', 'CREDITOS', 'PRESENCIAL')
    RETURNING id
)
INSERT INTO compras_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT gen_random_uuid(), id, :prod_agua::uuid, 1, 950.00, 950.00 FROM nueva_compra;

-- Día 5: 3 Alfajores (Patron preocupante)
WITH nueva_compra AS (
    INSERT INTO compras (id, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago, tipo)
    VALUES (gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 1500.00, now() - interval '5 days', 'CREDITOS', 'PRESENCIAL')
    RETURNING id
)
INSERT INTO compras_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT gen_random_uuid(), id, :prod_alfajor::uuid, 3, 500.00, 1500.00 FROM nueva_compra;

-- Día 7: Yogur + Alfajor (Mixto)
WITH nueva_compra AS (
    INSERT INTO compras (id, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago, tipo)
    VALUES (gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 1300.00, now() - interval '7 days', 'CREDITOS', 'PRESENCIAL')
    RETURNING id
)
INSERT INTO compras_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT gen_random_uuid(), id, :prod_yogur::uuid, 1, 800.00, 800.00 FROM nueva_compra
UNION ALL
SELECT gen_random_uuid(), id, :prod_alfajor::uuid, 1, 500.00, 500.00 FROM nueva_compra;

-- Día 10: 2 Cocas (Patron excesivo azucar)
WITH nueva_compra AS (
    INSERT INTO compras (id, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago, tipo)
    VALUES (gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 2400.00, now() - interval '10 days', 'CREDITOS', 'PRESENCIAL')
    RETURNING id
)
INSERT INTO compras_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT gen_random_uuid(), id, :prod_coca::uuid, 2, 1200.00, 2400.00 FROM nueva_compra;

-- Día 12: Wrap + Yogur
WITH nueva_compra AS (
    INSERT INTO compras (id, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago, tipo)
    VALUES (gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 2900.00, now() - interval '12 days', 'CREDITOS', 'PRESENCIAL')
    RETURNING id
)
INSERT INTO compras_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT gen_random_uuid(), id, :prod_wrap::uuid, 1, 2100.00, 2100.00 FROM nueva_compra
UNION ALL
SELECT gen_random_uuid(), id, :prod_yogur::uuid, 1, 800.00, 800.00 FROM nueva_compra;
