# ED_HUD_REFERENCE.md

Справочник по визуальному языку HUD **Elite Dangerous** (ванильный Horizons/Odyssey)
для проекта **EliteIntel**. Единый источник правды по дизайну HUD-компонентов;
словесная спецификация языка, а не копия игрового арта.

> **Кто это читает.** Ты — агент (Claude Code) в репозитории EliteIntel. Этот файл —
> канон дизайна HUD; перед любой правкой, затрагивающей UI, сверяй её с нужным разделом.

> **Что хранит этот файл.** ТОЛЬКО дизайн: цветовые смыслы, поведение состояний
> (выбор/hover/disabled/статус), выравнивание, типографика, когда рамка-акцент а когда
> плоско, какой компонент под задачу. Hex-значения, px и сигнатуры методов — в классах слоя
> темы (`HudPalette`, `HudGlyphs`, `HudForms`, `AppTheme`) и исходниках. Имена констант и
> компонентов — якоря дизайн→код.

> **Как пользоваться.** Цвета — словесно (оранжевый, зелёный…), конкретные значения бери
> из `HudPalette` по имени. Если меняешь палитру или компоненты — обнови соответствующий
> раздел этого файла в том же коммите.

## Правила применения

- Стилизация = замена raw Swing на HUD-компоненты слоёв пакета `elite.intel.ui`, не локальная вёрстка.
- Цвета/размеры/шрифты/толщины/иконочные роли — ТОЛЬКО из `HudPalette` по имени константы;
  глиф-примитивы — `HudGlyphs`. Хардкод запрещён.
- Цветовой слой в `HudPalette`: raw-цвета называются `HUD_COLOR_<HEX>` и ссылаются только на
  literal-код цвета; роли называются `HUD_COLOR_ROLE_<SEMANTIC_NAME>` и ссылаются напрямую на
  `HUD_COLOR_*`, без role→role цепочек.
- Паттерн, нужный > 1 экрану, — в HUD-слой, не копировать по месту.

**Слой темы (`elite.intel.ui.theme`) — источник правды, разнесён по ролям:**
`HudPalette` — токены (цвета, метрики, роли шрифтов `HUD_FONT_*`); `HudGlyphs` — глиф/иконочные
примитивы (`paintHud*`, `*Icon`, `scaledIcon`/`tintIcon`/`dimIcon`); `HudForms` — GridBag-хелперы
форм (`baseGbc`/`addLabel`/`addField`/…); `AppTheme` — фабрики компонентов/рамок, стайлеры,
`hudModalScaffold`, `applyDarkPalette`. Прочие слои `ui`: `widget` (HUD-компоненты),
`screen`/`dialog` (экраны/модалки), `render` (рендереры таблиц), `support`, `controller`,
`telemetry`, `event`, `i18n`.

---

## I. Язык дизайна

## 0. Общие принципы

1. **Тёмный фон, тонкие линии, без объёма.** Плоский стиль («Flat 2.0»). Никаких
   градиентов, теней, скруглённых «капсул». Рамки — тонкие прямые линии.
2. **Цвет несёт смысл.** Оранжевый — основной/рабочий. Состояние выражается СМЕНОЙ
   цвета (зелёный/жёлтый/красный/циан), а не иконкой/заливкой-пилюлей.
3. **Капс + разрядка.** Подписи, заголовки, значения — заглавными; заголовки секций с
   лёгким letter-spacing.
4. **Выделение = инверсия.** Активная/выбранная строка — сплошная яркая заливка, текст
   ТЁМНЫЙ. Главный приём «фокуса» во всех списках/меню.
5. **Значения вправо.** В «ключ→значение» и числовых колонках значение прижато к правому краю.
6. **Приглушение = неактивно.** Disabled — тот же цвет, сильно приглушённый (`HUD_COLOR_ROLE_DISABLED`),
   не серый «из другой палитры». Гаснет и текст, и иконка единым тёплым тоном.

## 1. Цветовое кодирование

Канон ED (радар Odyssey: friendly=green, neutral=blue, alerted=yellow, hostile=red):

- `HUD_COLOR_ROLE_PRIMARY_ACTION` (оранжевый) — норма/рабочий · `HUD_COLOR_ROLE_SUCCESS` (зелёный) — позитив/OK/прибыль ·
  `HUD_COLOR_ROLE_WARNING` (жёлтый) — внимание/штатное ожидание · `HUD_COLOR_ROLE_DANGER` (красный) — опасность/
  провал/hostile · `HUD_COLOR_ROLE_INFORMATION` (синий) — нейтрально-информационное · `HUD_COLOR_ROLE_DISABLED`/`HUD_COLOR_ROLE_SECONDARY_TEXT` — неактивно · `HUD_COLOR_ROLE_READOUT_LABEL` — приглушённая метка в key→value readout/telemetry-блоках (`HudTelemetryBlock`, §7); тот же тон, что `HUD_COLOR_ROLE_SECONDARY_TEXT`, но отдельная семантика — НЕ путать с disabled · `HUD_COLOR_ROLE_CREDITS_TEXT` — баланс кредитов CMDR (`HudCommanderBlock`, §7); тот же тон, что `HUD_COLOR_ROLE_SECONDARY_TEXT`, отдельная семантика.
- Подложки: `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` (плашка строки) · `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND` (hover-состояние) ·
  `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` (фон тела таблицы/зазор, темнее плашки) · `HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND` (тело модалки,
  между `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` и плашкой, §10.1) · `HUD_COLOR_ROLE_MODAL_SCRIM` (вуаль под модалкой, §10.1).

**Правило:** цвет статуса = цвет ТЕКСТА значения (не заливка фона). Заливкой — только
ВЫБОР/АКТИВНОСТЬ строки (§6, §4).

**`StatusBadge.State`** (цвет значения + левой риски в `HudStatusReadout`):
`OK`→`HUD_COLOR_ROLE_SUCCESS`, `STANDBY`→`HUD_COLOR_ROLE_WARNING`, `OFFLINE`→`HUD_COLOR_ROLE_DANGER`, `INFO`→`HUD_COLOR_ROLE_INFORMATION`,
`IDLE`→`HUD_COLOR_ROLE_DISABLED`. `STANDBY` (жёлтый) — штатно ждёт пользователя; «спит/выключено/
не инициализировано» — `IDLE` (§0.6). STT `SLEEPING`, остановленные LLM/TTS — `IDLE`.

**Имя команды в диалоге** (built-in/custom; эталон `CommandDetailsDialog`) — `HUD_COLOR_ROLE_INFORMATION`,
осознанное исключение (имя→`HUD_COLOR_ROLE_PRIMARY_TEXT` по §0.2/§11.1) ради разгрузки оранжевого. НЕ на id/action
key, НЕ на таблицу каталога (§6), НЕ на binding id.

**Контекстный режим панели.** По умолчанию оранжево-циан; красный/синий — только если задача требует.

**Значок-индикатор в статус-ячейке — осознанное исключение.** Канон: статус = цвет текста,
без иконки. Исключение ТОЧЕЧНО: `CONFLICT` в `StatusCellRenderer` (Import-диалог) несёт
оранжевый треугольник `HUD_COLOR_ROLE_PRIMARY_ACTION`+«!» как УСИЛЕНИЕ (текст всё равно `HUD_COLOR_ROLE_WARNING`). `INVALID`/`OK`
— без значка. На другие ячейки не расширять без явного решения.
> **TODO.** Значок — растр (`ImageIcon`/`BufferedImage`). При следующем касании перевести
> на `paintHud*`-глиф (§13).

