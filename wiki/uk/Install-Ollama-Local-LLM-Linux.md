## Локальна мовна модель  налаштування Linux (Ollama)

Запуск локальної мовної моделі забезпечує повну конфіденційність даних та роботу офлайн. Підписка не потрібна. Враховуються витрати на обладнання та електроенергію.

Необхідні [Ollama](https://ollama.com) і потужний GPU.

---

### Мінімальні вимоги до обладнання

Для запуску Elite Dangerous і мовної моделі на **одній машині** потрібна щонайменше **NVIDIA RTX 3060 з 12 ГБ VRAM**. Запас продуктивності при цій конфігурації обмежений.

> **Порада:** Elite Intel можна спрямувати на екземпляр Ollama, що працює на **окремому ПК** у вашій мережі. Якщо доступна друга машина з потужним GPU, ігровий ПК не несе навантаження інференсу в цій конфігурації.

---

### Рекомендована модель

| Модель | Необхідно VRAM | Примітки |
|---|---|---|
| `Tulu-3.1-8B-SuperNova-Q4_K_M`| ~5 ГБ | ✅ Рекомендується. Надійна робота з командами та запитами. |
| `qwen3` 8B | ~8 ГБ | Експериментальна. Можливі пропущені команди та галюцинації. |

> **Примітка:** Для найшвидшого локального інференсу розгляньте [LM Studio](Install-LM-Studio-Linux) з `matrixportalx/tulu-3.1-8b-supernova`. За результатами тестування, він помітно швидший за Ollama на тому самому обладнанні з тією самою моделлю.

---

### Крок 1  Встановлення Ollama

```shell
curl -fsSL https://ollama.com/install.sh | sh
```

Ollama встановлюється як служба systemd і запускається автоматично.

---

### Крок 2  Завантаження рекомендованої моделі

```shell
ollama pull hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

Або експериментальні альтернативи:

```shell
ollama pull qwen3:8b
```

---

### Крок 3  (Необов'язково) Налаштування служби Ollama

Ollama працює без додаткового налаштування. Наведена конфігурація покращує керування VRAM при спільному запуску з Elite Dangerous.

```shell
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Вставте наступне:

```ini
[Service]
Environment="OLLAMA_MAX_VRAM=14000000000"
Environment="OLLAMA_DEBUG=0"
Environment="OLLAMA_NUM_PARALLEL=3"
Environment="OLLAMA_MAX_LOADED_MODELS=1"
Environment="OLLAMA_FLASH_ATTENTION=1"
Environment="OLLAMA_KEEP_ALIVE=-1"
Nice=10
IOSchedulingClass=best-effort
IOSchedulingPriority=5
```

Потім перезавантажте конфігурацію та перезапустіть службу:

```shell
sudo systemctl daemon-reload
sudo systemctl restart ollama.service
```

#### Що роблять ці налаштування

**`OLLAMA_MAX_VRAM`**: Жорсткий ліміт VRAM, яку може використовувати Ollama, у байтах. `14000000000` = 14 ГБ. Решта залишається для Elite Dangerous. Скоригуйте відповідно до вашого GPU та вимог гри.

**`OLLAMA_NUM_PARALLEL`**: Кількість запитів, що обробляються одночасно. Elite Intel виконує асинхронні виклики, тому занадто низьке значення спричинить збої. `3` покриває типове перекриття команд і запитів без надмірного виділення ресурсів.

**`OLLAMA_MAX_LOADED_MODELS`**: Тримає в VRAM лише одну модель одночасно.

**`OLLAMA_FLASH_ATTENTION`**: Вмикає Flash Attention, що знижує використання пропускної здатності пам'яті під час інференсу. Як правило, швидше  особливо для повторюваних запитів.

**`OLLAMA_KEEP_ALIVE=-1`**: Тримає модель завантаженою у VRAM безстроково. Без цього Ollama може вивантажити модель після певного часу бездіяльності, що призведе до затримки при наступному запиті.

---

### Крок 4  Налаштування Elite Intel

Відкрийте **вкладку «Settings»** в Elite Intel:

- Залиште поле **LLM Key** порожнім (локальний Ollama ключ не потребує).
- **LLM Address** за замовчуванням: `http://localhost:11434/api/chat`. Якщо Ollama працює на іншій машині, замініть `localhost` на IP-адресу тієї машини.
- **Command LLM**: задайте `hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF:latest` (або ім'я з виводу `ollama ls`).
- **Query LLM**: задайте `hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF:latest` (або ім'я з виводу `ollama ls`).
- Натисніть **Stop**, а потім **Start** на вкладці AI, щоб застосувати зміни.

---

Спільнота 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
