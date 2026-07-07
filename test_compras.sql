\set alumno_id '''7058aa34-c843-41ca-a8dc-27c496fa7413'''

-- 3. Insertar compras
INSERT INTO compras (
    id, tipo, alumno_id, buffet_id, estado, monto_total, fecha_hora, medio_pago
)
VALUES 
(gen_random_uuid(), 'PRESENCIAL', :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 500.00, now() - interval '1 day', 'CREDITOS'),
(gen_random_uuid(), 'PRESENCIAL', :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 750.00, now() - interval '2 days', 'CREDITOS'),
(gen_random_uuid(), 'PRESENCIAL', :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 350.00, now() - interval '3 days', 'CREDITOS'),
(gen_random_uuid(), 'PRESENCIAL', :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 800.00, now() - interval '4 days', 'CREDITOS'),
(gen_random_uuid(), 'PRESENCIAL', :alumno_id, (SELECT id FROM buffets LIMIT 1), 'ENTREGADO', 400.00, now() - interval '5 days', 'CREDITOS');