**Красная заливка слайдера — осознанное исключение.** Канон: красный = `HUD_COLOR_ROLE_DANGER`
(опасность/провал). Исключение ТОЧЕЧНО: активная часть трека `HudSlider` (§4) — насыщенный
красный `HUD_COLOR_ROLE_SLIDER_VALUE_TRACK` как ИНДИКАТОР УРОВНЯ (повторяет ваниль-ED, выбран ради читаемости
на тёплом треке), НЕ сигнал опасности. На другие контролы не расширять.

## 2. Типографика

**Кегль — ТОЛЬКО роли `HUD_FONT_*`, хардкод (`deriveFont`/`new Font`) запрещён.** Начертание
(`BOLD`/`PLAIN`) роль НЕ несёт — ставит сайт через `deriveFont(Font.BOLD, РОЛЬ)`.

**База/ступени** (всё от одной базы): `HUD_FONT_BASE`; `HUD_FONT_XS` < `SM` < `MD` < `LG`.
Значения — в `HudPalette`.

- **XS:** `HUD_FONT_READOUT_KEY` (метка readout/telemetry, версия/метки шапки),
  `HUD_FONT_BADGE_ROLE` (`StatusBadge`), `HUD_FONT_BANNER` (`HudBanner`).
- **SM:** `HUD_FONT_TABLE_HEADER` (заголовок таблицы; компакт — на ступень мельче),
  `HUD_FONT_FIELD_VALUE` (значение полей/metadata), `HUD_FONT_READOUT_VALUE` (значение
  readout, дата/баланс часов), `HUD_FONT_SECTION_TITLE` (`hudSectionLabel`/`hudGroupLabel`),
  `HUD_FONT_TAB_COMPACT` (плотные внутренние вкладки `COMPACT`), `HUD_FONT_BUTTON`, `HUD_FONT_CHECKBOX`.
- **MD:** `HUD_FONT_TABLE_ROW` (строки таблиц), `HUD_FONT_COMMANDER_NAME` (CMDR/SHIP в шапке),
  `HUD_FONT_TAB_SECTION` (вкладки второго уровня `SECTION`).
- **LG:** `HUD_FONT_TAB_MAIN` (`MAIN_NAV`), `HUD_FONT_APP_TITLE` (имя приложения; титул
  шапки диалога §10.1), `HUD_FONT_ICON_BUTTON` (символ-кнопки и глифы ⓘ/«i»/×).
- **Штучный:** `HUD_FONT_CLOCK` (моно, часы `HudCommanderBlock`), `HUD_FONT_STAT_LG`
  (крупные статы `UsageStatsTabPanel`).

**Новая роль, а не деление существующей.** Совпали по кеглю два несвязанных сайта — НЕ
повод делить роль (позже кегли разъедутся). Заводи отдельную роль с тем же значением.

**Относительный кегль (`getSize2D()±N`)** — ТОЛЬКО когда относительность несёт смысл и база —
уже правильная роль: техподпись двухстрочной ячейки, декоративные заголовки. НЕ относить
от LAF-дефолта — переводить на абсолютную роль.

## 3. Токены (spacing, высоты, иконки)

Все значения — в `HudPalette`, хардкод запрещён.

**Отступы и зазоры:** `HUD_GAP` (базовый шаг) · `HUD_DIALOG_BODY_INSET` = `HUD_GAP×2`
(боковой инсет диалога) · `HUD_SEP_W` (щель между зонами checkbox/field) ·
`HUD_PADDING` / `HUD_PADDING_SMALL`.

**Высоты строк и контролов:** `HUD_TABLE_ROW_HEIGHT` / `…_COMPACT`; `HUD_BUTTON_HEIGHT` /
`…_COMPACT`; `HUD_FIELD_HEIGHT`; `HUD_DIALOG_HEADER_HEIGHT` (НЕ путать с `HUD_BUTTON_HEIGHT` —
шапка не кнопка).

**Иконки (`HUD_ICON_*`):** `MAIN` (крупный nav) · `NAV` (средний) · `SMALL` · `TABLE`
(аффорданс в ячейке, меньше высоты строки).

**Рамки:** `HUD_BORDER_THICKNESS` (стандартная) · `HUD_BORDER_THICKNESS_ACCENT` (акцент).
**Каретка набора:** `HUD_CARET_WIDTH`; вертикальное выравнивание — по visual bounds маркера через
`HudGlyphs.paintHudTextCaret`, не по ручному пиксельному сдвигу.

---

## II. Компоненты

## 4. Кнопки и действия

Эталоны: Station Services (шорткаты), Ship Functions (переключатели).

**HudButton** — кнопка-действие. Единый размер в группе: ширина, шрифт `HUD_FONT_BUTTON` bold,
высота `HUD_BUTTON_HEIGHT`.
- **Покой:** `HUD_COLOR_ROLE_PRIMARY_ACTION`-текст на `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`, контур `HUD_COLOR_ROLE_CONTROL_DECORATION`.
- **Pressed:** инверсия `HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT`, только на время нажатия.
- **Hover:** `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND`. **Disabled:** `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` + `HUD_COLOR_ROLE_DISABLED` + контур `HUD_COLOR_ROLE_SECONDARY_BORDER`.

**Переключатель = СМЕНА ТЕКСТА-действия** (что произойдёт: `SLEEP`/`WAKE UP`/`STOP SERVICES`).
Источник истины — внешнее состояние, не `isSelected()`. Остаётся кнопкой-глаголом, не статус-строкой.

**Отображение состояния (не кнопка).** Если нужно показать ON/OFF/RETRACTED — текстом-значением
в правой колонке, НЕ галочкой/слайдером-пилюлей.

**Дискретный числовой степпер `◄ значение ►`** (`HudStepper`). Горизонтальные ЗАЛИВНЫЕ треугольники
у ЛЕВОГО/ПРАВОГО краёв залитой плашки `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND` БЕЗ рамки (как чекбокс §5.2 в OFF),
значение — по центру. Зоны стрелок отделены от значения вертикальными щелями `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` (как зазор у чекбокса §5.2).
Состояния стрелки: покой `HUD_COLOR_ROLE_PRIMARY_ACTION`; hover — лёгкий accent-вош на зоне; нажатие —
полная заливка `HUD_COLOR_ROLE_PRIMARY_ACTION` + инверсия стрелки в `HUD_COLOR_ROLE_SELECTED_TEXT` (как pressed у subtle-кнопки §4); на краю
диапазона стрелка гаснет до `HUD_COLOR_ROLE_DISABLED`. Значение —
текст по центру, БЕЗ свободного ввода (как в игре). НЕ нативный `JSpinner` с вертикальными ▲▼.
Стрелки — примитивы `paintHudArrowLeft`/`paintHudArrowRight` (§13). Якорь: `HudStepper(min, max, step, initial)`,
`getValue()`/`setValue(int)`; в layout — фикс-ширина (`fill=NONE`/`weightx=0`).

**Слайдер-шкала `HudSlider`** (форма ваниль-ED; эталон Options→Audio). Тёплая коричневая
плашка-трек `HUD_COLOR_ROLE_PANEL_SEPARATOR` во всю ширину; приглушённая рейка `HUD_COLOR_ROLE_CONTROL_DECORATION` с
КРАЕВЫМ отступом (`HUD_SLIDER_EDGE_INSET`, не упирается в края); активная часть слева до ручки —
насыщенная красная заливка `HUD_COLOR_ROLE_SLIDER_VALUE_TRACK` (осознанное исключение §1, индикатор уровня),
нарисована ПОВЕРХ высокой вертикальной стартовой риски (0); ручка — круглый диск `HUD_COLOR_ROLE_PRIMARY_ACTION` с
кольцом `HUD_COLOR_ROLE_BUTTON_TEXT`. Значение — над ручкой (`HUD_COLOR_ROLE_PRIMARY_ACTION`, едет с ручкой); легко переключается на
показ только при перетаскивании. Снап к шагу. Disabled — всё гаснет до `HUD_COLOR_ROLE_DISABLED` (§0.6).
Все метрики — токены `HUD_SLIDER_*`, хардкод запрещён. Якорь: `HudSlider(min, max, step, value)`,
`getValue()`/`setValue(int)`/`addChangeListener(ChangeListener)`; в layout — `fill=HORIZONTAL`
(тянется по ширине). НЕ сырой `JSlider`.

