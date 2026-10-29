-- Russian and Ukrainian are back as app languages (11009 had removed them).
--
-- Kokoro could not read Cyrillic, and that was most of the cost of carrying them. Supertonic 3 is now the one
-- local engine and reads both, so the two languages work like any other.
--
-- 11009 dropped their name columns. This adds them back and refills the curated names they held before, word
-- for word from the seed migrations. Material and commodity rows are matched by symbol, the few seeded
-- commodities with no symbol by their English name, subsystems by machine key. A name learned from the journal
-- since 11009 is not filed here. A Russian client fills those in as it shows them, the same as any other
-- client. Nothing ever filled commodity_uk or label_uk, and they come back empty.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.

ALTER TABLE material_names
    ADD COLUMN name_ru TEXT;
ALTER TABLE material_names
    ADD COLUMN name_uk TEXT;
ALTER TABLE commodities
    ADD COLUMN commodity_ru TEXT;
ALTER TABLE commodities
    ADD COLUMN commodity_uk TEXT;
ALTER TABLE sub_system
    ADD COLUMN label_ru TEXT;
ALTER TABLE sub_system
    ADD COLUMN label_uk TEXT;

UPDATE material_names
SET name_ru = 'Захват адаптивного шифровальщика',
    name_uk = 'Захоплення адаптивного шифрувальника'
WHERE symbol = 'adaptiveencryptors';
UPDATE material_names
SET name_ru = 'Данные с обелиска «Альфа»',
    name_uk = 'Дані з обеліска «Альфа»'
WHERE symbol = 'ancientbiologicaldata';
UPDATE material_names
SET name_ru = 'Данные с обелиска «Бета»',
    name_uk = 'Дані з обеліска «Бета»'
WHERE symbol = 'ancientculturaldata';
UPDATE material_names
SET name_ru = 'Данные с обелиска «Гамма»',
    name_uk = 'Дані з обеліска «Гамма»'
WHERE symbol = 'ancienthistoricaldata';
UPDATE material_names
SET name_ru = 'Данные с обелиска «Дельта»',
    name_uk = 'Дані з обеліска «Дельта»'
WHERE symbol = 'ancientlanguagedata';
UPDATE material_names
SET name_ru = 'Данные с обелиска «Эпсилон»',
    name_uk = 'Дані з обеліска «Епсилон»'
WHERE symbol = 'ancienttechnologicaldata';
UPDATE material_names
SET name_ru = 'Сурьма',
    name_uk = 'Сурма'
WHERE symbol = 'antimony';
UPDATE material_names
SET name_ru = 'Нестандартные данные об излучении',
    name_uk = 'Нестандартні дані про випромінювання'
WHERE symbol = 'archivedemissiondata';
UPDATE material_names
SET name_ru = 'Мышьяк',
    name_uk = 'Миш''як'
WHERE symbol = 'arsenic';
UPDATE material_names
SET name_ru = 'Простые проводники',
    name_uk = 'Прості провідники'
WHERE symbol = 'basicconductors';
UPDATE material_names
SET name_ru = 'Биотехнические проводники',
    name_uk = 'Біотехнічні провідники'
WHERE symbol = 'biotechconductors';
UPDATE material_names
SET name_ru = 'Бор',
    name_uk = 'Бор'
WHERE symbol = 'boron';
UPDATE material_names
SET name_ru = 'Аномальный массив данных сканирования',
    name_uk = 'Аномальний масив даних сканування'
WHERE symbol = 'bulkscandata';
UPDATE material_names
SET name_ru = 'Кадмий',
    name_uk = 'Кадмій'
WHERE symbol = 'cadmium';
UPDATE material_names
SET name_ru = 'Углерод',
    name_uk = 'Вуглець'
WHERE symbol = 'carbon';
UPDATE material_names
SET name_ru = 'Оборудование для перегонки химикатов',
    name_uk = 'Обладнання для перегонки хімікатів'
WHERE symbol = 'chemicaldistillery';
UPDATE material_names
SET name_ru = 'Манипуляторы для работы с химикатами',
    name_uk = 'Маніпулятори для роботи з хімікатами'
WHERE symbol = 'chemicalmanipulators';
UPDATE material_names
SET name_ru = 'Оборудование для химобработки',
    name_uk = 'Обладнання для хімобробки'
WHERE symbol = 'chemicalprocessors';
UPDATE material_names
SET name_ru = 'Контейнеры для химикатов',
    name_uk = 'Контейнери для хімікатів'
WHERE symbol = 'chemicalstorageunits';
UPDATE material_names
SET name_ru = 'Хром',
    name_uk = 'Хром'
WHERE symbol = 'chromium';
UPDATE material_names
SET name_ru = 'Засекреченные фрагменты данных сканирования',
    name_uk = 'Засекречені фрагменти даних сканування'
WHERE symbol = 'classifiedscandata';
UPDATE material_names
SET name_ru = 'Спрессованные композиты',
    name_uk = 'Спресовані композити'
WHERE symbol = 'compactcomposites';
UPDATE material_names
SET name_ru = 'Аномальные компактные данные об излучении',
    name_uk = 'Аномальні компактні дані про випромінювання'
WHERE symbol = 'compactemissionsdata';
UPDATE material_names
SET name_ru = 'Многоступенчатая защита',
    name_uk = 'Багатоступеневий захист'
WHERE symbol = 'compoundshielding';
UPDATE material_names
SET name_ru = 'Проводящая керамика',
    name_uk = 'Провідна кераміка'
WHERE symbol = 'conductiveceramics';
UPDATE material_names
SET name_ru = 'Проводящие компоненты',
    name_uk = 'Провідні компоненти'
WHERE symbol = 'conductivecomponents';
UPDATE material_names
SET name_ru = 'Проводящие полимеры',
    name_uk = 'Провідні полімери'
WHERE symbol = 'conductivepolymers';
UPDATE material_names
SET name_ru = 'Настраиваемые компоненты',
    name_uk = 'Налаштовувані компоненти'
WHERE symbol = 'configurablecomponents';
UPDATE material_names
SET name_ru = 'Измененные пользовательские микропрограммы',
    name_uk = 'Змінені користувацькі мікропрограми'
WHERE symbol = 'consumerfirmware';
UPDATE material_names
SET name_ru = 'Осколки кристаллов',
    name_uk = 'Уламки кристалів'
WHERE symbol = 'crystalshards';
UPDATE material_names
SET name_ru = 'Исключения из глубинного анализа данных следа',
    name_uk = 'Винятки з глибинного аналізу даних сліду'
WHERE symbol = 'dataminedwake';
UPDATE material_names
SET name_ru = 'Расшифрованные данные об излучении',
    name_uk = 'Розшифровані дані про випромінювання'
WHERE symbol = 'decodedemissiondata';
UPDATE material_names
SET name_ru = 'Атипичное эхо поврежденного следа',
    name_uk = 'Атипове відлуння пошкодженого сліду'
WHERE symbol = 'disruptedwakeechoes';
UPDATE material_names
SET name_ru = 'Электрохимические массивы',
    name_uk = 'Електрохімічні масиви'
WHERE symbol = 'electrochemicalarrays';
UPDATE material_names
SET name_ru = 'Измененные встроенные микропрограммы',
    name_uk = 'Змінені вбудовані мікропрограми'
WHERE symbol = 'embeddedfirmware';
UPDATE material_names
SET name_ru = 'Неожиданные данные об излучении',
    name_uk = 'Неочікувані дані про випромінювання'
WHERE symbol = 'emissiondata';
UPDATE material_names
SET name_ru = 'Неформатные данные сканирования',
    name_uk = 'Неформатні дані сканування'
WHERE symbol = 'encodedscandata';
UPDATE material_names
SET name_ru = 'Особые зашифрованные файлы',
    name_uk = 'Особливі зашифровані файли'
WHERE symbol = 'encryptedfiles';
UPDATE material_names
SET name_ru = 'Нетипичные архивы шифрования',
    name_uk = 'Нетипові архіви шифрування'
WHERE symbol = 'encryptionarchives';
UPDATE material_names
SET name_ru = 'Меченые шифровальные коды',
    name_uk = 'Мічені шифрувальні коди'
WHERE symbol = 'encryptioncodes';
UPDATE material_names
SET name_ru = 'Отборные фокусировочные кристаллы',
    name_uk = 'Добірні фокусувальні кристали'
WHERE symbol = 'exquisitefocuscrystals';
UPDATE material_names
SET name_ru = 'Композиты Core Dynamics',
    name_uk = 'Композити Core Dynamics'
WHERE symbol = 'fedcorecomposites';
UPDATE material_names
SET name_ru = 'Патентованные композиты',
    name_uk = 'Патентовані композити'
WHERE symbol = 'fedproprietarycomposites';
UPDATE material_names
SET name_ru = 'Волокнистые композиты',
    name_uk = 'Волокнисті композити'
WHERE symbol = 'filamentcomposites';
UPDATE material_names
SET name_ru = 'Фокусировочные кристаллы',
    name_uk = 'Фокусувальні кристали'
WHERE symbol = 'focuscrystals';
UPDATE material_names
SET name_ru = 'Аномальная телеметрия FSD',
    name_uk = 'Аномальна телеметрія FSD'
WHERE symbol = 'fsdtelemetry';
UPDATE material_names
SET name_ru = 'Сплавы для гальванизации',
    name_uk = 'Сплави для гальванізації'
WHERE symbol = 'galvanisingalloys';
UPDATE material_names
SET name_ru = 'Германий',
    name_uk = 'Германій'
WHERE symbol = 'germanium';
UPDATE material_names
SET name_ru = 'Наборные резисторы',
    name_uk = 'Набірні резистори'
WHERE symbol = 'gridresistors';
UPDATE material_names
SET name_ru = 'Фрагмент чертежа модуля Стражей',
    name_uk = 'Фрагмент креслення модуля Стражів'
WHERE symbol = 'guardian_moduleblueprint';
UPDATE material_names
SET name_ru = 'Энергоячейка Стражей',
    name_uk = 'Енергокомірка Стражів'
WHERE symbol = 'guardian_powercell';
UPDATE material_names
SET name_ru = 'Энергопроводники Стражей',
    name_uk = 'Енергопроводи Стражів'
WHERE symbol = 'guardian_powerconduit';
UPDATE material_names
SET name_ru = 'Детали вооружения часовых Стражей',
    name_uk = 'Деталі озброєння вартових Стражів'
WHERE symbol = 'guardian_sentinel_weaponparts';
UPDATE material_names
SET name_ru = 'Обломки кораблекрушения Стражей',
    name_uk = 'Уламки корабельної аварії Стражів'
WHERE symbol = 'guardian_sentinel_wreckagecomponents';
UPDATE material_names
SET name_ru = 'Компоненты технологий Стражей',
    name_uk = 'Компоненти технологій Стражів'
