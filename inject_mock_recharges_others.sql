-- Rocio
INSERT INTO movimientos_billetera (id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id) VALUES
(gen_random_uuid(), NOW() - INTERVAL '1 day', 'Recarga mediante Mercado Pago', 2000.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '3 days', 'Recarga mediante Mercado Pago', 5000.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '6 days', 'Recarga mediante Mercado Pago', 1000.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '8 days', 'Recarga mediante Mercado Pago', 3500.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '12 days', 'Recarga mediante Mercado Pago', 4500.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '15 days', 'Recarga mediante Mercado Pago', 2500.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '19 days', 'Recarga mediante Mercado Pago', 6000.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '22 days', 'Recarga mediante Mercado Pago', 8000.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '25 days', 'Recarga mediante Mercado Pago', 1500.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5'),
(gen_random_uuid(), NOW() - INTERVAL '28 days', 'Recarga mediante Mercado Pago', 3000.00, 'CARGA', '772e96d5-73f8-434e-b4e6-f01cd26ccb74', '6dfa4aa1-b332-4188-a346-f25ea169e4e5');

-- Adrian
INSERT INTO movimientos_billetera (id, creado_en, descripcion, monto, tipo, billetera_id, usuario_id) VALUES
(gen_random_uuid(), NOW() - INTERVAL '2 days', 'Recarga mediante Mercado Pago', 3000.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '4 days', 'Recarga mediante Mercado Pago', 6000.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '7 days', 'Recarga mediante Mercado Pago', 1500.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '9 days', 'Recarga mediante Mercado Pago', 4000.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '11 days', 'Recarga mediante Mercado Pago', 5500.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '14 days', 'Recarga mediante Mercado Pago', 2000.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '17 days', 'Recarga mediante Mercado Pago', 7500.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '21 days', 'Recarga mediante Mercado Pago', 1200.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '26 days', 'Recarga mediante Mercado Pago', 4500.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df'),
(gen_random_uuid(), NOW() - INTERVAL '30 days', 'Recarga mediante Mercado Pago', 9000.00, 'CARGA', '32fc1676-aa58-4869-8fba-d6746513b4ca', 'be3ce00f-5953-4362-a543-1749243fb6df');
