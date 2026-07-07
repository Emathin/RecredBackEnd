UPDATE sugerencias_ia
SET 
  titulo = REPLACE(titulo, 'Patr??n', 'Patrón'),
  mensaje = REPLACE(mensaje, 'd??as al mediod??a', 'días al mediodía')
WHERE titulo LIKE '%Patr??n%';

UPDATE sugerencias_ia
SET 
  mensaje = REPLACE(mensaje, '??Deseas', '¿Deseas')
WHERE mensaje LIKE '%??Deseas%';
