-- ---------------------------------------------------------------
-- V2 — catalogo inicial de cervejas
--
-- As mais comuns de bar, para o app ja nascer com o que buscar.
-- O resto a galera cadastra pelo app (POST /api/cervejas).
--
-- chave_busca = nome|cervejaria normalizados (minusculo, sem acento),
-- no mesmo formato que o BeerService gera. Reexecutar e inofensivo:
-- o ON CONFLICT ignora o que ja existe.
-- ---------------------------------------------------------------

INSERT INTO beers (nome, estilo, cervejaria, chave_busca, criado_em) VALUES
('Brahma Chopp',          'Pilsen',          'Ambev',            'brahma chopp|ambev',             NOW()),
('Skol',                  'Pilsen',          'Ambev',            'skol|ambev',                     NOW()),
('Antarctica Original',   'Pilsen',          'Ambev',            'antarctica original|ambev',      NOW()),
('Antarctica Subzero',    'Pilsen',          'Ambev',            'antarctica subzero|ambev',       NOW()),
('Bohemia',               'Pilsen',          'Ambev',            'bohemia|ambev',                  NOW()),
('Budweiser',             'American Lager',  'Ambev',            'budweiser|ambev',                NOW()),
('Stella Artois',         'Premium Lager',   'Ambev',            'stella artois|ambev',            NOW()),
('Corona Extra',          'Lager',           'Ambev',            'corona extra|ambev',             NOW()),
('Spaten',                'Munich Helles',   'Ambev',            'spaten|ambev',                   NOW()),
('Michelob Ultra',        'Light Lager',     'Ambev',            'michelob ultra|ambev',           NOW()),
('Patagonia Amber Lager', 'Amber Lager',     'Patagonia',        'patagonia amber lager|patagonia', NOW()),
('Colorado Appia',        'Cerveja de Trigo com Mel', 'Colorado', 'colorado appia|colorado',       NOW()),
('Colorado Indica',       'IPA',             'Colorado',         'colorado indica|colorado',       NOW()),
('Goose Island IPA',      'IPA',             'Goose Island',     'goose island ipa|goose island',  NOW()),
('Heineken',              'Premium Lager',   'Heineken',         'heineken|heineken',              NOW()),
('Amstel',                'Lager',           'Heineken',         'amstel|heineken',                NOW()),
('Devassa',               'Lager',           'Heineken',         'devassa|heineken',               NOW()),
('Eisenbahn Pilsen',      'Pilsen',          'Eisenbahn',        'eisenbahn pilsen|eisenbahn',     NOW()),
('Baden Baden Golden',    'Golden Ale',      'Baden Baden',      'baden baden golden|baden baden', NOW()),
('Sol',                   'Lager',           'Heineken',         'sol|heineken',                   NOW()),
('Itaipava',              'Pilsen',          'Grupo Petrópolis', 'itaipava|grupo petropolis',      NOW()),
('Petra',                 'Pilsen',          'Grupo Petrópolis', 'petra|grupo petropolis',         NOW()),
('Guinness Draught',      'Stout',           'Guinness',         'guinness draught|guinness',      NOW()),
('Hoegaarden',            'Witbier',         'Hoegaarden',       'hoegaarden|hoegaarden',          NOW()),
('Leffe Blonde',          'Belgian Blonde',  'Leffe',            'leffe blonde|leffe',             NOW()),
('Estrella Galicia',      'Lager',           'Estrella Galicia', 'estrella galicia|estrella galicia', NOW()),
('Chopp da casa',         'Chopp',           NULL,               'chopp da casa|',                 NOW())

ON CONFLICT (chave_busca) DO NOTHING;
