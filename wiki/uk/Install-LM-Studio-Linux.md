## Локальна LLM — налаштування на Linux (LM Studio)

Запуск локальної LLM зберігає всі дані приватними та офлайн. Плата за підписку відсутня. Застосовуються витрати на обладнання та електроенергію.

Для роботи потрібні [LM Studio](https://lmstudio.ai) та здатний GPU.

---

### Мінімальні вимоги до обладнання

Щоб запустити Elite Dangerous та LLM на **одній машині**, потрібен мінімум **NVIDIA RTX 3060 з 12 GB VRAM**. При такій специфікації запас продуктивності обмежений.

> **Порада:** Elite Intel можна спрямувати на екземпляр LM Studio, що працює на **окремому ПК** у вашій мережі. Якщо доступна друга машина зі здатним GPU, ігровий ПК не несе жодного навантаження від інференсу в цій конфігурації.

---

### Рекомендована модель

| Модель | Потрібно VRAM | Примітки |
|---|---|---|
| `google/gemma-4-e4b` | ~6.3 GB | ✅ Потрібно |

> **Чому саме ця модель?** Вона підтримує виклик функцій, потрібний компаньйону. Модель, яка не вміє формувати
> виклик інструмента, не зможе керувати застосунком, хоч би як добре вона писала.
> Див. [Обрати LLM](installing-local-llms).

---

[[youtube:2HGFmlZGK1g]]

---

### Крок 1 — Встановлення LM Studio

```shell
curl -fsSL https://lmstudio.ai/install.sh | bash
```

Інсталятор розміщує все у `~/.lmstudio/` та додає інструмент CLI `lms`. Після завершення додайте CLI до вашого PATH:

```shell
# Додайте це до ~/.bashrc
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Потім перезавантажте оболонку:

```shell
source ~/.bashrc
```

Перевірте, що все працює:

```shell
lms --help
```

---

### Крок 2 — Завантаження моделі

Запустіть фоновий сервер LM Studio, а потім завантажте модель:

```shell
lms daemon up
lms get google/gemma-4-e4b
```
Якщо ім'я не збігається точно, `lms get` покаже знайдені варіанти — оберіть потрібний
стрілками та клавішею Enter.

Щоб переглянути завантажені моделі:

```shell
lms ls
```

Це стандартний шлях. Однак [LM Studio має відомий баг](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). У деяких випадках завантаження завершується помилкою:
```Error: No staff picks found with the specified search criteria.```

У такому разі завантажте GGUF вручну зі сторінки моделі на HuggingFace та імпортуйте його:

```shell
lms import /path/to/the-model.gguf
```

---

### Крок 3  Запуск сервера

Завантажте модель та запустіть сервер інференсу:

```shell
lms daemon up
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

`--gpu max` передає інференс на GPU для максимальної продуктивності. `lms daemon up` нічого не робить, якщо фоновий сервер уже запущено, тому його можна безпечно виконувати щоразу.

Перевірте, що він запущений:

```shell
curl http://localhost:1234/v1/models
```

Ви отримаєте JSON-список завантажених моделей. Рядок ідентифікатора моделі у цій відповіді  це те, що ви введете у поле **Модель** в Elite Intel.

Щоб зупинити сервер:

```shell
lms server stop
```

> ⚠️ **Важливо:** Сервер LM Studio **не** переживає перезавантажень. Виконуйте три команди з кроку 3 після кожного перезапуску або налаштуйте необов'язковий автозапуск нижче.

---

### Крок 4  (Необов'язково) Автозапуск при завантаженні

Щоб LM Studio запускалася автоматично, налаштуйте її як **користувацьку** службу systemd. Вона працює під вашим власним сеансом, а не як системна служба. Запускається після того, як середовище робочого столу готове. Права адміністратора не потрібні.

Знайдіть ваш ідентифікатор користувача (замініть ім'я користувача на ваше справжнє):
```shell
id -u YOUR_USER_NAME
```

Запам'ятайте це число. Воно знадобиться для подальшого налаштування.

Створіть каталог користувацького systemd, якщо він не існує:

```shell
mkdir -p ~/.config/systemd/user
```

Створіть файл служби:

```shell
nano ~/.config/systemd/user/lmstudio.service
```

Вставте це:

```ini
[Unit]
Description=LM Studio Server
After=network.target

[Service]
Type=oneshot
RemainAfterExit=yes
Environment="HOME=/home/YOUR_USERNAME"
Environment="PATH=/home/YOUR_USERNAME/.lmstudio/bin:/usr/local/bin:/usr/bin:/bin"
Environment="XDG_RUNTIME_DIR=/run/user/YOUR_UID"
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon up
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms load google/gemma-4-e4b --yes --context-length 8192 --gpu max
ExecStart=/home/YOUR_USERNAME/.lmstudio/bin/lms server start --bind 127.0.0.1 --port 1234
ExecStop=/home/YOUR_USERNAME/.lmstudio/bin/lms server stop
ExecStopPost=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon down

[Install]
WantedBy=default.target
```

Замініть `YOUR_USERNAME` на ваше ім'я користувача Linux та `YOUR_UID` на ваш ідентифікатор користувача. Щоб знайти ваш UID:

```shell
id -u
```

> **Гра на іншому ПК?** `--bind 127.0.0.1` робить сервер доступним лише з цього комп'ютера. Якщо Elite Intel працює на іншому ПК, змініть його на `--bind 0.0.0.0`, щоб інші машини у вашій мережі могли до нього під'єднатися.

> ⚠️ **Навіщо `XDG_RUNTIME_DIR`?** Користувацькі служби запускаються у спрощеному середовищі, яке може не містити змінних сеансу. LM Studio використовує `XDG_RUNTIME_DIR` для IPC. Без нього служба може мовчки відмовити, навіть якщо `lms` коректно працює з терміналу. Це найпоширеніша причина збою служби, коли ручний запуск успішний.

Увімкніть та запустіть:

```shell
systemctl --user daemon-reload
systemctl --user enable lmstudio.service
systemctl --user start lmstudio.service
```

Перевірте, що служба запущена:

```shell
systemctl --user status lmstudio.service
curl http://localhost:1234/v1/models
```

> **Усунення несправностей:** Якщо служба зазнає збою, перевірте журнал:
> ```shell
> journalctl --user -xeu lmstudio.service --no-pager | tail -40
> ```
> Якщо повідомляється «Failed to load model», виконайте `lms ls` та переконайтеся, що назва моделі точно збігається з тим, що вказано у файлі служби.

---

### Крок 4b  (Необов'язково) Виправлення повільного інференсу після завантаження

Деякі користувачі відчувають повільні відповіді інференсу, коли LM Studio запускається під час завантаження. Проблема вирішується одразу після ручного перезапуску служби. Причину не встановлено.

Спершу перевірте, що рядок `lms load` у вашому файлі служби закінчується на `--gpu max`. У старіших версіях цієї сторінки його не було. Без нього LM Studio сам вирішує, яку частину моделі розмістити на GPU, і під час завантаження системи може залишити частину моделі на CPU. Якщо ви його додали, виконайте `systemctl --user daemon-reload` і перезавантажтеся для перевірки.

Якщо повільні відповіді все ще з'являються після перезавантаження та зникають після ручного перезапуску, цей таймер автоматизує виправлення.

Створіть супутню службу:

```shell
nano ~/.config/systemd/user/lmstudio-restart.service
```

```ini
[Unit]
Description=LM Studio post-boot restart
After=lmstudio.service

[Service]
Type=oneshot
ExecStart=systemctl --user restart lmstudio.service
```

Створіть таймер:

```shell
nano ~/.config/systemd/user/lmstudio-restart.timer
```

```ini
[Unit]
Description=Restart LM Studio 2 minutes after boot

[Timer]
OnBootSec=2min
Unit=lmstudio-restart.service

[Install]
WantedBy=timers.target
```

Увімкніть таймер:

```shell
systemctl --user daemon-reload
systemctl --user enable --now lmstudio-restart.timer
```

Таймер чекає 2 хвилини після завантаження системи, перезапускає службу LM Studio один раз, а потім залишається неактивним. Якщо ви не відчуваєте повільного інференсу, цей крок не потрібний.

---

### Крок 5  Налаштування Elite Intel

Відкрийте в Elite Intel **Сервіси → Сервіси ШІ**. У розділі **Мовна модель (LLM)** виберіть **LMStudio**:

- **Адреса**: `http://localhost:1234/v1/chat/completions` (типове значення). Якщо LM Studio на іншій машині, замініть `localhost` на IP цієї машини.
- **Модель**: вставте рядок ідентифікатора моделі з `curl http://localhost:1234/v1/models`. Одна модель обслуговує і команди, і запити.

Для локального LLM ключ API не потрібен.

Натисніть **Зберегти**. Elite Intel сам перезапустить мовну модель.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
