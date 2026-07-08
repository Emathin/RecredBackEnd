\set alumno_id '''7058aa34-c843-41ca-a8dc-27c496fa7413'''
\set prod_1 '''c1a00000-0000-4000-a000-000000000003'''
\set prod_2 '''ce88e616-f15b-4045-b071-9f8a63d7caf9'''
\set prod_3 '''237756df-0310-4792-b8f1-1231c6a0d716'''

-- 1. Insertar Sugerencias de IA para Preferencias
INSERT INTO sugerencias_ia (
    id, tipo, titulo, mensaje, alumno_id, producto_id, creado_en, aceptada
)
VALUES (
    gen_random_uuid(), 
    'PREFERENCIA_DETECTADA', 
    'Posible nueva preferencia', 
    'Notamos que tu hijo ha comprado Jugo de Naranja varias veces esta semana. ¿Deseas agregarlo a sus favoritos?',
    :alumno_id, 
    :prod_2,
    now() - interval '1 day',
    null
),
(
    gen_random_uuid(), 
    'PREFERENCIA_DETECTADA', 
    'Patrón de consumo', 
    'Agua con gas es consumida casi todos los días al mediodía.',
    :alumno_id, 
    :prod_3,
    now() - interval '3 days',
    null
);

-- 2. Insertar Productos Favoritos
INSERT INTO productos_favoritos (
    alumno_id, producto_id
)
VALUES 
(:alumno_id, :prod_1)
ON CONFLICT DO NOTHING;

-- 3. Insertar compras
INSERT INTO compras (
    id, alumno_id, buffet_id, estado, monto_total, fecha, metodo_pago
)
VALUES 
(gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'COMPLETADA', 500.00, now() - interval '1 day', 'SALDO_CUENTA'),
(gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'COMPLETADA', 750.00, now() - interval '2 days', 'SALDO_CUENTA'),
(gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'COMPLETADA', 350.00, now() - interval '3 days', 'SALDO_CUENTA'),
(gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'COMPLETADA', 800.00, now() - interval '4 days', 'SALDO_CUENTA'),
(gen_random_uuid(), :alumno_id, (SELECT id FROM buffets LIMIT 1), 'COMPLETADA', 400.00, now() - interval '5 days', 'SALDO_CUENTA');
