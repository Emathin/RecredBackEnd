DO $$
DECLARE
    rec RECORD;
    i INT;
    random_amount DECIMAL;
    random_days INT;
    random_date TIMESTAMP;
BEGIN
    FOR rec IN SELECT id FROM alumnos LOOP
        FOR i IN 1..20 LOOP
            random_amount := floor(random() * (10000 - 1000 + 1) + 1000);
            random_days := floor(random() * 90); -- up to 90 days ago
            random_date := NOW() - (random_days || ' days')::INTERVAL;
            
            INSERT INTO micro_creditos (id, alumno_id, amount, status, installments, creado_en)
            VALUES (gen_random_uuid(), rec.id, random_amount, 'PAID', 1, random_date);
        END LOOP;
    END LOOP;
END $$;