**Сегментный метр уровня `HudMicMeter`** (вертикальный LED-VU; эталон — монитор микрофона).
ИНДИКАЦИЯ realtime-уровня, НЕ ввод. Две колонки дискретных сегментов: **LIVE** (горит до текущего
уровня, цвет зоны — `HUD_COLOR_ROLE_DANGER` ниже floor, `HUD_COLOR_ROLE_WARNING` floor→gate, `HUD_COLOR_ROLE_SUCCESS` выше gate) и узкая
**PEAK-trail** (удерживаемый максимум, тусклый `HUD_COLOR_ROLE_DISABLED` со светлой крышкой `HUD_COLOR_ROLE_BUTTON_TEXT`;
`HUD_COLOR_ROLE_DANGER` при клипе = «too hot»). Негорящие сегменты — `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`. Пороги floor/gate —
тонкие подписанные рейки (`HUD_COLOR_ROLE_SECONDARY_TEXT` / `HUD_COLOR_ROLE_INFORMATION`); под колонками — крупное текущее значение в
цвет статуса (`HUD_FONT_STAT_LG`) + строка `LIVE · OPEN/MARGINAL/CLOSED/HOT`. Авто-масштаб по
бегущему пику. Цифровые подписи — на самом контроле (шкала зон слева, PEAK у крышки), без широкого
бокового блока. Данные — `AudioMonitorBus` (off-EDT → volatile-поля + `invokeLater(repaint)`),
регистрация по `addNotify`/`removeNotify`. Метрики — токены `HUD_METER_*`. Якорь: `HudMicMeter`.

**Развилка:**
- ЗНАЧЕНИЕ-индикация (key→value) → `HudStatusReadout` (§7.1);
- ЖИВОЙ уровень/метр (realtime) → `HudMicMeter`;
- ПЕРЕКЛЮЧАТЕЛЬ-кнопка → `HudButton` со сменой текста;
- НАСТРОЙКА в форме → чекбокс §5.2;
- ДИАПАЗОН с видимой позицией на шкале (громкость, скорость) → `HudSlider`;
- немного дискретных значений компактно, без шкалы → `HudStepper` (`◄ значение ►`).

**Компактная квадратная кнопка-picker у поля** (выбор каталога/файла справа от поля). Квадрат
со стороной = высоте СОСЕДНЕГО поля (идёт за полем, НЕ за `HUD_BUTTON_HEIGHT`), узкая. Глиф —
ПРИМИТИВ (§13), не Unicode-текст (символ зависит от шрифта и «ломается»). На primary-заливке
глиф ТЁМНЫЙ (инверсия §0.4), не белый. Боковые text-инсеты кнопки в квадратном режиме обнуляются —
иначе глиф смещается. Добавлять в layout с `fill=NONE`/`weightx=0`, иначе квадрат растянется.

**→ Якоря:** `HudButton(label, boolean primary)` (primary=true — оранжевая заливка, false — контур);
навигационные списки — `HudTabbedPane` (§11) / `HudSection` (§9);
двухстрочные пункты — `HudCommandNameCellRenderer`;
компактный picker — `makeFieldButton(glyph|Icon, fieldHeight)` + `HudButton.setSquareSide`,
глиф ⋮ — `verticalEllipsisIcon` / `paintHudVerticalEllipsis`;
иконка-аффорданс (close ×, save-в-файл) — `HudGlyphButton(painter, restTint, hoverTint, tooltip, onClick)`
(глиф-примитив §13, футпринт `HUD_TABLE_ROW_HEIGHT_COMPACT`, глиф `HUD_ICON_TABLE`; единственный владелец — `HudDialogHeader` и шапка секции его переиспользуют); в шапку секции — через `HudSection.setHeaderActions` (§9);
слайдер-шкала — `HudSlider(min, max, step, value)` (токены `HUD_SLIDER_*`, цвет заливки `HUD_COLOR_ROLE_SLIDER_VALUE_TRACK`);
сегментный метр уровня — `HudMicMeter` (токены `HUD_METER_*`; подписка `AudioMonitorBus`).

## 5. Поля форм

### 5.1 Метка + текстовое поле

Для строк «метка → поле» в формах (эталон: TRADE PROFILE):
- **Метка** — светлая `HUD_COLOR_ROLE_PRIMARY_TEXT`-капс (как в ваниль-ED: метка светлая, значение оранжевое), кегль `HUD_FONT_SM`,
  БЕЗ двоеточия, НЕ микс-кейс. Двоеточие из i18n чистить в бандлах (ВСЕ языки), не в коде.
  Единый стиль — `styleFieldLabel` (один источник для `addLabel` и `hudReadoutLabel`); цвет/кегль меняются
  централизованно в нём.
- **Поле** — `HudTextField`, тёплая рамка `HUD_COLOR_ROLE_CONTROL_DECORATION` (§8), фон `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`. Текст-значение —
  `HUD_COLOR_ROLE_PRIMARY_ACTION` (оранжевое, парно к светлой метке; `styleTextComponent`), одинаково для однострочных и
  многострочных полей; различие только enabled/disabled (`HUD_COLOR_ROLE_DISABLED`). Цвет НЕ зависит от
  editable/read-only. Живая консоль/лог — отдельная роль (`HudLogArea`), не поле.
- **Disabled (§0.6)** — централизованно, без локальных хаков: рамка следит за `isEnabled()` и гаснет до
  `HUD_COLOR_ROLE_DISABLED` (`hudFieldLine` внутри `hudFieldBorder()`/`…WithInfo()`); текст в поле — `disabledTextColor=HUD_COLOR_ROLE_DISABLED`
  (`styleTextComponent`); метка строки (`addLabel`) гаснет вместе с полем до `HUD_COLOR_ROLE_DISABLED`. Группу гасят
  одним `setEnabled(false)` на контролах — каждый рисует свой disabled сам.
- **Info-«i» (опц.)** — зона ВНУТРИ поля справа, отделена щелью `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`. Тинт: покой
  `HUD_COLOR_ROLE_CONTROL_DECORATION`; hover `HUD_COLOR_ROLE_PRIMARY_ACTION`; disabled `HUD_COLOR_ROLE_DISABLED`. Глиф — `paintHudInfoGlyph`.
  Клик открывает справку, не ставит каретку. НЕ отдельной внешней кнопкой и НЕ Unicode-глифом.
- **Picker у края поля** (выбор каталога/файла) — компактная квадратная кнопка, §4.

**Read-only значение в форме.** Развилка:
- короткое скалярное read-only БЕЗ справки → плоский текст `hudReadoutValue` (§7.2), без рамки;
- значение, которому нужна in-field info-«i» или длинный путь со скроллом/выделением →
  `HudTextField` + `setEditable(false)` (рамка = «ограниченная поверхность», не признак ввода);
- компактная «ограниченная поверхность» без справки → `makeMetadataField` (`HudMetadataField`).

Синие подчёркнутые action-«ссылки» — АНТИПАТТЕРН: справку несёт info-«i» внутри контрола.

**Раскладка строки** «метка→поле[→picker/i]» — хелперы `baseGbc` / `addLabel` / `addField` (§ниже),
не сырой `GridBagConstraints` по месту.

