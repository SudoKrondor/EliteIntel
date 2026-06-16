## Локальна LLM  налаштування на Linux (LM Studio)

Запуск локальної LLM зберігає всі дані приватними та офлайн. Плата за підписку відсутня. Застосовуються витрати на обладнання та електроенергію.

LM Studio є альтернативою Ollama. Вона використовує ті самі моделі та той самий OpenAI-сумісний API. Вибір можна змінити в налаштуваннях будь-коли.

Для роботи потрібні [LM Studio](https://lmstudio.ai) та здатний GPU.

---

### Мінімальні вимоги до обладнання

Щоб запустити Elite Dangerous та LLM на **одній машині**, потрібен мінімум **NVIDIA RTX 3060 з 12 GB VRAM**. При такій специфікації запас продуктивності обмежений.

> **Порада:** Elite Intel можна спрямувати на екземпляр LM Studio, що працює на **окремому ПК** у вашій мережі. Якщо доступна друга машина зі здатним GPU, ігровий ПК не несе жодного навантаження від інференсу в цій конфігурації.

---

### Рекомендована модель

| Модель | Потрібно VRAM | Примітки |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB | ✅ Рекомендована. Швидка, точна, відмінно підходить для команд і запитів. |
| `tulu-3.1-8b-supernova` Q8_0 | ~8.5 GB | Вища якість, якщо є запас VRAM. |
| `qwen3` 8B | ~8 GB | Експериментальна. Очікуйте випадкових пропущених команд та галюцинацій. |

---

[[youtube:2HGFmlZGK1g]]

---

### Крок 1  Встановлення LM Studio

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

### Крок 2  Завантаження моделі

```shell
lms get tulu3.1
Searching for models with the term tulu3.1
No exact match found. Please choose a model from the list below.

? Select a model to download
❯ QuantFactory/Tulu-3.1-8B-SuperNova-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF
  QuantFactory/Tulu-3.1-8B-SuperNova-Smart-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-GGUF
  bunnycore/Tulu-3.1-8B-SuperNova-Smart-IQ4_XS-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-Smart-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-Smart-i1-GGUF
  matrixportalx/Tulu-3.1-8B-SuperNova-Q4_0-GGUF
  matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF

↑↓ navigate • ⏎ select
```
Використовуйте клавіші зі стрілками для навігації та Enter для вибору. Виберіть `matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`.

Щоб переглянути завантажені моделі:

```shell
lms ls
```

Це стандартний шлях. Однак [LM Studio має відомий баг](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). У деяких випадках завантаження завершується помилкою:
```Error: No staff picks found with the specified search criteria.```

Якщо це сталося, завантажте модель вручну:

```shell
curl -s "https://huggingface.co/api/models/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF" | grep -o '"rfilename":"[^"]*\.gguf"'
```
Потім імпортуйте її:

```shell
lms import /path/to/tulu-3.1-8b-supernova-q4_k_m.gguf
```


---

### Крок 3  Запуск сервера

Завантажте модель та запустіть сервер інференсу:

```shell
lms load tulu-3.1-8b-supernova --context-length 8192 --gpu max
lms server start
```

`--gpu max` передає інференс на GPU для максимальної продуктивності.

Перевірте, що він запущений:

```shell
curl http://localhost:1234/v1/models
```

Ви отримаєте JSON-список завантажених моделей. Рядок ідентифікатора моделі у цій відповіді  це те, що ви введете у поле **LLM Model** в Elite Intel.

Щоб зупинити сервер:

```shell
lms server stop
```

> ⚠️ **Важливо:** Сервер LM Studio **не** переживає перезавантажень. Виконуйте `lms server start` після кожного перезапуску або налаштуйте необов'язковий автозапуск нижче.

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
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms load matrixportalx/tulu-3.1-8b-supernova --yes --context-length 8192
ExecStart=/home/YOUR_USERNAME/.lmstudio/bin/lms server start --bind 0.0.0.0 --port 1234
ExecStop=/home/YOUR_USERNAME/.lmstudio/bin/lms server stop
ExecStopPost=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon down

[Install]
WantedBy=default.target
```

Замініть `YOUR_USERNAME` на ваше ім'я користувача Linux та `YOUR_UID` на ваш ідентифікатор користувача. Щоб знайти ваш UID:

```shell
id -u
```

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

Деякі користувачі відчувають повільні відповіді інференсу, коли LM Studio запускається під час завантаження. Проблема вирішується одразу після ручного перезапуску служби. Це спричинено особливістю ініціалізації демона LM Studio. Перший холодний запуск може залишити середовище виконання інференсу у деградованому стані.

Якщо повільні відповіді з'являються після перезавантаження та зникають після ручного перезапуску, цей таймер автоматизує виправлення.

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
Description=Restart LM Studio 2 minutes after login

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

Таймер чекає 2 хвилини після входу в систему, перезапускає службу LM Studio один раз, а потім залишається неактивним. Якщо ви не відчуваєте повільного інференсу, цей крок не потрібний.

---

### Вимкнення автозапуску Ollama (якщо встановлено)

Ollama за замовчуванням встановлює себе як увімкнену службу systemd. Щоб замість неї запустити LM Studio та запускати Ollama лише на вимогу:

```shell
sudo systemctl disable ollama.service
sudo systemctl stop ollama.service
```

---

### Крок 5  Налаштування Elite Intel

Відкрийте вкладку **Settings** в Elite Intel:

- Залиште поле **LLM Key** порожнім (локальна LM Studio не потребує ключа).
- **LLM Address**: встановіть `http://localhost:1234/v1/chat/completions`. Якщо LM Studio знаходиться на іншій машині, замініть `localhost` на IP-адресу тієї машини.
- **LLM Model**: вставте рядок ідентифікатора моделі з відповіді `curl http://localhost:1234/v1/models`.
- **Command LLM**: встановіть той самий ідентифікатор моделі.
- **Query LLM**: встановіть той самий ідентифікатор моделі.
- Натисніть **Stop**, а потім **Start** на вкладці AI для застосування змін.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
