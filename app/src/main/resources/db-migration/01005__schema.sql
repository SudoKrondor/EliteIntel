UPDATE commodities
SET commodity_es = 'Tratamiento agronómico',
    commodity_fr = 'Traitement agronomique'
WHERE LOWER(commodity) = LOWER('Agronomic Treatment');

UPDATE commodities
SET commodity_es = 'Equipo experimental clasificado',
    commodity_fr = 'Équipement expérimental classifié'
WHERE LOWER(commodity) = LOWER('Classified Experimental Equipment');

UPDATE commodities
SET commodity_es = 'Chateau de Aegaeon'
WHERE LOWER(commodity) = LOWER('Chateau De Aegaeon');