**→ Якоря:** `AppTheme.hudReadoutLabel`, `HudTextField.setInfoAction` / `makeTextField(infoAction)`,
`hudFieldBorderWithInfo()`, `HUD_SEP_W`; read-only — `hudReadoutValue` (§7.2) / `makeMetadataField`
(`HudMetadataField`); picker — `makeFieldButton` (§4); раскладка — `HudForms.baseGbc` / `addLabel` / `addField`.

### 5.2 Чекбокс

Чекбокс ED — НЕ LAF-«птичка», а контрол-строка `[маркер | щель | текст]`, состояние несёт
заливка (инверсия §0.4):

- **ВКЛ:** плашка `HUD_COLOR_ROLE_PRIMARY_ACTION`; маркер — бокс-контур + залитый квадрат `HUD_COLOR_ROLE_SELECTED_TEXT`; текст `HUD_COLOR_ROLE_SELECTED_TEXT`-капс.
- **ВЫКЛ:** плашка `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND`; маркер — пустой бокс `HUD_COLOR_ROLE_CONTROL_DECORATION`; текст `HUD_COLOR_ROLE_SECONDARY_TEXT`-капс.
- **Disabled:** плашка `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND`; маркер — пустой бокс `HUD_COLOR_ROLE_DISABLED`; текст `HUD_COLOR_ROLE_DISABLED`.

Щель `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` делит маркер и текст. Маркер — прямые линии, без «птички»/скругления.

**Info-«i» (опц.)** — строка `[маркер | щель | текст | щель | i]`. Тинт по строке: ВЫКЛ
`HUD_COLOR_ROLE_CONTROL_DECORATION`; ВКЛ `HUD_COLOR_ROLE_SELECTED_TEXT`; disabled `HUD_COLOR_ROLE_DISABLED`; hover над зоной `HUD_COLOR_ROLE_PRIMARY_ACTION`. Клик
открывает справку, НЕ переключает. Без справки зона не рисуется.

**→ Якоря:** `HudCheckBox` (высота `HUD_TABLE_ROW_HEIGHT_COMPACT`), `setInfoAction` /
`makeCheckBox(label, sel, infoAction)`, `paintHudInfoGlyph`, `paintHudCheckMarker`, `HUD_SEP_W`.

### 5.3 Combo

Эталон: combo ED (PRIMARY/BORDERLESS) — тёплый тёмный фон, оранжевый текст, плоская ▼
без бокса-кнопки. НЕ нативный LAF.
- **Свёрнутое** — фон `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`, текст `HUD_COLOR_ROLE_PRIMARY_ACTION` (значение оранжевое, как в ваниль-ED; placeholder/
  muted — `HUD_COLOR_ROLE_SECONDARY_TEXT`), рамка `hudFieldBorder()` (`HUD_COLOR_ROLE_CONTROL_DECORATION`). ▼ плоская (`HUD_COLOR_ROLE_PRIMARY_ACTION`; disabled
  `HUD_COLOR_ROLE_DISABLED`) у края, без бокса/сепаратора. Серый сепаратор editor↔▼ — баг FlatLaf, гасится
  глобально `ComboBox.buttonSeparatorWidth=0`.
- **Список (popup)** — подложка `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`; пункты `HUD_COLOR_ROLE_PRIMARY_ACTION`; выбранный — `HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT`;
  рамка тёплая `HUD_COLOR_ROLE_CONTROL_DECORATION`. Отступ — `HUD_COMBO_ITEM_INSET_V/H`, шрифт `HUD_FONT_FIELD_VALUE`.
  Рендер списка — внутренний рендерер фабрики, НЕ внешний `setRenderer`.
- **Combo — поле ВВОДА**: на выбранной строке таблицы остаётся тёплым, не красится `HUD_COLOR_ROLE_PRIMARY_ACTION`.
- **Выделение текста** — тёплое: `ComboBox.selectionBackground=HUD_COLOR_ROLE_PRIMARY_ACTION`,
  `selectionForeground=HUD_COLOR_ROLE_SELECTED_TEXT` (глобально).
- **Disabled** — тёплый приглушённый (§0.6): фон `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`, текст/▼ `HUD_COLOR_ROLE_DISABLED`.

**Единый API.** Combo — ТОЛЬКО через `HudComboBox`, НЕ `new JComboBox` + ручной `styleComboBox`.
Текст элемента через `labelFn`, НЕ внешний `setRenderer`. Три точки входа:
- **Обычный combo** — `new HudComboBox<>(E[]/ComboBoxModel[, labelFn[, mutedWhen]])`.
  `mutedWhen` (`Predicate<E>`) — приглушать до `HUD_COLOR_ROLE_SECONDARY_TEXT` на невыбранной строке (placeholder/none).
  `ComboBoxModel`-конструктор — для динамических моделей (Audio-устройства, биндинги).
- **Editable-пикер с поиском** — `HudComboBox.picker(E[], labelFn, BiPredicate matches)`.
  Инкапсулирован (флаг editable не ставить снаружи). Поведение: пустое поле → полный список;
  ввод → фильтрация по `matches`. НЕ перетирать editor через `setSelectedItem` при фильтрации.
- **Ячейка таблицы** — `HudComboCellEditor<E>`: держит `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`, не инвертируется.

**Идемпотентность.** `styleComboBox` ставит `HudComboBoxUI` ТОЛЬКО если ещё не установлен
(`setUI` пересоздаёт editor → новый `Document`, осиротевает `DocumentListener` фильтра).

**Opt-out editor (§12).** Editor picker'а помечается `HUD_COMBO_EDITOR_LOCKED` после первичной
стилизации — иначе `applyDarkPalette` перетирает его `EmptyBorder` на `hudFieldBorder()`.

> **TODO.** Фильтр-поиск вручную (`DocumentListener` → пересборка модели). Если пикеров
> станет много, рассмотреть GlazedLists `AutoCompleteSupport` — ценой интеграции с HUD-каноном.
> НЕ внедрять без явного решения.

**→ Якоря:** `HudComboBox` (конструкторы `E[]`/`ComboBoxModel` × `[labelFn][, mutedWhen]`;
фабрика `picker(E[], labelFn, matches)`) + `HudComboBoxUI`. ▼ — `paintHudArrowDown`.
Стиль — `styleComboBox` (идемпотентный). Ячейка — `HudComboCellEditor<E>`. Глобально в
`AppView.installDarkDefaults`: `ComboBox.disabled*` → тёплые, `selectionBackground=HUD_COLOR_ROLE_PRIMARY_ACTION`/
`selectionForeground=HUD_COLOR_ROLE_SELECTED_TEXT`, `buttonSeparatorWidth=0`. Токены: `HUD_COMBO_ITEM_INSET_V/H`,
`HUD_PICKER_FIELD_WIDTH/HEIGHT`, opt-out `HUD_COMBO_EDITOR_LOCKED`.

### 5.4 Сегментный селектор (radio-группа)

Взаимоисключающий выбор «один-из» — НЕ круглый LAF-`JRadioButton` (круг ломает §0.1, а как
чекбокс-строка radio неотличим от §5.2). Канон уже умеет «выбери ровно один» — инверсия-заливкой
(§0.4, выбранная строка §6, активная вкладка/пункт §11). Контрол = бар равных сегментов,
разделённых щелью `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` (как маркер↔текст §5.2 / `intercellSpacing` §6), ровно один залит.
Высота `HUD_TABLE_ROW_HEIGHT_COMPACT`, шрифт `HUD_FONT_CHECKBOX` bold-капс — родня чекбоксу.
Палитра 1-в-1 с §5.2:

- **Выбран:** плашка `HUD_COLOR_ROLE_PRIMARY_ACTION`; текст `HUD_COLOR_ROLE_SELECTED_TEXT`.
- **Не выбран:** плашка `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND`; текст `HUD_COLOR_ROLE_SECONDARY_TEXT`.
- **Hover (невыбранный):** текст → `HUD_COLOR_ROLE_PRIMARY_ACTION` (плашка та же).
- **Disabled:** плашка `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND`; текст `HUD_COLOR_ROLE_DISABLED` (§0.6).

Без обводки самого контрола (как слаб чекбокса; рамка-бокс — акцент §9, не дефолт). Программный
`setSelectedIndex` НЕ шлёт `ChangeListener` (как `setSelected` у кнопки) — слушатель только на клик.

**→ Якоря:** `HudSegmentedControl(String[] labels, int selectedIndex)`, `getSelectedIndex()` /
`setSelectedIndex(int)` / `addChangeListener(ChangeListener)`; opt-out `HUD_LOCKED_FOREGROUND` (§12).
Высота `HUD_TABLE_ROW_HEIGHT_COMPACT`, зазор `HUD_SEP_W`, шрифт `HUD_FONT_CHECKBOX`.

## 6. Таблицы

Эталоны: Commodities Market, Sub-Targets.

- **Заголовки колонок** — `HUD_COLOR_ROLE_SECONDARY_TEXT`-капс, тонкая тёплая рейка `HUD_COLOR_ROLE_CONTROL_DECORATION` под
  шапкой (НЕ холодный `HUD_COLOR_ROLE_SECONDARY_BORDER`). Обязательна у ВСЕХ таблиц.
- **Групповые сепараторы** (CHEMICALS/FOODS) — яркий капс без заливки, отдельной строкой.
- **Строки данных** — плашка `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`. НЕ zebra, НЕ grid.
- **Фон тела** — `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` (темнее плашки). Деление — зазор `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` через `intercellSpacing`
  (гориз.+вертик.). НЕ прозрачность — фон задаётся явно.
- **Значения** — капс в РЕНДЕРЕРЕ (`toUpperCase`). Цвет: параметр/настройка → `HUD_COLOR_ROLE_PRIMARY_ACTION`;
  имя-идентификатор → `HUD_COLOR_ROLE_PRIMARY_TEXT`. Статус-данные — по §1.
- **Числовые колонки** — вправо; текстовые — влево.
- **Combo-колонка** — рендерер с приглушённой ▼ (`HUD_COLOR_ROLE_CONTROL_DECORATION`; на выбранной `HUD_COLOR_ROLE_SELECTED_TEXT`);
  редактор — `HudComboBox` (§5.3), всегда тёплый, НЕ инвертируется с выбором строки.
- **Иконка-аффорданс** (шестерёнка) — мелкая, меньше высоты строки. Тинт по строке:
  покой `HUD_COLOR_ROLE_CONTROL_DECORATION`, выбранная `HUD_COLOR_ROLE_SELECTED_TEXT`. Размер — `HUD_ICON_TABLE`.

**Selected row:** заливка `HUD_COLOR_ROLE_PRIMARY_ACTION` + текст `HUD_COLOR_ROLE_SELECTED_TEXT`, ЯВНО в рендерере (FlatLaf перебивает).
Одна за раз. **Disabled row:** `HUD_COLOR_ROLE_DISABLED`.

**→ Якоря:** `HudTable.style()/styleCompact()`, `HudTable.dataPlaneScrollPane()` (§12, не
сырой `JScrollPane`). Высоты `HUD_TABLE_ROW_HEIGHT*`. Combo-ячейки — `DefaultCellEditor` на
`HudComboBox`. Иконки — `HudGlyphs.scaledIcon`+`tintIcon`. Шрифты: `style()`→`HUD_FONT_TABLE_ROW`,
`styleCompact()`→`HUD_FONT_SM`; заголовок `HUD_FONT_TABLE_HEADER`.

## 7. Readout и статус-строки

### 7.1 HudStatusReadout (key→value с состоянием)

Эталоны: Outpost dialog, Ship Functions, Station faction-block.
- **Метка слева** — `HUD_COLOR_ROLE_SECONDARY_TEXT`-капс, без двоеточия (ED разделяет колонкой, не пунктуацией).
- **Значение справа** — прижато ВПРАВО, цвет по §1 (`ON`→норма, `OFF`→приглушён, опасное→красный).
- Тонкая accent-риска слева. Правая колонка ровная.

**→ Якорь:** `HudStatusReadout` (метка `HUD_COLOR_ROLE_SECONDARY_TEXT`, значение справа в цвет `StatusBadge.State`).

### 7.2 Read-only key→value в диалогах деталей

(эталон: `CommandDetailsDialog`) — ОТДЕЛЬНАЯ модель от `HudStatusReadout` (тот — `StatusBadge.State`
и значение ВПРАВО). Две колонки:
- **Метка** — капс без двоеточия (`hudReadoutLabel`).
- **Значение** — плоский текст БЕЗ рамки, левым краем (НЕ вправо). Цвет: имя → `HUD_COLOR_ROLE_INFORMATION` (§1),
  прочее → `HUD_COLOR_ROLE_PRIMARY_ACTION` (оранжевое, как значения полей). НЕ форсить в капс в рендере — если нужно, капсить в источнике.
- **Рамка = признак ВВОДА**: read-only → плоский текст; многострочное/редактируемое
  → area с `hudFieldBorder()` (§5.1).

**→ Якорь:** `AppTheme.hudReadoutValue(value, color)` (плоский `JLabel`, без рамки) —
пара к `hudReadoutLabel`. Один хелпер на все key→value диалоги.

### 7.3 Баннеры и прогресс-полосы

Эталоны: Quick Status, Community Goal tiers, market profit bars.
- Строка-индикатор: метка + значение/состояние в цвет §1.
- Прогресс/уровни — ряд тонких сегментов-делений в цвет-статус (циановые tier-бары).
- Рейтинговые ряды (S/A/B/C/D/E/F) — буква в боксе + строка справа; провал (`INSUFFICIENT`) — красным.

**Баннер-уведомление/подсказка-внизу-панели — ТОЛЬКО `HudBanner`.** Левая accent-рейка + текст
в цвет состояния (`StatusBadge.State`). Caution-хинт — `STANDBY` (жёлтый) с ведущим ⚠-глифом
(3-арг конструктор `leadingWarnGlyph=true`). Ручные warning-полосы (`JLabel`+Unicode «⚠»+`HUD_COLOR_ROLE_WARNING_PANEL_BACKGROUND`)
— АНТИПАТТЕРН: и bindings-хинт, и note «changes take effect» идут через `HudBanner`.

**Длинная подсказка в узкой колонке** — `HudBanner.multiline(text, state)`: текст переносится по
словам (`JTextArea`, пропорциональный шрифт), не клипуется. НЕ городить `<html width=…>`-хак.

**Disabled (§0.6).** `HudBanner` следит за `setEnabled`: рейка и текст гаснут до `HUD_COLOR_ROLE_DISABLED`,
при включении — обратно в цвет состояния. Гасить вместе с неактивной колонкой/секцией.

**→ Якоря:** `HudBanner(text, state[, leadingWarnGlyph])` (одиночные уведомления; ⚠ —
`warningGlyphIcon`/`paintHudWarningGlyph`, §13); `HudBanner.multiline(text, state)` (переносимый);
`HudStatusReadout`; прогресс — сегментированная полоса `HUD_COLOR_ROLE_INFORMATION`/`HUD_COLOR_ROLE_SUCCESS`.

### 7.4 Диалоговый лог

`HudLogArea.chat` ведёт реплики CMDR слева (зелёная рейка) и Vega справа (циановая). Активная
реплика Vega получает непрозрачную рейку и циановую заливку, затухающую от рейки к тексту. Это
осознанное исключение из запрета градиентов §0. После последнего символа печати заливка и рейка
плавно затухают до обычного вида за `HUD_CHAT_ACTIVE_HOLD_MS`.