WHERE symbol = 'guardian_techcomponent';
UPDATE material_names
SET name_ru = 'Фрагмент чертежа судна Стражей',
    name_uk = 'Фрагмент креслення судна Стражів'
WHERE symbol = 'guardian_vesselblueprint';
UPDATE material_names
SET name_ru = 'Фрагмент чертежа оружия Стражей',
    name_uk = 'Фрагмент креслення зброї Стражів'
WHERE symbol = 'guardian_weaponblueprint';
UPDATE material_names
SET name_ru = 'Теплопроводящие провода',
    name_uk = 'Теплопровідні дроти'
WHERE symbol = 'heatconductionwiring';
UPDATE material_names
SET name_ru = 'Теплорассеивающая пластина',
    name_uk = 'Теплорозсіювальна пластина'
WHERE symbol = 'heatdispersionplate';
UPDATE material_names
SET name_ru = 'Теплообменные агрегаты',
    name_uk = 'Теплообмінні агрегати'
WHERE symbol = 'heatexchangers';
UPDATE material_names
SET name_ru = 'Жаропрочная керамика',
    name_uk = 'Жаростійка кераміка'
WHERE symbol = 'heatresistantceramics';
UPDATE material_names
SET name_ru = 'Тепловые заслонки',
    name_uk = 'Теплові заслінки'
WHERE symbol = 'heatvanes';
UPDATE material_names
SET name_ru = 'Высокоплотностные композиты',
    name_uk = 'Високощільні композити'
WHERE symbol = 'highdensitycomposites';
UPDATE material_names
SET name_ru = 'Гибридные конденсаторы',
    name_uk = 'Гібридні конденсатори'
WHERE symbol = 'hybridcapacitors';
UPDATE material_names
SET name_ru = 'Аномальные траектории в гиперпространстве',
    name_uk = 'Аномальні траєкторії в гіперпросторі'
WHERE symbol = 'hyperspacetrajectories';
UPDATE material_names
SET name_ru = 'Имперская защита',
    name_uk = 'Імперський захист'
WHERE symbol = 'imperialshielding';
UPDATE material_names
SET name_ru = 'Кустарные компоненты',
    name_uk = 'Кустарні компоненти'
WHERE symbol = 'improvisedcomponents';
UPDATE material_names
SET name_ru = 'Взломанные промышленные микропрограммы',
    name_uk = 'Зламані промислові мікропрограми'
WHERE symbol = 'industrialfirmware';
UPDATE material_names
SET name_ru = 'Железо',
    name_uk = 'Залізо'
WHERE symbol = 'iron';
UPDATE material_names
SET name_ru = 'Свинец',
    name_uk = 'Свинець'
WHERE symbol = 'lead';
UPDATE material_names
SET name_ru = 'Специальные микропрограммы предыдущего поколения',
    name_uk = 'Спеціальні мікропрограми попереднього покоління'
WHERE symbol = 'legacyfirmware';
UPDATE material_names
SET name_ru = 'Марганец',
    name_uk = 'Марганець'
WHERE symbol = 'manganese';
UPDATE material_names
SET name_ru = 'Механические компоненты',
    name_uk = 'Механічні компоненти'
WHERE symbol = 'mechanicalcomponents';
UPDATE material_names
SET name_ru = 'Механическое оборудование',
    name_uk = 'Механічне обладнання'
WHERE symbol = 'mechanicalequipment';
UPDATE material_names
SET name_ru = 'Механические отходы',
    name_uk = 'Механічні відходи'
WHERE symbol = 'mechanicalscrap';
UPDATE material_names
SET name_ru = 'Ртуть',
    name_uk = 'Ртуть'
WHERE symbol = 'mercury';
UPDATE material_names
SET name_ru = 'Сплавы военного класса',
    name_uk = 'Сплави військового класу'
WHERE symbol = 'militarygradealloys';
UPDATE material_names
SET name_ru = 'Военные суперконденсаторы',
    name_uk = 'Військові суперконденсатори'
WHERE symbol = 'militarysupercapacitors';
UPDATE material_names
SET name_ru = 'Молибден',
    name_uk = 'Молібден'
WHERE symbol = 'molybdenum';
UPDATE material_names
SET name_ru = 'Никель',
    name_uk = 'Нікель'
WHERE symbol = 'nickel';
UPDATE material_names
SET name_ru = 'Ниобий',
    name_uk = 'Ніобій'
WHERE symbol = 'niobium';
UPDATE material_names
SET name_ru = 'Фармацевтические изоляционные материалы',
    name_uk = 'Фармацевтичні ізоляційні матеріали'
WHERE symbol = 'pharmaceuticalisolators';
UPDATE material_names
SET name_ru = 'Фазовые сплавы',
    name_uk = 'Фазові сплави'
WHERE symbol = 'phasealloys';
UPDATE material_names
SET name_ru = 'Фосфор',
    name_uk = 'Фосфор'
WHERE symbol = 'phosphorus';
UPDATE material_names
SET name_ru = 'Полоний',
    name_uk = 'Полоній'
WHERE symbol = 'polonium';
UPDATE material_names
SET name_ru = 'Полимерные конденсаторы',
    name_uk = 'Полімерні конденсатори'
WHERE symbol = 'polymercapacitors';
UPDATE material_names
SET name_ru = 'Осажденные сплавы',
    name_uk = 'Осаджені сплави'
WHERE symbol = 'precipitatedalloys';
UPDATE material_names
SET name_ru = 'Прототипы теплоизлучателей',
    name_uk = 'Прототипи тепловипромінювачів'
WHERE symbol = 'protoheatradiators';
UPDATE material_names
SET name_ru = 'Опытные легкие сплавы',
    name_uk = 'Дослідні легкі сплави'
WHERE symbol = 'protolightalloys';
UPDATE material_names
SET name_ru = 'Сплавы для изготовления зондов',
    name_uk = 'Сплави для виготовлення зондів'
WHERE symbol = 'protoradiolicalloys';
UPDATE material_names
SET name_ru = 'Обработанные фокусировочные кристаллы',
    name_uk = 'Оброблені фокусувальні кристали'
WHERE symbol = 'refinedfocuscrystals';
UPDATE material_names
SET name_ru = 'Рений',
    name_uk = 'Реній'
WHERE symbol = 'rhenium';
UPDATE material_names
SET name_ru = 'Рутений',
    name_uk = 'Рутеній'
WHERE symbol = 'ruthenium';
UPDATE material_names
SET name_ru = 'Захваченные сплавы',
    name_uk = 'Захоплені сплави'
WHERE symbol = 'salvagedalloys';
UPDATE material_names
SET name_ru = 'Неопознанные архивы сканирования',
    name_uk = 'Невпізнані архіви сканування'
WHERE symbol = 'scanarchives';
UPDATE material_names
SET name_ru = 'Засекреченные базы данных сканирования',
    name_uk = 'Засекречені бази даних сканування'
WHERE symbol = 'scandatabanks';
UPDATE material_names
SET name_ru = 'Исключительные зашифрованные данные об излучении',
    name_uk = 'Виняткові зашифровані дані про випромінювання'
WHERE symbol = 'scrambledemissiondata';
UPDATE material_names
SET name_ru = 'Обновление для защитной микропрограммы',
    name_uk = 'Оновлення для захисної мікропрограми'
WHERE symbol = 'securityfirmware';
UPDATE material_names
SET name_ru = 'Селен',
    name_uk = 'Селен'
WHERE symbol = 'selenium';
UPDATE material_names
SET name_ru = 'Поврежденные цикличные записи щита',
    name_uk = 'Пошкоджені циклічні записи щита'
WHERE symbol = 'shieldcyclerecordings';
UPDATE material_names
SET name_ru = 'Нетипичные данные сканирования щитов',
    name_uk = 'Нетипові дані сканування щитів'
WHERE symbol = 'shielddensityreports';
UPDATE material_names
SET name_ru = 'Щитоизлучатели',
    name_uk = 'Щитовипромінювачі'
WHERE symbol = 'shieldemitters';
UPDATE material_names
SET name_ru = 'Специфические данные о частоте щитов',
    name_uk = 'Специфічні дані про частоту щитів'
WHERE symbol = 'shieldfrequencydata';
UPDATE material_names
SET name_ru = 'Сенсоры системы экранирования',
    name_uk = 'Сенсори системи екранування'
WHERE symbol = 'shieldingsensors';
UPDATE material_names
SET name_ru = 'Анализ аномального поведения щита',
    name_uk = 'Аналіз аномальної поведінки щита'
WHERE symbol = 'shieldpatternanalysis';
UPDATE material_names
SET name_ru = 'Неполный анализ поглощения щита',
    name_uk = 'Неповний аналіз поглинання щита'
WHERE symbol = 'shieldsoakanalysis';
UPDATE material_names
SET name_ru = 'Сера',
    name_uk = 'Сірка'
WHERE symbol = 'sulphur';
UPDATE material_names
SET name_ru = 'Открытые симметричные ключи',
    name_uk = 'Відкриті симетричні ключі'
WHERE symbol = 'symmetrickeys';
UPDATE material_names
SET name_ru = 'Технеций',
    name_uk = 'Технецій'
WHERE symbol = 'technetium';
UPDATE material_names
SET name_ru = 'Теллур',
    name_uk = 'Телур'
WHERE symbol = 'tellurium';
UPDATE material_names
SET name_ru = 'Закаленные сплавы',
    name_uk = 'Загартовані сплави'
WHERE symbol = 'temperedalloys';
UPDATE material_names
SET name_ru = 'Образец теплового воздействия',
    name_uk = 'Зразок теплового впливу'
WHERE symbol = 'tg_abrasion01';
UPDATE material_names
SET name_ru = 'Остаток фазирующей мембраны',
    name_uk = 'Залишок фазувальної мембрани'
WHERE symbol = 'tg_abrasion02';
UPDATE material_names
SET name_ru = 'Окаменелые фрагменты поверхности',
    name_uk = 'Затверділі фрагменти поверхні'
WHERE symbol = 'tg_abrasion03';
UPDATE material_names
SET name_ru = 'Биомеханические энергопроводники',
    name_uk = 'Біомеханічні енергопроводи'
WHERE symbol = 'tg_biomechanicalconduits';
UPDATE material_names
SET name_ru = 'Едкий кристалл',
    name_uk = 'Їдкий кристал'
WHERE symbol = 'tg_causticcrystal';
UPDATE material_names
SET name_ru = 'Разъедающие механизмы',
    name_uk = 'Роз''їдаючі механізми'
WHERE symbol = 'tg_causticgeneratorparts';
UPDATE material_names
SET name_ru = 'Едкий осколок',
    name_uk = 'Їдкий уламок'
WHERE symbol = 'tg_causticshard';
UPDATE material_names
SET name_ru = 'Данные о составе таргоидских материалов',
    name_uk = 'Дані про склад таргоїдських матеріалів'
WHERE symbol = 'tg_compositiondata';
UPDATE material_names
SET name_ru = 'Телеметрия перехвата таргоидами',
    name_uk = 'Телеметрія перехоплення таргоїдами'
