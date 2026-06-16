## Локальный LLM  установка на Linux (Ollama)

Запуск локального LLM обеспечивает полную конфиденциальность данных и работу офлайн. Подписка не требуется. Учитываются затраты на оборудование и электроэнергию.

Требуется [Ollama](https://ollama.com) и мощный GPU.

---

### Минимальные требования к оборудованию

Для запуска Elite Dangerous и LLM на **одном компьютере** требуется минимум **NVIDIA RTX 3060 с 12 ГБ VRAM**. На этой конфигурации производительность ограничена.

> **Подсказка:** Elite Intel можно направить на экземпляр Ollama, работающий на **отдельном компьютере** в вашей сети. Если доступна вторая машина с мощным GPU, игровой ПК не несёт нагрузки инференса в этой конфигурации.

---

### Рекомендуемая модель

| Модель | Требуется VRAM | Примечания |
|---|---|---|
| `Tulu-3.1-8B-SuperNova-Q4_K_M`| ~5 ГБ | ✅ Рекомендуется. Надёжная работа с командами и запросами. |
| `qwen3` 8B | ~8 ГБ | Экспериментальная. Возможны пропущенные команды и галлюцинации. |

> **Примечание:** Для наиболее быстрого локального инференса рассмотрите [LM Studio](Install-LM-Studio-Linux) с `matrixportalx/tulu-3.1-8b-supernova`. По результатам тестирования, он заметно быстрее Ollama на том же оборудовании с той же моделью.

---

### Шаг 1  Установка Ollama

```shell
curl -fsSL https://ollama.com/install.sh | sh
```

Ollama устанавливается как служба systemd и запускается автоматически.

---

### Шаг 2  Загрузка рекомендуемой модели

```shell
ollama pull hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

Или экспериментальные альтернативы:

```shell
ollama pull qwen3:8b
```

---

### Шаг 3  (Необязательно) Тонкая настройка службы Ollama

Ollama работает без настройки. Следующая конфигурация улучшает управление VRAM при совместном запуске с Elite Dangerous.

```shell
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Вставьте следующее:

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

Затем перезагрузите конфигурацию и перезапустите:

```shell
sudo systemctl daemon-reload
sudo systemctl restart ollama.service
```

#### Что делают эти настройки

**`OLLAMA_MAX_VRAM`**: Жёсткий лимит VRAM, который может использовать Ollama, в байтах. `14000000000` = 14 ГБ. Оставляет остаток для Elite Dangerous. Скорректируйте под свой GPU и требования игры.

**`OLLAMA_NUM_PARALLEL`**: Количество одновременно обрабатываемых запросов. Elite Intel выполняет асинхронные вызовы, поэтому слишком низкое значение вызовет сбои. `3` покрывает типичное перекрытие команд и запросов без избыточного выделения ресурсов.

**`OLLAMA_MAX_LOADED_MODELS`**: Держит в VRAM только одну модель одновременно.

**`OLLAMA_FLASH_ATTENTION`**: Включает Flash Attention, снижающее использование пропускной способности памяти при инференсе. Как правило, быстрее, особенно для повторяющихся запросов.

**`OLLAMA_KEEP_ALIVE=-1`**: Держит модель загруженной в VRAM бессрочно. Без этого Ollama может выгрузить модель после периода бездействия, что приведёт к задержке при повторной загрузке.

---

### Шаг 4  Настройка Elite Intel

Откройте **вкладку «Настройки»** в Elite Intel:

- Оставьте поле **LLM Key** пустым (локальный Ollama ключ не требует).
- **LLM Address** по умолчанию: `http://localhost:11434/api/chat`. Если Ollama работает на другом компьютере, замените `localhost` на IP этого компьютера.
- **Command LLM**: задайте `hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF:latest` (или имя из `ollama ls`).
- **Query LLM**: задайте `hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF:latest` (или имя из `ollama ls`).
- Нажмите **Stop**, затем **Start** на вкладке AI для применения изменений.

---

Сообщество 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