## 8. Скроллбары

Служебный хром, не носитель смысла. Цвет-статус (в т.ч. циан) НЕ применяется.
- **Thumb** — плоский `fillRect`, тёплый `HUD_COLOR_ROLE_DISABLED`. **Track** — `fillRect` в `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`.
  Hover не «загорается». Кнопки-стрелки убраны, полоса узкая.

> **Тёплый thumb, холодные рамки.** `HUD_COLOR_ROLE_FRAME_BORDER` (холодный) — только рамки кнопок/тулбара,
> для thumb НЕ использовать. **Рамки полей** — тёплый `HUD_COLOR_ROLE_CONTROL_DECORATION`. **Скролл-обёртка
> таблицы — без рамки**: таблица «плавает» на `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`. Обрамление — приём §9 (FRAMED),
> к таблицам по умолчанию не применяется.

**→ Якорь:** `HudScrollPane` → `AppTheme.styleScrollPane()`. Все прокручиваемые области —
`HudScrollPane`, не сырой `JScrollPane`.

---

## III. Паттерны сборки

## 9. Секции: FLAT vs FRAMED

Рамка-бокс — АКЦЕНТ, не дефолт (несколько подряд → «коробка в коробке», шум).
- **FRAMED** (`new HudSection`, `compactCard`) — рамка `HUD_COLOR_ROLE_CONTROL_DECORATION` + заливка заголовка
  `HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND`. Для обособленных виджетов-акцентов: сайдбары, commander-block, карточки.
- **FLAT** (`HudSection.flat`, `compactFlat`) — без рамки/заливки. Заголовок-капс (`HUD_COLOR_ROLE_PRIMARY_ACTION`)
  + тёплая рейка `HUD_COLOR_ROLE_CONTROL_DECORATION`; фон тела ПРОЗРАЧНЫЙ. Для рабочих секций вкладки.

**Правило:** рабочая зона → FLAT; обособленный акцент → FRAMED.

**Внутри модалки — всегда FLAT.** Рамку окна даёт каркас §10.1; FRAMED-секция под ней =
вторая рамка («коробка в коробке») — антипаттерн.

**Две колонки рядом** — `HudTwoColumns(left, right)`: равные половины (`GridLayout 1×2`) +
центральный вертикальный разделитель `HUD_COLOR_ROLE_PANEL_SEPARATOR` (тёплый, тише рейки секции §10.1;
НЕ холодный `HUD_COLOR_ROLE_SECONDARY_BORDER`), нарисован `paintComponent` (палитра не перетирает). Дети заполняют
половину; для верхнего выравнивания контента — обернуть колонку в `BorderLayout` и добавить в `NORTH`.
Эталоны: AI Services (local/cloud setup), `CustomCommandEditorDialog` (identity/steps). НЕ городить
локальный `GridBag`-хак равных колонок по месту.

**Действия в шапке секции** — `HudSection.setHeaderActions(JComponent...)`: одна или несколько иконок-аффордансов
(`HudGlyphButton` §4) в правый край полосы заголовка, напротив тайтла, слева-направо в порядке аргументов
(последний — у правого инсета), для действий над содержимым секции (напр. save + clear у лог-панели). Кладутся в
`GridLayout`-полосу, зазор `HUD_GAP_TIGHT`; ПОЛОСА пиннится к высоте строки заголовка (шапка не растёт), а
`GridLayout` растягивает каждую иконку на эту высоту (глиф центрируется, не обрезается). Правый инсет — общий
`HEADER_H_INSET` (несёт бордер шапки). Действия — однотипные глиф-кнопки (равная ширина ячеек). НЕ верстать
кнопку по месту в теле секции. Эталон: AI-вкладка, шапка «Диагностика».

## 10. Диалоги

Эталоны: Universal Cartographics, Promotion to Master, Community Goal.
- Панель с чёткой рамкой поверх затемнённого фона. Заголовок капсом, с иконкой и линией.
- **Затемнение сцены ОБЯЗАТЕЛЬНО** (§10.1 scrim): без вуали окно сливается с экраном.
- **Кнопки:** Primary → яркая заливка, тёмный текст (`makeButton`); остальные → тусклый
  контур (`makeButtonSubtle`).
- **Раскладка футера:** левый слот — СЛЕВА; primary — СПРАВА; EXTRA — левее primary.
  Раскладку даёт `HudModalSpec` — вручную WEST/EAST не верстать. Прежний канон «primary слева» ОТМЕНЁН.
- **Один футер на всё:** и модалки, и подвалы вкладок собирает `HudFooter.build(modal, …)`.
  Различие ТОЛЬКО в левом слоте: модальный (`modal=true`) — `BACK`/dismiss; не-модальный
  (`modal=false`) — статус/инфо, **`BACK` запрещён** (флаг это и гарантирует).
- **«Unsaved changes» в SAVE-футере — стандартный `HudUnsavedHint`** (`HUD_COLOR_ROLE_WARNING` + ⚠-глиф, скрыт
  по умолчанию, `status.unsavedChanges`), вплотную СЛЕВА от `SAVE` в правой группе; показ/скрытие по
  dirty; `SAVE` гасить при отсутствии правок. НЕ полноширинная плашка-баннер над кнопками.
- **Dismiss — всегда `BACK`** (ключ `button.back`, subtle), не `CLOSE`/`CANCEL`. НЕ primary-заливка.
  Только в модальном футере.
- **default-кнопку** ставит САМ диалог после `setContentPane`. Обычно primary; вправе выбрать
  иную (эталон `CommandDetailsDialog`: default=`BACK`, чтобы Enter не запускал команду).
- **Титульный блок объекта** — в NORTH: имя `HUD_COLOR_ROLE_INFORMATION` bold крупно (`HUD_FONT_APP_TITLE`, капс)
  + id/ключ `HUD_COLOR_ROLE_SECONDARY_TEXT` (`HUD_FONT_READOUT_KEY`) под ним. Дублирование в key→value тела —
  по ситуации (форма, где имя уже в титуле, из key→value его убирает).
- Реплики NPC — «ёлочками».

**Confirm/yes-no — `HudConfirmDialog`, НЕ `JOptionPane`.** Переиспользуемая HUD-модалка на каркасе
§10.1: `HudConfirmDialog.confirm(parent, title, message, primary, dismiss)` (2 кнопки → boolean) или
`HudConfirmDialog.show(parent, title, message, primary, extra, dismiss)` (3 кнопки → `Result`
PRIMARY/EXTRA/DISMISS). ESC и крестик → DISMISS. Сырой `JOptionPane.showConfirmDialog`/`showOptionDialog`
— антипаттерн.

**→ Якоря:** сборка — ТОЛЬКО `AppTheme.hudModalScaffold(HudModalSpec)` (§10.1). Титульный
блок — `AppTheme.commandTitleBlock`. Секции тела — `HudSection.flat` (§9). Confirm — `HudConfirmDialog`.

### 10.1 Каркас диалога (шапка + тело + рамка + футер + scrim)

Системный титлбар ОС нарушает §0.1/§10 → `setUndecorated(true)` + кастомная HUD-шапка.

**Сборка — ТОЛЬКО через единый каркас.** `AppTheme.hudModalScaffold(HudModalSpec)` →
wrapper-`JPanel` для `setContentPane`. Композиция, НЕ базовый класс. `HudDialogHeader` и
`HudFooter`/`hudFooterBorder()` — ВНУТРЕННОСТИ каркаса, напрямую в окнах не верстать.