WHERE symbol = 'tg_interdictiondata';
UPDATE material_names
SET name_ru = 'Реактивные элементы',
    name_uk = 'Реактивні елементи'
WHERE symbol = 'tg_propulsionelement';
UPDATE material_names
SET name_ru = 'Данные об осадке таргоидского происхождения',
    name_uk = 'Дані про осад таргоїдського походження'
WHERE symbol = 'tg_residuedata';
UPDATE material_names
SET name_ru = 'Полетные данные корабля',
    name_uk = 'Польотні дані корабля'
WHERE symbol = 'tg_shipflightdata';
UPDATE material_names
SET name_ru = 'Данные бортовых систем корабля',
    name_uk = 'Дані бортових систем корабля'
WHERE symbol = 'tg_shipsystemsdata';
UPDATE material_names
SET name_ru = 'Параметры сильного энергетического импульса',
    name_uk = 'Параметри сильного енергетичного імпульсу'
WHERE symbol = 'tg_shutdowndata';
UPDATE material_names
SET name_ru = 'Данные о структуре таргоидского объекта',
    name_uk = 'Дані про структуру таргоїдського об''єкта'
WHERE symbol = 'tg_structuraldata';
UPDATE material_names
SET name_ru = 'Детали вооружения',
    name_uk = 'Деталі озброєння'
WHERE symbol = 'tg_weaponparts';
UPDATE material_names
SET name_ru = 'Обломки кораблекрушений',
    name_uk = 'Уламки корабельних аварій'
WHERE symbol = 'tg_wreckagecomponents';
UPDATE material_names
SET name_ru = 'Термические сплавы',
    name_uk = 'Термічні сплави'
WHERE symbol = 'thermicalloys';
UPDATE material_names
SET name_ru = 'Олово',
    name_uk = 'Олово'
WHERE symbol = 'tin';
UPDATE material_names
SET name_ru = 'Вольфрам',
    name_uk = 'Вольфрам'
WHERE symbol = 'tungsten';
UPDATE material_names
SET name_ru = 'Поврежденные фокусировочные кристаллы',
    name_uk = 'Пошкоджені фокусувальні кристали'
WHERE symbol = 'uncutfocuscrystals';
UPDATE material_names
SET name_ru = 'Неизвестно',
    name_uk = 'Невідомо'
WHERE symbol = 'unknown';
UPDATE material_names
SET name_ru = 'Таргоидский панцирь',
    name_uk = 'Таргоїдський панцир'
WHERE symbol = 'unknowncarapace';
UPDATE material_names
SET name_ru = 'Чип тактического ядра',
    name_uk = 'Чип тактичного ядра'
WHERE symbol = 'unknowncorechip';
UPDATE material_names
SET name_ru = 'Таргоидская энергоячейка',
    name_uk = 'Таргоїдська енергокомірка'
WHERE symbol = 'unknownenergycell';
UPDATE material_names
SET name_ru = 'Обломок сенсора',
    name_uk = 'Уламок сенсора'
WHERE symbol = 'unknownenergysource';
UPDATE material_names
SET name_ru = 'Таргоидская органическая схема',
    name_uk = 'Таргоїдська органічна схема'
WHERE symbol = 'unknownorganiccircuitry';
UPDATE material_names
SET name_ru = 'Сигнатура таргоидского корабля',
    name_uk = 'Сигнатура таргоїдського корабля'
WHERE symbol = 'unknownshipsignature';
UPDATE material_names
SET name_ru = 'Компоненты таргоидской техники',
    name_uk = 'Компоненти таргоїдської техніки'
WHERE symbol = 'unknowntechnologycomponents';
UPDATE material_names
SET name_ru = 'Данные следа таргоидского корабля',
    name_uk = 'Дані сліду таргоїдського корабля'
WHERE symbol = 'unknownwakedata';
UPDATE material_names
SET name_ru = 'Ванадий',
    name_uk = 'Ванадій'
WHERE symbol = 'vanadium';
UPDATE material_names
SET name_ru = 'Странные расчеты следа',
    name_uk = 'Дивні розрахунки сліду'
WHERE symbol = 'wakesolutions';
UPDATE material_names
SET name_ru = 'Изношенные щитоизлучатели',
    name_uk = 'Зношені щитовипромінювачі'
WHERE symbol = 'wornshieldemitters';
UPDATE material_names
SET name_ru = 'Иттрий',
    name_uk = 'Ітрій'
WHERE symbol = 'yttrium';
UPDATE material_names
SET name_ru = 'Цинк',
    name_uk = 'Цинк'
WHERE symbol = 'zinc';
UPDATE material_names
SET name_ru = 'Цирконий',
    name_uk = 'Цирконій'
WHERE symbol = 'zirconium';

