SET session_replication_role = 'replica';
\i '/tmp/bd_seed_completo.sql'
SET session_replication_role = 'origin';