**`HudModalSpec` (builder):** `title` (nullable → без шапки), `onClose`, `body`, `scrollBody`
(bool → viewport bg `HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND`), кнопки с ролями `primary`/`dismiss`/`extra` (§10).
Каркас НЕ создаёт кнопки — принимает готовые. ESC/default-кнопку ставит окно после `setContentPane`.

**Боковой инсет** — единый токен `HUD_DIALOG_BODY_INSET` (`HUD_GAP×2`). При `scrollBody`
тело-`body` своего бордера НЕ несёт. Литералы 18/16/12 отменены.

**Шапка = холодный якорь над тёплым телом** (отделяется сменой температуры, не яркостью).
- **Фон** — `HUD_COLOR_ROLE_DIALOG_HEADER_BACKGROUND`. НЕ `HUD_COLOR_ROLE_PRIMARY_ACTION`, НЕ тёплые тона.
- **Акцент** — нижняя рейка `HUD_COLOR_ROLE_PRIMARY_ACTION` (`HUD_BORDER_THICKNESS_ACCENT`).
- **Заголовок** — капс bold `HUD_FONT_APP_TITLE`, `HUD_COLOR_ROLE_DIALOG_TITLE_TEXT`.
- **Лого-якорь слева** — `elite-logo`, тинт `HUD_COLOR_ROLE_CONTROL_DECORATION`, `HUD_ICON_NAV`, декоративный.
- **Крестик** — `paintHudCloseGlyph`: покой `HUD_COLOR_ROLE_CONTROL_DECORATION`, hover `HUD_COLOR_ROLE_DANGER`.
- **Высота** — `HUD_DIALOG_HEADER_HEIGHT` (НЕ `HUD_BUTTON_HEIGHT`).

**Тело** — `HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND`: смысловая роль тела модалки; значение может быть alias к базовому фону.
НЕ использовать вместо неё `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`/`HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND` напрямую.

**Рейка-разделитель футера** — `HUD_COLOR_ROLE_PANEL_SEPARATOR` (тише рейки секции `HUD_COLOR_ROLE_CONTROL_DECORATION`
и рейки шапки `HUD_COLOR_ROLE_PRIMARY_ACTION` — три линии разного веса). `hudFooterBorder()`: боковой инсет 0.

**Рамка окна** — `HUD_COLOR_ROLE_PANEL_SEPARATOR`, толщина `HUD_BORDER_THICKNESS_ACCENT`. НЕ `HUD_COLOR_ROLE_PRIMARY_ACTION`
(конкурирует с рейкой шапки), НЕ `HUD_COLOR_ROLE_CONTROL_DECORATION` 1px (сливается по углам). `MatteBorder`
на wrapper каркаса. Drag за шапку; крестик перехватывает свои события.

**Scrim** — вуаль `HUD_COLOR_ROLE_MODAL_SCRIM` на `glassPane` окна-владельца. Ставится перед показом,
снимается при закрытии. Каркас scrim НЕ оркеструет — снаружи через `runWithModalScrim(owner,
showModal)`, owner — `SwingUtilities.getWindowAncestor(parent)`.

> **TODO (переходное рассогласование).** Scrim включён только у `CommandDetailsDialog`;
> остальные 9 модалок — `setVisible(true)` без вуали. Включать РАЗОМ, не по одному.

Внутри окна второй заголовок НЕ дублировать.

**→ Якоря:** `AppTheme.hudModalScaffold(HudModalSpec)` → wrapper-`JPanel` для `setContentPane`.
`HudModalSpec`: роли primary/dismiss/extra, `scrollBody`. Внутренности: `HudModalScaffold.build`;
`HudDialogHeader(title, onClose)` (opt-out `HUD_LOCKED_FOREGROUND`; drag за шапку);
футер — `HudFooter.build(modal, back, status, trailing)` / `hudFooterBorder()`. Scrim снаружи:
`runWithModalScrim(owner, show)`, owner —
`SwingUtilities.getWindowAncestor(parent)`. Особый случай ручного скролла: `SettingsPopup`
отдаёт `hudScrollPane` как `body` со `scrollBody=false`.

## 11. Навбар и вкладки

Эталоны: Ship panel tabs, top nav ED, Station Services.

**Вкладки** — ряд капсом, под рядом тонкая рейка.
- **Активная**: яркий бокс-заливка (SUB-TARGETS) или подчёркивание `HUD_COLOR_ROLE_PRIMARY_ACTION`; неактивные тусклее.
- **`SECTION` (второй уровень, ACTIONS/SETTINGS)**: активная — залитый бокс `HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_BACKGROUND`+`HUD_COLOR_ROLE_SELECTED_TEXT`
  (инверсия §0.4). Бокс доходит до нижней рейки; НЕ подчёркивание — иначе на фоне множества
  секций-рейок полоса табов теряется. Под рядом рейка-подчёркивание `HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_UNDERLINE`, во всю ширину.
  Первый таб — с лёгким левым отступом от начала рейки (`tabAreaInsets.left`).
  `COMPACT` (плотные внутренние) — остаётся подчёркивание.
- Иконочные: активный — сплошная заливка, иконка тёмная.

**Навигационные списки** (Station Services, market sidebar) — пункты на слабой подложке,
тонкие разделители. **Активный — сплошная заливка + тёмный текст** (`HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT`), один
за раз. Заголовки групп — тусклый капс. Иконка слева — монохромная в цвет пункта.
**Двухстрочный пункт:** верх — имя (`HUD_COLOR_ROLE_PRIMARY_TEXT`), низ — техподпись `HUD_COLOR_ROLE_SECONDARY_TEXT` (не красим). На
выбранной — обе → `HUD_COLOR_ROLE_SELECTED_TEXT`.

**→ Якорь:** `HudTabbedPane` уровней `MAIN_NAV` (§11.1) и `SECTION`/`COMPACT`; двухстрочные —
`HudCommandNameCellRenderer`.

### 11.1 App header + MAIN_NAV navbar

Эталоны: top nav ED + инфопанель станции. Канон `TopStatusBar` + `HudTabbedPane(MAIN_NAV)`.

**Шапка (`TopStatusBar`).** Слева — имя приложения капсом (`HUD_COLOR_ROLE_PRIMARY_TEXT`, bold) + версия (`HUD_COLOR_ROLE_SECONDARY_TEXT`).
Справа — пары «метка→значение»: метка (`CMDR`/`SHIP`) `HUD_COLOR_ROLE_SECONDARY_TEXT`-капс без двоеточия; значение
`HUD_COLOR_ROLE_PRIMARY_TEXT`-капс, bold. НЕ циан/`HUD_COLOR_ROLE_PRIMARY_ACTION`: имя — значение, не статус.

**Рейки навбара:** верхняя (шапка↔вкладки) `HUD_COLOR_ROLE_CONTROL_DECORATION`, тоньше; нижняя (навбар↔тело)
`HUD_COLOR_ROLE_PRIMARY_ACTION`, толще. Холодный `HUD_COLOR_ROLE_SECONDARY_BORDER` НЕ применять.

**Активная вкладка = инверсия**: `HUD_COLOR_ROLE_MAIN_TAB_ACTIVE_BACKGROUND`-заливка + `HUD_COLOR_ROLE_SELECTED_TEXT`. Заливка с вертикальным зазором
от рейк. Подчёркивания НЕТ (оно для SECTION/COMPACT §11).

**Неактивные:** текст `HUD_COLOR_ROLE_SECONDARY_TEXT`, иконка `HUD_COLOR_ROLE_CONTROL_DECORATION`. Disabled — `HUD_COLOR_ROLE_DISABLED`.

**→ Якоря:** `TopStatusBar`, `HudTabbedPaneUi`.

---

## IV. Правила

## 12. Palette opt-out