UPDATE commodities
SET commodity_ru = 'Формула 42 AZ Cancri'
WHERE symbol = 'AZCancriFormula42';
UPDATE commodities
SET commodity_ru = 'Улучшенные катализаторы'
WHERE symbol = 'AdvancedCatalysers';
UPDATE commodities
SET commodity_ru = 'Новейшие лекарства'
WHERE symbol = 'AdvancedMedicines';
UPDATE commodities
SET commodity_ru = 'Экспериментальные ультракомпактные процессоры'
WHERE symbol = 'Advert1';
UPDATE commodities
SET commodity_ru = 'Райские яблоки с Эриала'
WHERE symbol = 'AerialEdenApple';
UPDATE commodities
SET commodity_ru = 'Аганиппийский кайф'
WHERE symbol = 'AganippeRush';
UPDATE commodities
SET commodity_ru = 'Ветмедикаменты'
WHERE symbol = 'AgriculturalMedicines';
UPDATE commodities
SET commodity_ru = 'Средство очистки почвы'
WHERE symbol = 'AgronomicTreatment';
UPDATE commodities
SET commodity_ru = 'Фрагменты ИИ'
WHERE symbol = 'AiRelics';
UPDATE commodities
SET commodity_ru = 'Алакаракмовские татуировки'
WHERE symbol = 'AlacarakmoSkinArt';
UPDATE commodities
SET commodity_ru = 'Мясо белого кечуанского мамонта'
WHERE symbol = 'AlbinoQuechuaMammoth';
UPDATE commodities
SET commodity_ru = 'Александрит'
WHERE symbol = 'Alexandrite';
UPDATE commodities
SET commodity_ru = 'Водоросли'
WHERE symbol = 'Algae';
UPDATE commodities
SET commodity_ru = 'Кожистые яйца'
WHERE symbol = 'AlienEggs';
UPDATE commodities
SET commodity_ru = 'Альтаирская кожа'
WHERE symbol = 'AltairianSkin';
UPDATE commodities
SET commodity_ru = 'Алюминий'
WHERE symbol = 'Aluminium';
UPDATE commodities
SET commodity_ru = 'Алийское мыло для тела'
WHERE symbol = 'AlyaBodilySoap';
UPDATE commodities
SET commodity_ru = 'Шкатулка Стражей'
WHERE symbol = 'AncientCasket';
UPDATE commodities
SET commodity_ru = 'Древний ключ'
WHERE symbol = 'AncientKey';
UPDATE commodities
SET commodity_ru = 'Сфера Стражей'
WHERE symbol = 'AncientOrb';
UPDATE commodities
SET commodity_ru = 'Реликвия Стражей'
WHERE symbol = 'AncientRelic';
UPDATE commodities
SET commodity_ru = 'Неопознанная реликвия'
WHERE symbol = 'AncientRelicTG';
UPDATE commodities
SET commodity_ru = 'Табличка Стражей'
WHERE symbol = 'AncientTablet';
UPDATE commodities
SET commodity_ru = 'Тотем Стражей'
WHERE symbol = 'AncientTotem';
UPDATE commodities
SET commodity_ru = 'Урна Стражей'
WHERE symbol = 'AncientUrn';
UPDATE commodities
SET commodity_ru = 'Андулигские фейерверки'
WHERE symbol = 'AnduligaFireWorks';
UPDATE commodities
SET commodity_ru = 'Кромская серебряная дурь'
WHERE symbol = 'AnimalEffigies';
UPDATE commodities
SET commodity_ru = 'Мониторы фауны'
WHERE symbol = 'AnimalMonitors';
UPDATE commodities
SET commodity_ru = 'Мясо животных'
WHERE symbol = 'Animalmeat';
UPDATE commodities
SET commodity_ru = 'Контейнер с антиматерией'
WHERE symbol = 'AntimatterContainmentUnit';
UPDATE commodities
SET commodity_ru = 'Древние ювелирные украшения'
WHERE symbol = 'AntiqueJewellery';
UPDATE commodities
SET commodity_ru = 'Древние реликвии'
WHERE symbol = 'Antiquities';
UPDATE commodities
SET commodity_ru = 'Кофе Any Na'
WHERE symbol = 'AnyNaCoffee';
UPDATE commodities
SET commodity_ru = 'Apa Vietii'
WHERE symbol = 'ApaVietii';
UPDATE commodities
SET commodity_ru = 'Аквапонные системы'
WHERE symbol = 'AquaponicSystems';
UPDATE commodities
SET commodity_ru = 'Монастырские сладости Ароуки'
WHERE symbol = 'AroucaConventualSweets';
UPDATE commodities
SET commodity_ru = 'Шарнирные моторы'
WHERE symbol = 'ArticulationMotors';
UPDATE commodities
SET commodity_ru = 'Планы атак'
WHERE symbol = 'AssaultPlans';
UPDATE commodities
SET commodity_ru = 'Атмосферный процессор'
WHERE symbol = 'AtmosphericExtractors';
UPDATE commodities
SET commodity_ru = 'Автосинтезаторы'
WHERE symbol = 'AutoFabricators';
UPDATE commodities
SET commodity_ru = 'Печеные гриблы'
WHERE symbol = 'BakedGreebles';
UPDATE commodities
SET commodity_ru = 'Вакуумный криль с Балтах’сине'
WHERE symbol = 'BaltahSineVacuumKrill';
UPDATE commodities
SET commodity_ru = 'Кожа банкийских амфибий'
WHERE symbol = 'BankiAmphibiousLeather';
UPDATE commodities
SET commodity_ru = 'Основные лекарства'
WHERE symbol = 'BasicMedicines';
UPDATE commodities
SET commodity_ru = 'Наркотики'
WHERE symbol = 'BasicNarcotics';
UPDATE commodities
SET commodity_ru = 'Настойка из бастетовых змей'
WHERE symbol = 'BastSnakeGin';
UPDATE commodities
SET commodity_ru = 'Военное оружие'
WHERE symbol = 'BattleWeapons';
UPDATE commodities
SET commodity_ru = 'Боксит'
WHERE symbol = 'Bauxite';
UPDATE commodities
SET commodity_ru = 'Пиво'
WHERE symbol = 'Beer';
UPDATE commodities
SET commodity_ru = 'Кожа белаланских скатов'
WHERE symbol = 'BelalansRayLeather';
UPDATE commodities
SET commodity_ru = 'Бенитоит'
WHERE symbol = 'Benitoite';
UPDATE commodities
SET commodity_ru = 'Бертрандит'
WHERE symbol = 'Bertrandite';
UPDATE commodities
SET commodity_ru = 'Бериллий'
WHERE symbol = 'Beryllium';
UPDATE commodities
SET commodity_ru = 'Лишайник-биоредуктор'
WHERE symbol = 'BioReducingLichen';
UPDATE commodities
SET commodity_ru = 'Биоотходы'
WHERE symbol = 'Biowaste';
UPDATE commodities
SET commodity_ru = 'Висмут'
WHERE symbol = 'Bismuth';
UPDATE commodities
SET commodity_ru = 'Лазурное молоко'
WHERE symbol = 'BlueMilk';
UPDATE commodities
SET commodity_ru = 'Самогон'
WHERE symbol = 'BootlegLiquor';
UPDATE commodities
SET commodity_ru = 'Боразетанские патогены'
WHERE symbol = 'BorasetaniPathogenetics';
UPDATE commodities
SET commodity_ru = 'Бромеллит'
WHERE symbol = 'Bromellite';
UPDATE commodities
SET commodity_ru = 'Подставка под пиво Buckyball'
WHERE symbol = 'BuckyballBeerMats';
UPDATE commodities
SET commodity_ru = 'Строительные синтезаторы'
WHERE symbol = 'BuildingFabricators';
UPDATE commodities
SET commodity_ru = 'Бернхемский желчный дистиллят'
WHERE symbol = 'BurnhamBileDistillate';
UPDATE commodities
SET commodity_ru = 'Кошачий кофе CD-75'
WHERE symbol = 'CD75CatCoffee';
UPDATE commodities
SET commodity_ru = 'CMM-композит'
WHERE symbol = 'CMMComposite';
UPDATE commodities
SET commodity_ru = 'Кентаврский мега-джин'
WHERE symbol = 'CentauriMegaGin';
UPDATE commodities
SET commodity_ru = 'Керамокомпозиты'
WHERE symbol = 'CeramicComposites';
UPDATE commodities
SET commodity_ru = 'Церемониальный чай Heike'
WHERE symbol = 'CeremonialHeikeTea';
UPDATE commodities
SET commodity_ru = 'Яйцо эпиорниса'
WHERE symbol = 'CetiAepyornisEgg';
UPDATE commodities
SET commodity_ru = 'Кролики с Кита'
WHERE symbol = 'CetiRabbits';
UPDATE commodities
SET commodity_ru = 'Ткань-хамелеон'
WHERE symbol = 'ChameleonCloth';
UPDATE commodities
SET commodity_ru = 'Шато де Эгеон'
WHERE symbol = 'ChateauDeAegaeon';
UPDATE commodities
SET commodity_ru = 'Радиоактивные материалы'
WHERE symbol = 'ChemicalWaste';
UPDATE commodities
SET commodity_ru = 'Чербонские кровавые кристаллы'
WHERE symbol = 'CherbonesBloodCrystals';
UPDATE commodities
SET commodity_ru = 'Морская паста с Чи Эридана'
WHERE symbol = 'ChiEridaniMarinePaste';
UPDATE commodities
SET commodity_ru = 'Классифицированное экспериментальное оборудование'
WHERE symbol = 'ClassifiedExperimentalEquipment';
UPDATE commodities
SET commodity_ru = 'Одежда'
WHERE symbol = 'Clothing';
UPDATE commodities
SET commodity_ru = 'Кобальт'
WHERE symbol = 'Cobalt';
UPDATE commodities
SET commodity_ru = 'Кофе'
WHERE symbol = 'Coffee';
UPDATE commodities
SET commodity_ru = 'Колтан'
WHERE symbol = 'Coltan';
UPDATE commodities
SET commodity_ru = 'Боевые стабилизаторы'
WHERE symbol = 'CombatStabilisers';
UPDATE commodities
SET commodity_ru = 'Рекламные образцы'
WHERE symbol = 'ComercialSamples';
UPDATE commodities
SET commodity_ru = 'Компьютерные компоненты'
WHERE symbol = 'ComputerComponents';
UPDATE commodities
SET commodity_ru = 'Проводящая ткань'
WHERE symbol = 'ConductiveFabrics';
UPDATE commodities
SET commodity_ru = 'Потребительские товары'
WHERE symbol = 'ConsumerTechnology';
UPDATE commodities
SET commodity_ru = 'Шланги системы охлаждения малых диаметров'
WHERE symbol = 'CoolingHoses';
UPDATE commodities
SET commodity_ru = 'Медь'
WHERE symbol = 'Copper';
UPDATE commodities
SET commodity_ru = 'Губковые продукты с Кокуима'
WHERE symbol = 'CoquimSpongiformVictuals';
UPDATE commodities
SET commodity_ru = 'Коралловая смола'
WHERE symbol = 'CoralSap';
UPDATE commodities
SET commodity_ru = 'Уборочный комбайн'
WHERE symbol = 'CropHarvesters';
UPDATE commodities
SET commodity_ru = 'Криолит'
WHERE symbol = 'Cryolite';
UPDATE commodities
SET commodity_ru = 'Прозрачные сферы'
WHERE symbol = 'CrystallineSpheres';
UPDATE commodities
SET commodity_ru = 'Поврежденная спасательная капсула'
WHERE symbol = 'DamagedEscapePod';
UPDATE commodities
SET commodity_ru = 'Панцири с Дамны'
WHERE symbol = 'DamnaCarapaces';
UPDATE commodities
SET commodity_ru = 'Ядро данных'
WHERE symbol = 'DataCore';
UPDATE commodities
SET commodity_ru = 'Фениксовые пальмы с Дельты'
WHERE symbol = 'DeltaPhoenicisPalms';
UPDATE commodities
SET commodity_ru = 'Трюфели с Деуринги'
WHERE symbol = 'DeuringasTruffles';
UPDATE commodities
SET commodity_ru = 'Сенсор диагностики оборудования'
WHERE symbol = 'DiagnosticSensor';
UPDATE commodities
SET commodity_ru = 'Дипломатическая сумка'
WHERE symbol = 'DiplomaticBag';
UPDATE commodities
SET commodity_ru = 'Зерна дисо ма'
WHERE symbol = 'DisoMaCorn';
UPDATE commodities
SET commodity_ru = 'Бытовая техника'
WHERE symbol = 'DomesticAppliances';
UPDATE commodities
SET commodity_ru = 'Дроны'
WHERE symbol = 'Drones';
UPDATE commodities
SET commodity_ru = 'Дюрадрайвы'
WHERE symbol = 'Duradrives';
UPDATE commodities
SET commodity_ru = 'Реликвии с Земли'
WHERE symbol = 'EarthRelics';
UPDATE commodities
SET commodity_ru = 'Элеусская теплая одежда'
WHERE symbol = 'EleuThermals';
UPDATE commodities
SET commodity_ru = 'Аварийные энергоячейки'
WHERE symbol = 'EmergencyPowerCells';
UPDATE commodities
SET commodity_ru = 'Зашифрованный носитель данных'
WHERE symbol = 'EncriptedDataStorage';
UPDATE commodities
SET commodity_ru = 'Шифрованная переписка'
WHERE symbol = 'EncryptedCorrespondence';
UPDATE commodities
SET commodity_ru = 'Эранинское жемчужное виски'
WHERE symbol = 'EraninPearlWhisky';
UPDATE commodities
SET commodity_ru = 'Зонтики Эшу'
WHERE symbol = 'EshuUmbrellas';
UPDATE commodities
SET commodity_ru = 'Эсусекусская осетровая икра'
WHERE symbol = 'EsusekuCaviar';
UPDATE commodities
SET commodity_ru = 'Чайные бутоны Этгриза'
WHERE symbol = 'EthgrezeTeaBuds';
UPDATE commodities
SET commodity_ru = 'Эвакуационное убежище'
WHERE symbol = 'EvacuationShelter';
UPDATE commodities
SET commodity_ru = 'Выпускной коллектор'
WHERE symbol = 'ExhaustManifold';
UPDATE commodities
SET commodity_ru = 'Взрывчатка'
WHERE symbol = 'Explosives';
UPDATE commodities
SET commodity_ru = 'Рыба'
WHERE symbol = 'Fish';
UPDATE commodities
SET commodity_ru = 'Пищевые брикеты'
WHERE symbol = 'FoodCartridges';
UPDATE commodities
SET commodity_ru = 'Ископаемые останки'
WHERE symbol = 'FossilRemnants';
UPDATE commodities
SET commodity_ru = 'Фрукты и овощи'
WHERE symbol = 'FruitAndVegetables';
UPDATE commodities
SET commodity_ru = 'Фудзинский чай'
WHERE symbol = 'FujinTea';
UPDATE commodities
SET commodity_ru = 'Путеводитель галактического путешественника'
WHERE symbol = 'GalacticTravelGuide';
UPDATE commodities
SET commodity_ru = 'Галлит'
WHERE symbol = 'Gallite';
UPDATE commodities
SET commodity_ru = 'Галлий'
WHERE symbol = 'Gallium';
UPDATE commodities
SET commodity_ru = 'Геавенская танцевальная пыль'
WHERE symbol = 'GeawenDanceDust';
UPDATE commodities
SET commodity_ru = 'Генотека'
WHERE symbol = 'GeneBank';
UPDATE commodities
SET commodity_ru = 'Геологическое оборудование'
WHERE symbol = 'GeologicalEquipment';
UPDATE commodities
SET commodity_ru = 'Образцы породы'
WHERE symbol = 'GeologicalSamples';
UPDATE commodities
SET commodity_ru = 'Герасианское пиво гез'
WHERE symbol = 'GerasianGueuzeBeer';
UPDATE commodities
SET commodity_ru = 'Огромные улитки с Ирукамы'
WHERE symbol = 'GiantIrukamaSnails';
UPDATE commodities
SET commodity_ru = 'Гигант Веррикс'
WHERE symbol = 'GiantVerrix';
UPDATE commodities
SET commodity_ru = 'Именное оружие из системы Гилья'
WHERE symbol = 'GilyaSignatureWeapons';
UPDATE commodities
SET commodity_ru = 'Золото'
WHERE symbol = 'Gold';
UPDATE commodities
SET commodity_ru = 'Кофе Goman Yaupon'
WHERE symbol = 'GomanYauponCoffee';
UPDATE commodities
SET commodity_ru = 'Госларит'
WHERE symbol = 'Goslarite';
UPDATE commodities
SET commodity_ru = 'Зерно'
WHERE symbol = 'Grain';
UPDATE commodities
SET commodity_ru = 'Грандидьерит'
WHERE symbol = 'Grandidierite';
UPDATE commodities
SET commodity_ru = 'Мясо лесной дичи с HIP 10175'
WHERE symbol = 'HIP10175BushMeat';
UPDATE commodities
SET commodity_ru = 'Рой HIP 118311'
WHERE symbol = 'HIP118311Swarm';
UPDATE commodities
SET commodity_ru = 'Протокальмар с HIP'
WHERE symbol = 'HIP41181Squid';
UPDATE commodities
SET commodity_ru = 'Мощные органофосфаты'
WHERE symbol = 'HIPOrganophosphates';
UPDATE commodities
SET commodity_ru = 'Разрядная установка HN'
WHERE symbol = 'HNShockMount';
UPDATE commodities
SET commodity_ru = 'Пшеница HR 7221'
WHERE symbol = 'HR7221Wheat';
UPDATE commodities
SET commodity_ru = 'Гематит'
WHERE symbol = 'Haematite';
UPDATE commodities
SET commodity_ru = 'Гафний-178'
WHERE symbol = 'Hafnium178';
UPDATE commodities
SET commodity_ru = 'Гайденовский черный напиток'
WHERE symbol = 'HaidneBlackBrew';
UPDATE commodities
SET commodity_ru = 'Ром «Серебряное море Harma»'
WHERE symbol = 'HarmaSilverSeaRum';
UPDATE commodities
SET commodity_ru = 'Хавасупайский ловец снов'
WHERE symbol = 'HavasupaiDreamCatcher';
UPDATE commodities
SET commodity_ru = 'Защитные костюмы'
WHERE symbol = 'HazardousEnvironmentSuits';
UPDATE commodities
SET commodity_ru = 'Радиаторный соединитель'
WHERE symbol = 'HeatsinkInterlink';
UPDATE commodities
SET commodity_ru = 'Микробные печи'
WHERE symbol = 'HeliostaticFurnaces';
UPDATE commodities
SET commodity_ru = 'Жемчуг с Хельветитч'
WHERE symbol = 'HelvetitjPearls';
UPDATE commodities
SET commodity_ru = 'Хольванские дуэльные клинки'
WHERE symbol = 'HolvaDuellingBlades';
UPDATE commodities
SET commodity_ru = 'Пилюли честности'
WHERE symbol = 'HonestyPills';
UPDATE commodities
SET commodity_ru = 'Заложники'
WHERE symbol = 'Hostage';
UPDATE commodities
SET commodity_ru = 'Водородное топливо'
WHERE symbol = 'HydrogenFuel';
UPDATE commodities
SET commodity_ru = 'Пероксид водорода'
WHERE symbol = 'HydrogenPeroxide';
UPDATE commodities
SET commodity_ru = 'Имперские рабы'
WHERE symbol = 'ImperialSlaves';
UPDATE commodities
SET commodity_ru = 'Инди-бурбон'
WHERE symbol = 'IndiBourbon';
UPDATE commodities
SET commodity_ru = 'Индит'
WHERE symbol = 'Indite';
UPDATE commodities
SET commodity_ru = 'Индий'
WHERE symbol = 'Indium';
UPDATE commodities
SET commodity_ru = 'Изолирующая мембрана'
WHERE symbol = 'InsulatingMembrane';
UPDATE commodities
SET commodity_ru = 'Ионный распределитель'
WHERE symbol = 'IonDistributor';
UPDATE commodities
SET commodity_ru = 'Жадеит'
WHERE symbol = 'Jadeite';
UPDATE commodities
SET commodity_ru = 'Дистиллятор Жака Квиненциана'
WHERE symbol = 'JaquesQuinentianStill';
UPDATE commodities
SET commodity_ru = 'Джарадхаррская головоломка'
WHERE symbol = 'JaradharrePuzzlebox';
UPDATE commodities
SET commodity_ru = 'Жаруйский рис'
WHERE symbol = 'JarouaRice';
UPDATE commodities
SET commodity_ru = 'Йотунская моока'
WHERE symbol = 'JotunMookah';
UPDATE commodities
SET commodity_ru = 'Очищающие пиявки с Качиригина'
WHERE symbol = 'KachiriginLeaches';
UPDATE commodities
SET commodity_ru = 'Сигара Kamitra'
WHERE symbol = 'KamitraCigars';
UPDATE commodities
SET commodity_ru = 'Каморинское историческое оружие'
WHERE symbol = 'KamorinHistoricWeapons';
UPDATE commodities
SET commodity_ru = 'Модные вещи от Karetii'
WHERE symbol = 'KaretiiCouture';
UPDATE commodities
SET commodity_ru = 'Саранча с Карсуки'
WHERE symbol = 'KarsukiLocusts';
UPDATE commodities
SET commodity_ru = 'Скрипки Kinago'
WHERE symbol = 'KinagoInstruments';
UPDATE commodities
SET commodity_ru = 'Эль с Кунгги'
WHERE symbol = 'KonggaAle';
UPDATE commodities
SET commodity_ru = 'Корокунгские катышки'
WHERE symbol = 'KorroKungPellets';
UPDATE commodities
SET commodity_ru = 'Кофе Экстракт пустоты'
WHERE symbol = 'LFTVoidExtractCoffee';
UPDATE commodities
SET commodity_ru = 'Гиперсладости LTT'
WHERE symbol = 'LTTHyperSweet';
UPDATE commodities
SET commodity_ru = 'Мины'
WHERE symbol = 'Landmines';
UPDATE commodities
SET commodity_ru = 'Лантан'
WHERE symbol = 'Lanthanum';
UPDATE commodities
SET commodity_ru = 'Большой пакет с данными исследования'
WHERE symbol = 'LargeExplorationDataCash';
UPDATE commodities
SET commodity_ru = 'Лавианский бренди'
WHERE symbol = 'LavianBrandy';
UPDATE commodities
SET commodity_ru = 'Кожа'
WHERE symbol = 'Leather';
UPDATE commodities
SET commodity_ru = 'Леестийский сок зла'
WHERE symbol = 'LeestianEvilJuice';
UPDATE commodities
SET commodity_ru = 'Лепидолит'
WHERE symbol = 'Lepidolite';
UPDATE commodities
SET commodity_ru = 'Жидкий кислород'
WHERE symbol = 'LiquidOxygen';
UPDATE commodities
SET commodity_ru = 'Спиртное'
WHERE symbol = 'Liquor';
UPDATE commodities
SET commodity_ru = 'Литий'
WHERE symbol = 'Lithium';
UPDATE commodities
SET commodity_ru = 'Гидроксид лития'
WHERE symbol = 'LithiumHydroxide';
UPDATE commodities
SET commodity_ru = 'Живые морские черви с Гекаты'
WHERE symbol = 'LiveHecateSeaWorms';
UPDATE commodities
SET commodity_ru = 'Низкотемпературные алмазы'
WHERE symbol = 'LowTemperatureDiamond';
UPDATE commodities
SET commodity_ru = 'Трава лираи'
WHERE symbol = 'LyraeWeed';
UPDATE commodities
SET commodity_ru = 'Мембрана моллюска'
WHERE symbol = 'M3_TissueSample_Membrane';
UPDATE commodities
SET commodity_ru = 'Мицелий моллюска'
WHERE symbol = 'M3_TissueSample_Mycelium';
UPDATE commodities
SET commodity_ru = 'Споры моллюска'
WHERE symbol = 'M3_TissueSample_Spores';
UPDATE commodities
SET commodity_ru = 'Жидкость моллюска'
WHERE symbol = 'M_TissueSample_Fluid';
UPDATE commodities
SET commodity_ru = 'Мозговое вещество моллюска'
WHERE symbol = 'M_TissueSample_Nerves';
UPDATE commodities
SET commodity_ru = 'Мягкие ткани моллюска'
WHERE symbol = 'M_TissueSample_Soft';
UPDATE commodities
SET commodity_ru = 'Спираль магнитного излучателя'
WHERE symbol = 'MagneticEmitterCoil';
UPDATE commodities
SET commodity_ru = 'Морское оборудование'
WHERE symbol = 'MarineSupplies';
UPDATE commodities
SET commodity_ru = 'Мастер-шефы'
WHERE symbol = 'MasterChefs';
UPDATE commodities
SET commodity_ru = 'Мечукосский кайфочай'
WHERE symbol = 'MechucosHighTea';
UPDATE commodities
SET commodity_ru = 'Звездная смазка с Медба'
WHERE symbol = 'MedbStarlube';
UPDATE commodities
SET commodity_ru = 'Диагностическое медоборудование'
WHERE symbol = 'MedicalDiagnosticEquipment';
UPDATE commodities
SET commodity_ru = 'Метасплавы'
WHERE symbol = 'MetaAlloys';
UPDATE commodities
SET commodity_ru = 'Клатрат метана'
WHERE symbol = 'MethaneClathrate';
UPDATE commodities
SET commodity_ru = 'Кристаллы моногидрата метанола'
WHERE symbol = 'MethanolMonohydrateCrystals';
UPDATE commodities
SET commodity_ru = 'Микроконтроллеры'
WHERE symbol = 'MicroControllers';
UPDATE commodities
SET commodity_ru = 'Ткани военного класса'
WHERE symbol = 'MilitaryGradeFabrics';
UPDATE commodities
SET commodity_ru = 'Разведданные'
WHERE symbol = 'MilitaryIntelligence';
UPDATE commodities
SET commodity_ru = 'Экстракторы минералов'
WHERE symbol = 'MineralExtractors';
UPDATE commodities
SET commodity_ru = 'Нефтепродукты'
WHERE symbol = 'MineralOil';
UPDATE commodities
SET commodity_ru = 'Модульные терминалы'
WHERE symbol = 'ModularTerminals';
UPDATE commodities
SET commodity_ru = 'Муассанит'
WHERE symbol = 'Moissanite';
UPDATE commodities
SET commodity_ru = 'Мокоджингское зверское яство'
WHERE symbol = 'MokojingBeastFeast';
UPDATE commodities
SET commodity_ru = 'Момусовский болотный спаниель'
WHERE symbol = 'MomusBogSpaniel';
UPDATE commodities
SET commodity_ru = 'Монацит'
WHERE symbol = 'Monazite';
UPDATE commodities
SET commodity_ru = 'Мотронское желе'
WHERE symbol = 'MotronaExperienceJelly';
UPDATE commodities
SET commodity_ru = 'Мюонное видеоустройство'
WHERE symbol = 'MuTomImager';
UPDATE commodities
SET commodity_ru = 'Мукусубские хитинос'
WHERE symbol = 'MukusubiiChitinOs';
UPDATE commodities
SET commodity_ru = 'Гигатские грибы с Мулачи'
WHERE symbol = 'MulachiGiantFungus';
UPDATE commodities
SET commodity_ru = 'Мусгравит'
WHERE symbol = 'Musgravite';
UPDATE commodities
SET commodity_ru = 'Таинственный идол'
WHERE symbol = 'MysteriousIdol';
UPDATE commodities
SET commodity_ru = 'Нанопрерыватели'
WHERE symbol = 'Nanobreakers';
UPDATE commodities
SET commodity_ru = 'Нанолекарства'
WHERE symbol = 'Nanomedicines';
UPDATE commodities
SET commodity_ru = 'Натуральная ткань'
WHERE symbol = 'NaturalFabrics';
UPDATE commodities
SET commodity_ru = 'Высокотехнологичная изоляция'
WHERE symbol = 'NeofabricInsulation';
UPDATE commodities
SET commodity_ru = 'Нерутские ягоды'
WHERE symbol = 'NeritusBerries';
UPDATE commodities
SET commodity_ru = 'Агенты нервно-паралитического действия'
WHERE symbol = 'NerveAgents';
UPDATE commodities
SET commodity_ru = 'Нгадандарийские огненные опалы'
WHERE symbol = 'NgadandariFireOpals';
UPDATE commodities
SET commodity_ru = 'Современные древности с Нгуны'
WHERE symbol = 'NgunaModernAntiques';
UPDATE commodities
SET commodity_ru = 'Седла Njangari'
WHERE symbol = 'NjangariSaddles';
UPDATE commodities
SET commodity_ru = 'Неевклидовы экзобаки'
WHERE symbol = 'NonEuclidianExotanks';
UPDATE commodities
SET commodity_ru = 'Нелетальное оружие'
WHERE symbol = 'NonLethalWeapons';
UPDATE commodities
SET commodity_ru = 'Спасательная капсула с пассажиром'
WHERE symbol = 'OccupiedCryoPod';
UPDATE commodities
SET commodity_ru = 'Перчик чили с Очоенга'
WHERE symbol = 'OchoengChillies';
UPDATE commodities
SET commodity_ru = 'Луковая головка'
WHERE symbol = 'OnionHead';
UPDATE commodities
SET commodity_ru = 'Луковая головка, сорт альфа'
WHERE symbol = 'OnionHeadA';
UPDATE commodities
SET commodity_ru = 'Луковая головка, сорт бета'
WHERE symbol = 'OnionHeadB';
UPDATE commodities
SET commodity_ru = 'Лукхеда гамма -штамм'
WHERE symbol = 'OnionHeadC';
UPDATE commodities
SET commodity_ru = 'Опал бездны'
WHERE symbol = 'Opal';
UPDATE commodities
SET commodity_ru = 'Артефакты офиух экзино'
WHERE symbol = 'OphiuchiExinoArtefacts';
UPDATE commodities
SET commodity_ru = 'Оррерское жуткое пойло'
WHERE symbol = 'OrrerianViciousBrew';
UPDATE commodities
SET commodity_ru = 'Осмий'
WHERE symbol = 'Osmium';
UPDATE commodities
SET commodity_ru = 'Аномальные частицы'
WHERE symbol = 'P_ParticulateSample';
UPDATE commodities
SET commodity_ru = 'Пейнит'
WHERE symbol = 'Painite';
UPDATE commodities
SET commodity_ru = 'Палладий'
WHERE symbol = 'Palladium';
UPDATE commodities
SET commodity_ru = 'Пантаские молитвенные палочки'
WHERE symbol = 'PantaaPrayerSticks';
UPDATE commodities
SET commodity_ru = 'Павлинские ухочерви'
WHERE symbol = 'PavonisEarGrubs';
UPDATE commodities
SET commodity_ru = 'Стимуляторы'
WHERE symbol = 'PerformanceEnhancers';
UPDATE commodities
SET commodity_ru = 'Личные вещи'
WHERE symbol = 'PersonalEffects';
UPDATE commodities
SET commodity_ru = 'Персональные подарки'
WHERE symbol = 'PersonalGifts';
UPDATE commodities
SET commodity_ru = 'Личное оружие'
WHERE symbol = 'PersonalWeapons';
UPDATE commodities
SET commodity_ru = 'Пестициды'
WHERE symbol = 'Pesticides';
UPDATE commodities
SET commodity_ru = 'Платина'
WHERE symbol = 'Platinum';
UPDATE commodities
SET commodity_ru = 'Платиновый сплав'
WHERE symbol = 'PlatinumAloy';
UPDATE commodities
SET commodity_ru = 'Политзаключенные'
WHERE symbol = 'PoliticalPrisoner';
UPDATE commodities
SET commodity_ru = 'Полимеры'
WHERE symbol = 'Polymers';
UPDATE commodities
SET commodity_ru = 'Преобразователь энергии'
WHERE symbol = 'PowerConverter';
UPDATE commodities
SET commodity_ru = 'Электрогенераторы'
WHERE symbol = 'PowerGenerators';
UPDATE commodities
SET commodity_ru = 'Электросеть в сборе'
WHERE symbol = 'PowerGridAssembly';
UPDATE commodities
SET commodity_ru = 'Энергообменная шина'
WHERE symbol = 'PowerTransferConduits';
UPDATE commodities
SET commodity_ru = 'Празеодим'
WHERE symbol = 'Praseodymium';
UPDATE commodities
SET commodity_ru = 'Драгоценные камни'
WHERE symbol = 'PreciousGems';
UPDATE commodities
SET commodity_ru = 'Прогениторные клетки'
WHERE symbol = 'ProgenitorCells';
UPDATE commodities
SET commodity_ru = 'Запретные материалы исследований'
WHERE symbol = 'ProhibitedResearchMaterials';
UPDATE commodities
SET commodity_ru = 'Пирофиллит'
WHERE symbol = 'Pyrophyllite';
UPDATE commodities
SET commodity_ru = 'Отражатель излучения'
WHERE symbol = 'RadiationBaffle';
UPDATE commodities
SET commodity_ru = 'Мультипечи Rajukru'
WHERE symbol = 'RajukruStoves';
UPDATE commodities
SET commodity_ru = 'Змеиные шкуры с Рапа Бао'
WHERE symbol = 'RapaBaoSnakeSkins';
UPDATE commodities
SET commodity_ru = 'Реактивная защита'
WHERE symbol = 'ReactiveArmour';
UPDATE commodities
SET commodity_ru = 'Усиленная монтажная плита'
WHERE symbol = 'ReinforcedMountingPlate';
UPDATE commodities
SET commodity_ru = 'Резонансные сепараторы'
WHERE symbol = 'ResonatingSeparators';
UPDATE commodities
SET commodity_ru = 'Родплумсайт'
WHERE symbol = 'Rhodplumsite';
UPDATE commodities
SET commodity_ru = 'Роботы'
WHERE symbol = 'Robotics';
UPDATE commodities
SET commodity_ru = 'Удобрение Rockforth'
WHERE symbol = 'RockforthFertiliser';
UPDATE commodities
SET commodity_ru = 'Старые папиросы Rusani'
WHERE symbol = 'RusaniOldSmokey';
UPDATE commodities
SET commodity_ru = 'Рутил'
WHERE symbol = 'Rutile';
UPDATE commodities
SET commodity_ru = 'Внешняя ткань семянки'
WHERE symbol = 'S6_TissueSample_Cells';
UPDATE commodities
SET commodity_ru = 'Ткань оболочки семянки'
WHERE symbol = 'S6_TissueSample_Coenosarc';
UPDATE commodities
SET commodity_ru = 'Мезоглея семянки'
WHERE symbol = 'S6_TissueSample_Mesoglea';
UPDATE commodities
SET commodity_ru = 'Ткань семянки'
WHERE symbol = 'S9_TissueSample_Shell';
UPDATE commodities
SET commodity_ru = 'Контейнер «SAP 8 Core»'
WHERE symbol = 'SAP8CoreContainer';
UPDATE commodities
SET commodity_ru = 'Ткань ядра семянки'
WHERE symbol = 'S_TissueSample_Cells';
UPDATE commodities
SET commodity_ru = 'Поверхностная ткань семянки'
WHERE symbol = 'S_TissueSample_Core';
UPDATE commodities
SET commodity_ru = 'Мертвая ткань семянки'
WHERE symbol = 'S_TissueSample_Surface';
UPDATE commodities
SET commodity_ru = 'Самарий'
WHERE symbol = 'Samarium';
UPDATE commodities
SET commodity_ru = 'Декоративное мясо с Касанумы'
WHERE symbol = 'SanumaMEAT';
UPDATE commodities
SET commodity_ru = 'Саксонское вино'
WHERE symbol = 'SaxonWine';
UPDATE commodities
SET commodity_ru = 'Материалы исследования'
WHERE symbol = 'ScientificResearch';
UPDATE commodities
SET commodity_ru = 'Научные образцы'
WHERE symbol = 'ScientificSamples';
UPDATE commodities
SET commodity_ru = 'Утильсырье'
WHERE symbol = 'Scrap';
UPDATE commodities
SET commodity_ru = 'Полупроводники'
WHERE symbol = 'Semiconductors';
UPDATE commodities
SET commodity_ru = 'Серендибит'
WHERE symbol = 'Serendibite';
UPDATE commodities
SET commodity_ru = 'Орхидея Shan Charis'
WHERE symbol = 'ShansCharisOrchid';
UPDATE commodities
SET commodity_ru = 'Серебро'
WHERE symbol = 'Silver';
UPDATE commodities
SET commodity_ru = 'Детали оборон. беспилотников'
WHERE symbol = 'SkimerComponents';
UPDATE commodities
SET commodity_ru = 'Рабы'
WHERE symbol = 'Slaves';
UPDATE commodities
SET commodity_ru = 'Малый пакет с данными исследования'
WHERE symbol = 'SmallExplorationDataCash';
UPDATE commodities
SET commodity_ru = 'Реликты Сунтилла'
WHERE symbol = 'SoontillRelics';
UPDATE commodities
SET commodity_ru = 'Кристаллическое золото системы Sothis'
WHERE symbol = 'SothisCrystallineGold';
UPDATE commodities
SET commodity_ru = 'Следы первопроходцев космоса'
WHERE symbol = 'SpacePioneerRelics';
UPDATE commodities
SET commodity_ru = 'Сталь'
WHERE symbol = 'Steel';
UPDATE commodities
SET commodity_ru = 'Конструкционные регуляторы'
WHERE symbol = 'StructuralRegulators';
UPDATE commodities
SET commodity_ru = 'Сверхпроводники'
WHERE symbol = 'Superconductors';
UPDATE commodities
SET commodity_ru = 'Стабилизаторы поверхности'
WHERE symbol = 'SurfaceStabilisers';
UPDATE commodities
SET commodity_ru = 'Снаряжение для выживания'
WHERE symbol = 'SurvivalEquipment';
UPDATE commodities
SET commodity_ru = 'Синтетическая ткань'
WHERE symbol = 'SyntheticFabrics';
UPDATE commodities
SET commodity_ru = 'Синтетическое мясо'
WHERE symbol = 'SyntheticMeat';
UPDATE commodities
SET commodity_ru = 'Синтетические реагенты'
WHERE symbol = 'SyntheticReagents';
UPDATE commodities
SET commodity_ru = 'Тааффеит'
WHERE symbol = 'Taaffeite';
UPDATE commodities
SET commodity_ru = 'Тактические данные'
WHERE symbol = 'TacticalData';
UPDATE commodities
SET commodity_ru = 'Успокаивающий чай Tanmark'
WHERE symbol = 'TanmarkTranquilTea';
UPDATE commodities
SET commodity_ru = 'Тантал'
WHERE symbol = 'Tantalum';
UPDATE commodities
SET commodity_ru = 'Таракская пряность'
WHERE symbol = 'TarachTorSpice';
UPDATE commodities
SET commodity_ru = 'Колокольчики с Тельца'
WHERE symbol = 'TauriChimes';
UPDATE commodities
SET commodity_ru = 'Чай'
WHERE symbol = 'Tea';
UPDATE commodities
SET commodity_ru = 'Телеметрический комплект'
WHERE symbol = 'TelemetrySuite';
UPDATE commodities
SET commodity_ru = 'Кровяные усилители с Терра матер'
WHERE symbol = 'TerraMaterBloodBores';
UPDATE commodities
SET commodity_ru = 'Системы обогащения почвы'
WHERE symbol = 'TerrainEnrichmentSystems';
UPDATE commodities
SET commodity_ru = 'Таллий'
WHERE symbol = 'Thallium';
UPDATE commodities
SET commodity_ru = 'Фрагменты кости'
WHERE symbol = 'ThargoidBoneFragments';
UPDATE commodities
SET commodity_ru = 'Образец кисты'
WHERE symbol = 'ThargoidCystSpecimen';
UPDATE commodities
SET commodity_ru = 'Образец ткани таргоидского генератора коррозии'
WHERE symbol = 'ThargoidGeneratorTissueSample';
UPDATE commodities
SET commodity_ru = 'Таргоидское «сердце»'
WHERE symbol = 'ThargoidHeart';
UPDATE commodities
SET commodity_ru = 'Образец органа'
WHERE symbol = 'ThargoidOrganSample';
UPDATE commodities
SET commodity_ru = 'Таргоидская капсула для биоматериалов'
WHERE symbol = 'ThargoidPod';
UPDATE commodities
SET commodity_ru = 'Образец тканей таргоида-разведчика'
WHERE symbol = 'ThargoidScoutTissueSample';
UPDATE commodities
SET commodity_ru = 'Образец ткани таргоидского корабля «Циклоп»'
WHERE symbol = 'ThargoidTissueSampleType1';
UPDATE commodities
SET commodity_ru = 'Образец глубокой ткани пасти Титана'
WHERE symbol = 'ThargoidTissueSampleType10a';
UPDATE commodities
SET commodity_ru = 'Образец ткани пасти Титана'
WHERE symbol = 'ThargoidTissueSampleType10b';
UPDATE commodities
SET commodity_ru = 'Неполный образец ткани пасти Титана'
WHERE symbol = 'ThargoidTissueSampleType10c';
UPDATE commodities
SET commodity_ru = 'Образец ткани таргоидского корабля «Василиск»'
WHERE symbol = 'ThargoidTissueSampleType2';
UPDATE commodities
SET commodity_ru = 'Образец ткани таргоидского корабля «Медуза»'
WHERE symbol = 'ThargoidTissueSampleType3';
UPDATE commodities
SET commodity_ru = 'Образец ткани таргоидского корабля «Гидра»'
WHERE symbol = 'ThargoidTissueSampleType4';
UPDATE commodities
SET commodity_ru = 'Образец ткани таргоидского корабля «Орф»'
WHERE symbol = 'ThargoidTissueSampleType5';
UPDATE commodities
SET commodity_ru = 'Образец ткани таргоидского корабля «Глефа»'
WHERE symbol = 'ThargoidTissueSampleType6';
UPDATE commodities
SET commodity_ru = 'Образец ткани таргоидского корабля «Коса»'
WHERE symbol = 'ThargoidTissueSampleType7';
UPDATE commodities
SET commodity_ru = 'Образец глубокой ткани Титана'
WHERE symbol = 'ThargoidTissueSampleType9a';
UPDATE commodities
SET commodity_ru = 'Образец ткани Титана'
WHERE symbol = 'ThargoidTissueSampleType9b';
UPDATE commodities
SET commodity_ru = 'Неполный образец ткани Титана'
WHERE symbol = 'ThargoidTissueSampleType9c';
UPDATE commodities
SET commodity_ru = 'Компонент двигателя титана'
WHERE symbol = 'ThargoidTitanDriveComponent';
UPDATE commodities
SET commodity_ru = 'Кружка Hutton'
WHERE symbol = 'TheHuttonMug';
UPDATE commodities
SET commodity_ru = 'Термальные охладители'
WHERE symbol = 'ThermalCoolingUnits';
UPDATE commodities
SET commodity_ru = 'Торий'
WHERE symbol = 'Thorium';
UPDATE commodities
SET commodity_ru = 'Трутисские сливки'
WHERE symbol = 'ThrutisCream';
UPDATE commodities
SET commodity_ru = 'Тигфрайский синтешелк'
WHERE symbol = 'TiegfriesSynthSilk';
UPDATE commodities
SET commodity_ru = 'Мемориальная капсула'
WHERE symbol = 'TimeCapsule';
UPDATE commodities
SET commodity_ru = 'Устройства Мусор-в-пасту Tiolce'
WHERE symbol = 'TiolceWaste2PasteUnits';
UPDATE commodities
SET commodity_ru = 'Титан'
WHERE symbol = 'Titanium';
UPDATE commodities
SET commodity_ru = 'Табак'
WHERE symbol = 'Tobacco';
UPDATE commodities
SET commodity_ru = 'Токсанджийские вирициды'
WHERE symbol = 'ToxandjiVirocide';
UPDATE commodities
SET commodity_ru = 'Токсичные отходы'
WHERE symbol = 'ToxicWaste';
UPDATE commodities
SET commodity_ru = 'Луканская луковая головка'
WHERE symbol = 'TransgenicOnionHead';
UPDATE commodities
SET commodity_ru = 'Безделушки таинственной Фортуны'
WHERE symbol = 'TrinketsOfFortune';
UPDATE commodities
SET commodity_ru = 'Тритий'
WHERE symbol = 'Tritium';
UPDATE commodities
SET commodity_ru = 'Древний артефакт'
WHERE symbol = 'USSCargoAncientArtefact';
UPDATE commodities
SET commodity_ru = 'Черный ящик'
WHERE symbol = 'USSCargoBlackBox';
UPDATE commodities
SET commodity_ru = 'Экспериментальные химикаты'
WHERE symbol = 'USSCargoExperimentalChemicals';
UPDATE commodities
SET commodity_ru = 'Военные планы'
WHERE symbol = 'USSCargoMilitaryPlans';
UPDATE commodities
SET commodity_ru = 'Экспериментальная техника'
WHERE symbol = 'USSCargoPrototypeTech';
UPDATE commodities
SET commodity_ru = 'Редкие произведения искусства'
WHERE symbol = 'USSCargoRareArtwork';
UPDATE commodities
SET commodity_ru = 'Переговоры повстанцев'
WHERE symbol = 'USSCargoRebelTransmissions';
UPDATE commodities
SET commodity_ru = 'Промышленные чертежи'
WHERE symbol = 'USSCargoTechnicalBlueprints';
UPDATE commodities
SET commodity_ru = 'Торговая информация'
WHERE symbol = 'USSCargoTradeData';
UPDATE commodities
SET commodity_ru = 'Таргоидский сенсор'
WHERE symbol = 'UnknownArtifact';
UPDATE commodities
SET commodity_ru = 'Таргоидский зонд'
WHERE symbol = 'UnknownArtifact2';
UPDATE commodities
SET commodity_ru = 'Таргоидское звено'
WHERE symbol = 'UnknownArtifact3';
UPDATE commodities
SET commodity_ru = 'Таргоидская биомасса'
WHERE symbol = 'UnknownBiologicalMatter';
UPDATE commodities
SET commodity_ru = 'Неочищенный минерал со шпилей'
WHERE symbol = 'UnknownMineral';
UPDATE commodities
SET commodity_ru = 'Полуочищенный минерал со шпилей'
WHERE symbol = 'UnknownRefinedMineral';
UPDATE commodities
SET commodity_ru = 'Таргоидская смола'
WHERE symbol = 'UnknownResin';
UPDATE commodities
SET commodity_ru = 'Остатки защитной мембраны'
WHERE symbol = 'UnknownSack';
UPDATE commodities
SET commodity_ru = 'Образцы таргоидских технологий'
WHERE symbol = 'UnknownTechnologySamples';
UPDATE commodities
SET commodity_ru = 'Пустая спасательная капсула'
WHERE symbol = 'UnocuppiedEscapePod';
UPDATE commodities
SET commodity_ru = 'Нестабильное ядро данных'
WHERE symbol = 'UnstableDataCore';
UPDATE commodities
SET commodity_ru = 'Уранинит'
WHERE symbol = 'Uraninite';
UPDATE commodities
SET commodity_ru = 'Уран'
WHERE symbol = 'Uranium';
UPDATE commodities
SET commodity_ru = 'Ушжаанский росток дерева'
WHERE symbol = 'UszaianTreeGrub';
UPDATE commodities
SET commodity_ru = 'Миллениальные яйца с Утгарора'
WHERE symbol = 'UtgaroarMillenialEggs';
UPDATE commodities
SET commodity_ru = 'Узумокские крылья для малой гравитации'
WHERE symbol = 'UzumokuLowGWings';
UPDATE commodities
SET commodity_ru = 'Скраб с V Геркулеса'
WHERE symbol = 'VHerculisBodyRub';
UPDATE commodities
SET commodity_ru = 'Мех носорогов с Ванайеку'
WHERE symbol = 'VanayequiRhinoFur';
UPDATE commodities
SET commodity_ru = 'Водоросли для похудения с Веги'
WHERE symbol = 'VegaSlimWeed';
UPDATE commodities
SET commodity_ru = 'Видавантийский шнурок'
WHERE symbol = 'VidavantianLace';
UPDATE commodities
SET commodity_ru = 'Дроны Volkhab Bee'
WHERE symbol = 'VolkhabBeeDrones';
UPDATE commodities
SET commodity_ru = 'Вода'
WHERE symbol = 'Water';
UPDATE commodities
SET commodity_ru = 'Водоочистители'
WHERE symbol = 'WaterPurifiers';
UPDATE commodities
SET commodity_ru = 'Воды Шинрарты'
WHERE symbol = 'WatersOfShintara';
UPDATE commodities
SET commodity_ru = 'Уимитские пшеничные галеты'
WHERE symbol = 'WheemeteWheatCakes';
UPDATE commodities
SET commodity_ru = 'Вино'
WHERE symbol = 'Wine';
UPDATE commodities
SET commodity_ru = 'Мясо кобе с Вичхола'
WHERE symbol = 'WitchhaulKobeBeef';
UPDATE commodities
SET commodity_ru = 'Волчья дурь'
WHERE symbol = 'Wolf1301Fesh';
UPDATE commodities
SET commodity_ru = 'Обломки кораблекрушений'
WHERE symbol = 'WreckageComponents';
UPDATE commodities
SET commodity_ru = 'Гипербольные системы вульпа'
WHERE symbol = 'WulpaHyperboreSystems';
UPDATE commodities
SET commodity_ru = 'Пиво Ку с Вутхиело'
WHERE symbol = 'WuthieloKuFroth';
UPDATE commodities
SET commodity_ru = 'Биоморфные спутники Xihe'
WHERE symbol = 'XiheCompanions';
UPDATE commodities
SET commodity_ru = 'Лист ясо Конди'
WHERE symbol = 'YasoKondiLeaf';
UPDATE commodities
SET commodity_ru = 'Зесский муравьиный клей'
WHERE symbol = 'ZeesszeAntGlue';
UPDATE commodities
SET commodity_ru = 'Агитационные материалы Айслинг'
WHERE symbol IS NULL
  AND commodity = 'Aisling Media Materials';
UPDATE commodities
SET commodity_ru = 'Политическая программа Айслинг'
WHERE symbol IS NULL
  AND commodity = 'Aisling Programme Materials';
UPDATE commodities
SET commodity_ru = 'Запечатанные контракты Айслинг'
WHERE symbol IS NULL
  AND commodity = 'Aisling Sealed Contracts';
UPDATE commodities
SET commodity_ru = 'Законодательные договоры Альянса'
WHERE symbol IS NULL
  AND commodity = 'Alliance Legislative Contracts';
UPDATE commodities
SET commodity_ru = 'Законодательные отчеты Альянса'
WHERE symbol IS NULL
  AND commodity = 'Alliance Legislative Records';
UPDATE commodities
SET commodity_ru = 'Торговые соглашения Альянса'
WHERE symbol IS NULL
  AND commodity = 'Alliance Trade Agreements';
UPDATE commodities
SET commodity_ru = 'Секретный груз Core'
WHERE symbol IS NULL
  AND commodity = 'Core Restricted Package';
UPDATE commodities
SET commodity_ru = 'Разведданные Грома'
WHERE symbol IS NULL
  AND commodity = 'Grom Counter Intelligence';
UPDATE commodities
SET commodity_ru = 'Подпольная поддержка Грома'
WHERE symbol IS NULL
  AND commodity = 'Grom Underground Support';