Компонент, несущий свой фон/foreground, помечает себя opt-out client-property — палитра его
пропускает. Так сделаны кнопки, таблицы (`dataPlaneScrollPane()`), шапка (`HUD_LOCKED_FOREGROUND`),
editor picker'а (`HUD_COMBO_EDITOR_LOCKED`; иначе палитра ставит ему `hudFieldBorder()` →
видимая вертикаль у ▼). `styleComboBox` идемпотентен по `setUI` (§5.3). Флаги — в `AppTheme`.

**Поле с info-зоной (§5.1)** — палитра (`styleTextComponent`) обязана сохранять широкий
`hudFieldBorderWithInfo()`, а не сбрасывать на `hudFieldBorder()`: иначе резерв под «i» теряется
и длинный текст налезает на глиф. Определяется через `HudTextField.hasInfoZone()`, не client-property.

## 13. Чек-лист

- Цвета/шрифты/высоты/иконки/толщины рамок — ТОЛЬКО из `HudPalette` по имени. Хардкод запрещён.
  Raw-цвета — только `HUD_COLOR_<HEX>`; роли — только `HUD_COLOR_ROLE_<SEMANTIC_NAME>` как прямой alias на `HUD_COLOR_*`.
- Размер шрифта — ТОЛЬКО роли `HUD_FONT_*` (§2), хардкод-кегль запрещён.
- Размер иконки — роль `HUD_ICON_*` (§3), хардкод-px запрещён.
- Толщина рамки — роль (`HUD_BORDER_THICKNESS` / `HUD_BORDER_THICKNESS_ACCENT`), хардкод запрещён.
- UI-текст — только `MultiLingualTextProvider.getText("key")`, без литералов.
- Выделение строки = `HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT`. Состояние = цвет текста (§1). Disabled =
  приглушение тем же цветом (`HUD_COLOR_ROLE_DISABLED`), не «серый из другой палитры» (§0.6).
- Плоские прямые формы: без пилюль/градиентов/теней. Скроллбары — `fillRect`, без циана.
- Хром и вторичные подписи — `HUD_COLOR_ROLE_SECONDARY_TEXT`/`HUD_COLOR_ROLE_DISABLED`, не цветом-статусом и не холодным
  `HUD_COLOR_ROLE_FRAME_BORDER` (рамки полей — тёплый `HUD_COLOR_ROLE_CONTROL_DECORATION`).
- Таблицы (§6) — плашка `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` на `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`, без zebra/grid; деление — `intercellSpacing`;
  выбор ЯВНО `HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT` в рендерере; hover `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND`; капс/цвет/выравнивание — в рендерере.
- Иконка-аффорданс в ячейке — `HUD_ICON_TABLE`, тинт по строке (`HUD_COLOR_ROLE_CONTROL_DECORATION` покой, `HUD_COLOR_ROLE_SELECTED_TEXT` выбранная).
- Combo (§5.3) — `HudComboBox`: тёплый фон, плоская ▼, тёплый список; НЕ инвертируется с
  выбором строки. Без `HUD_COLOR_ROLE_INFORMATION`/`HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND`.
- Примитивы для >1 места — в `HudGlyphs`: ▼ `paintHudArrowDown`; ▲ `paintHudArrowUp`; ◄ `paintHudArrowLeft`; ► `paintHudArrowRight`;
  «i» `paintHudInfoGlyph`; × `paintHudCloseGlyph`; маркер чекбокса `paintHudCheckMarker`; ⋮ `paintHudVerticalEllipsis`; ⚠ `paintHudWarningGlyph`;
  ⤓ save/download `paintHudSaveGlyph`; 🗑 clear/trash `paintHudTrashGlyph`; каретка `paintHudTextCaret`; тинт `tintIcon`; приглушение альфой `dimIcon`.
  Глифы — примитивами, НЕ `drawString`/Unicode и НЕ растром.
- Info-«i» — ВНУТРИ контрола (§5.2/§5.1) через `setInfoAction`. Синие ссылки — антипаттерн.
- Тултипы (`setToolTipText`) — стиль ГЛОБАЛЬНО через `UIManager` `ToolTip.*` в `AppView.installDarkDefaults`
  (тёмный `HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND` + тёплая рейка `HUD_COLOR_ROLE_CONTROL_DECORATION` `HUD_BORDER_THICKNESS`,
  текст `HUD_COLOR_ROLE_PRIMARY_TEXT`, шрифт `HUD_FONT_TOOLTIP` — иначе наследует крупный `HUD_FONT_UI_DEFAULT`);
  тени попапов сняты `Popup.dropShadowPainted=false` (HUD без теней). Кастомный tooltip по месту — антипаттерн.
- Метка-ключ — `hudReadoutLabel` (`HUD_COLOR_ROLE_SECONDARY_TEXT`-капс без двоеточия). Двоеточие чистить в i18n (ВСЕ языки).
- Read-only key→value (§7.2) — `hudReadoutValue(value, color)`: плоский текст, микс-кейс.
  Рамка-поле — только у вводимого/area.
- Титульный блок диалога (§10) — `commandTitleBlock(name, id)` в NORTH.
- Модальный диалог (§10.1): ТОЛЬКО `AppTheme.hudModalScaffold(HudModalSpec)`. undecorated.
  Dismiss=`BACK` subtle слева, primary справа. Scrim снаружи — `runWithModalScrim` (ЦЕЛЕВОЕ для всех разом).
- Confirm/yes-no/save-discard — `HudConfirmDialog` (§10), НЕ `JOptionPane.showConfirmDialog`/`showOptionDialog`.
- Тёплое оформление вне дефолта палитры защищать opt-out client-property (§12).
- Секции тела модалки — `HudSection.flat` (§9). FRAMED внутри модалки — антипаттерн.
- Панель шорткатов — `HudButton(primary=false)`, переключатель меняет текст-действие (§4).
- On/off в форме — `HudCheckBox` (§5.2), без LAF-«птички».
- Выбор «один-из» — `HudSegmentedControl` (§5.4): сегментный бар, инверсия-заливка выбранного,
  деление `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`. НЕ круглый LAF-`JRadioButton`.
- Диапазон на шкале — `HudSlider` (§4): коричневый трек, красная заливка `HUD_COLOR_ROLE_SLIDER_VALUE_TRACK`
  (исключение §1), круглая ручка `HUD_COLOR_ROLE_PRIMARY_ACTION`+кольцо `HUD_COLOR_ROLE_BUTTON_TEXT`, значение над ручкой; метрики `HUD_SLIDER_*`.
  НЕ сырой `JSlider`.
- Realtime-метр уровня — `HudMicMeter` (§4): сегментный LIVE + PEAK-trail, зоны `HUD_COLOR_ROLE_DANGER`/`HUD_COLOR_ROLE_WARNING`/`HUD_COLOR_ROLE_SUCCESS`,
  пороги-рейки `HUD_COLOR_ROLE_SECONDARY_TEXT`/`HUD_COLOR_ROLE_INFORMATION`, подписи на контроле; метрики `HUD_METER_*`. НЕ хардкод-палитра/шрифты.
- Паттерн для >1 экрана — в HUD-слой.

---

## Приложение: Commander block

Виджет `HudCommanderBlock`. Эталон: инфопанель ED (крупное время, дата, баланс).
- **Часы ED** — `HUD_COLOR_ROLE_PRIMARY_ACTION` моно bold крупно; дата под ним `HUD_COLOR_ROLE_SECONDARY_TEXT` plain (`dd MMM yyyy`,
  месяц-капс), год = реальный **+1286**, время **UTC**.
- **Баланс** — `HUD_COLOR_ROLE_SECONDARY_TEXT` по §7.1, тысячи запятыми + ` CR`. Прятать при ≤ 0.
- **Лого** — приглушён альфой, не спорит с кнопками.