UPDATE commodities
SET commodity_ru = 'Гарнизонные припасы Хадсона'
WHERE symbol IS NULL
  AND commodity = 'Hudson Garrison Supplies';
UPDATE commodities
SET commodity_ru = 'Полевые припасы Хадсона'
WHERE symbol IS NULL
  AND commodity = 'Hudson’s Field Supplies';
UPDATE commodities
SET commodity_ru = 'Секретные разведданные Хадсона'
WHERE symbol IS NULL
  AND commodity = 'Hudson’s Restricted Intel';
UPDATE commodities
SET commodity_ru = 'Гуманитарная помощь Кейн'
WHERE symbol IS NULL
  AND commodity = 'Kaine Aid Supplies';
UPDATE commodities
SET commodity_ru = 'Лоббистские материалы Кейн'
WHERE symbol IS NULL
  AND commodity = 'Kaine Lobbying Materials';
UPDATE commodities
SET commodity_ru = 'Дезинформация Кейн'
WHERE symbol IS NULL
  AND commodity = 'Kaine Misinformation';
UPDATE commodities
SET commodity_ru = 'Контрабанда Kumo'
WHERE symbol IS NULL
  AND commodity = 'Kumo Contraband Package';
UPDATE commodities
SET commodity_ru = 'Отчеты Лавиньи о коррупции'
WHERE symbol IS NULL
  AND commodity = 'Lavigny Corruption Reports';
UPDATE commodities
SET commodity_ru = 'Полевые припасы Лавиньи'
WHERE symbol IS NULL
  AND commodity = 'Lavigny Field Supplies';
UPDATE commodities
SET commodity_ru = 'Гарнизонные припасы Лавиньи'
WHERE symbol IS NULL
  AND commodity = 'Lavigny Garrison Supplies';
UPDATE commodities
SET commodity_ru = 'Оперативный доклад Лавиньи'
WHERE symbol IS NULL
  AND commodity = 'Lavigny Strategic Reports';
UPDATE commodities
SET commodity_ru = 'Либеральные субсидии Федерации'
WHERE symbol IS NULL
  AND commodity = 'Liberal Federal Aid';
UPDATE commodities
SET commodity_ru = 'Либеральный груз Федерации'
WHERE symbol IS NULL
  AND commodity = 'Liberal Federal Packages';
UPDATE commodities
SET commodity_ru = 'Либеральная пропаганда'
WHERE symbol IS NULL
  AND commodity = 'Liberal Propaganda';
UPDATE commodities
SET commodity_ru = 'Помеченное оружие'
WHERE symbol IS NULL
  AND commodity = 'Marked Military Arms';
UPDATE commodities
SET commodity_ru = 'Помеченные рабы'
WHERE symbol IS NULL
  AND commodity = 'Marked Slaves';
UPDATE commodities
SET commodity_ru = 'Компоненты луковых головок'
WHERE symbol IS NULL
  AND commodity = 'Onionhead Derivatives';
UPDATE commodities
SET commodity_ru = 'Образцы луковых головок'
WHERE symbol IS NULL
  AND commodity = 'Onionhead Samples';
UPDATE commodities
SET commodity_ru = 'Устаревшие товары'
WHERE symbol IS NULL
  AND commodity = 'Out Of Date Goods';
UPDATE commodities
SET commodity_ru = 'Полевые припасы Патреуса'
WHERE symbol IS NULL
  AND commodity = 'Patreus Field Supplies';
UPDATE commodities
SET commodity_ru = 'Гарнизонные припасы Патреуса'
WHERE symbol IS NULL
  AND commodity = 'Patreus Garrison Supplies';
UPDATE commodities
SET commodity_ru = 'Припасы революционеров'
WHERE symbol IS NULL
  AND commodity = 'Revolutionary supplies';
UPDATE commodities
SET commodity_ru = 'Корпоративные контракты Sirius'
WHERE symbol IS NULL
  AND commodity = 'Sirius Corporate Contracts';
UPDATE commodities
SET commodity_ru = 'Посылка с товаром Sirius'
WHERE symbol IS NULL
  AND commodity = 'Sirius Franchise Package';
UPDATE commodities
SET commodity_ru = 'Промышленное оборудование Sirius'
WHERE symbol IS NULL
  AND commodity = 'Sirius Industrial Equipment';
UPDATE commodities
SET commodity_ru = 'Таргоидская капсула для биоматериалов'
WHERE symbol IS NULL
  AND commodity = 'Thargoid Bio-storage Capsule';
UPDATE commodities
SET commodity_ru = 'Договора Торвал'
WHERE symbol IS NULL
  AND commodity = 'Torval Deeds';
UPDATE commodities
SET commodity_ru = 'Политические заключенные Торвал'
WHERE symbol IS NULL
  AND commodity = 'Torval Political Prisoners';
UPDATE commodities
SET commodity_ru = 'Торговые соглашения Торвал'
WHERE symbol IS NULL
  AND commodity = 'Torval Trade Agreements';
UPDATE commodities
SET commodity_ru = 'Неизвестный'
WHERE symbol IS NULL
  AND commodity = 'Unknown';
UPDATE commodities
SET commodity_ru = 'Военные припасы без меток'
WHERE symbol IS NULL
  AND commodity = 'Unmarked Military supplies';
UPDATE commodities
SET commodity_ru = 'Диссиденты Утопии'
WHERE symbol IS NULL
  AND commodity = 'Utopian Dissident';
UPDATE commodities
SET commodity_ru = 'Публикации Утопии'
WHERE symbol IS NULL
  AND commodity = 'Utopian Publicity';
UPDATE commodities
SET commodity_ru = 'Припасы Утопии'
WHERE symbol IS NULL
  AND commodity = 'Utopian Supplies';
UPDATE commodities
SET commodity_ru = 'Военные припасы Юрия Грома'
WHERE symbol IS NULL
  AND commodity = 'Yuri Grom’s Military Supplies';

UPDATE sub_system
SET label_ru = 'Контроллер дрона-сборщика'
WHERE machine_key = 'dronecontrol_collection';
UPDATE sub_system
SET label_ru = 'Контроллер дронов'
WHERE machine_key = 'dronecontrol_resourcesiphon';
UPDATE sub_system
SET label_ru = 'Пучковый лазер'
WHERE machine_key = 'hpt_beamlaser';
UPDATE sub_system
SET label_ru = 'Орудие'
WHERE machine_key = 'hpt_cannon';
UPDATE sub_system
SET label_ru = 'Сканер груза'
WHERE machine_key = 'hpt_cargoscanner';
UPDATE sub_system
SET label_ru = 'Радиоэлектронное подавление'
WHERE machine_key = 'hpt_electroniccountermeasure';
UPDATE sub_system
SET label_ru = 'Теплоотводная катапульта'
WHERE machine_key = 'hpt_heatsinklauncher';
UPDATE sub_system
SET label_ru = 'Минирующее устройство'
WHERE machine_key = 'hpt_minelauncher';
UPDATE sub_system
SET label_ru = 'Проходочный лазер'
WHERE machine_key = 'hpt_mininglaser';
UPDATE sub_system
SET label_ru = 'Турель точечной обороны'
WHERE machine_key = 'hpt_plasmapointdefence';
UPDATE sub_system
SET label_ru = 'Усилитель щита'
WHERE machine_key = 'hpt_shieldbooster';
UPDATE sub_system
SET label_ru = 'Двигатель FSD'
WHERE machine_key = 'int_hyperdrive';
UPDATE sub_system
SET label_ru = 'Система жизнеобеспечения'
WHERE machine_key = 'int_lifesupport';
UPDATE sub_system
SET label_ru = 'Распределитель питания'
WHERE machine_key = 'int_powerdistributor';
UPDATE sub_system
SET label_ru = 'Силовая установка'
WHERE machine_key = 'int_powerplant';
UPDATE sub_system
SET label_ru = 'Очиститель'
WHERE machine_key = 'int_refinery';
UPDATE sub_system
SET label_ru = 'Щитонакопитель'
WHERE machine_key = 'int_shieldcellbank';
UPDATE sub_system
SET label_ru = 'Щитогенератор'
WHERE machine_key = 'int_shieldgenerator';
UPDATE sub_system
SET label_ru = 'Блок ракет'
WHERE machine_key = 'missilerack';
UPDATE sub_system
SET label_ru = 'Люк грузового ковша'
WHERE machine_key = 'modularcargobaydoor';
