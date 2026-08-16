## Локальный LLM  установка на Linux (LM Studio)

Запуск локального LLM обеспечивает полную конфиденциальность данных и работу офлайн. Подписка не требуется. Учитываются затраты на оборудование и электроэнергию.

Требуется [LM Studio](https://lmstudio.ai) и мощный GPU.

---

### Минимальные требования к оборудованию

Для запуска Elite Dangerous и LLM на **одном компьютере** требуется минимум **NVIDIA RTX 3060 с 12 ГБ VRAM**. На этой конфигурации производительность ограничена.

> **Подсказка:** Elite Intel можно направить на экземпляр LM Studio, работающий на **отдельном компьютере** в вашей сети. Если доступна вторая машина с мощным GPU, игровой ПК не несёт нагрузки инференса в этой конфигурации.

---

### Рекомендуемая модель

| Модель | Требуется VRAM | Примечания |
|---|---|---|
| `google/gemma-4-e4b` | ~6.3 ГБ | ✅ Требуется |

> **Почему именно она?** Она поддерживает вызов функций, необходимый компаньону. Модель, которая не умеет
> формировать вызов инструмента, не сможет управлять приложением, как бы хорошо она ни
> писала. См. [Выбрать LLM](installing-local-llms).

---

[[youtube:2HGFmlZGK1g]]

---

### Шаг 1  Установка LM Studio

```shell
curl -fsSL https://lmstudio.ai/install.sh | bash
```

Установщик размещает всё в `~/.lmstudio/` и добавляет CLI-инструмент `lms`. После завершения установки добавьте CLI в PATH:

```shell
# Добавьте это в ~/.bashrc
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Затем перезагрузите оболочку:

```shell
source ~/.bashrc
```

Проверьте, что всё работает:

```shell
lms --help
```

---

### Шаг 2  Загрузка модели

```shell
lms get google/gemma-4-e4b
```
Если имя не совпадает точно, `lms get` покажет найденные варианты — выберите нужный
стрелками и клавишей Enter.

Чтобы просмотреть загруженные модели:

```shell
lms ls
```

Это стандартный путь. Однако [в LM Studio есть известная ошибка](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). В некоторых случаях загрузка завершается с ошибкой:
```Error: No staff picks found with the specified search criteria.```

В этом случае скачайте GGUF вручную со страницы модели на HuggingFace и импортируйте его:

```shell
lms import /path/to/the-model.gguf
```

---

### Шаг 3  Запуск сервера

Загрузите модель и запустите сервер инференса:

```shell
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

`--gpu max` переносит инференс на GPU для максимальной производительности.

Проверьте работу:

```shell
curl http://localhost:1234/v1/models
```

Вы должны получить JSON-список загруженных моделей. Строка model ID в этом ответе  то, что нужно ввести в поле **LLM Model** в Elite Intel.

Остановить сервер:

```shell
lms server stop
```

> ⚠️ **Важно:** Сервер LM Studio **не переживает перезагрузку**. После каждого перезапуска выполняйте `lms server start` снова или настройте автозапуск, описанный ниже.

---

### Шаг 4  (Необязательно) Автозапуск при загрузке системы

Чтобы LM Studio запускался автоматически, настройте его как **пользовательскую** службу systemd. Она работает под вашей сессией, а не как системная служба. Запускается после старта рабочего стола. Root-права не требуются.

Узнайте свой ID пользователя (замените имя пользователя на ваше реальное):
```shell
id -u YOUR_USER_NAME
```

Запомните это число  оно понадобится для конфигурации.

Создайте каталог пользовательских служб systemd, если его нет:

```shell
mkdir -p ~/.config/systemd/user
```

Создайте файл службы:

```shell
nano ~/.config/systemd/user/lmstudio.service
```

Вставьте следующее:

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
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms load google/gemma-4-e4b --yes --context-length 8192
ExecStart=/home/YOUR_USERNAME/.lmstudio/bin/lms server start --bind 0.0.0.0 --port 1234
ExecStop=/home/YOUR_USERNAME/.lmstudio/bin/lms server stop
ExecStopPost=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon down

[Install]
WantedBy=default.target
```

Замените `YOUR_USERNAME` на ваше имя пользователя Linux, а `YOUR_UID`  на ваш ID пользователя. Чтобы узнать UID:

```shell
id -u
```

> ⚠️ **Зачем нужен `XDG_RUNTIME_DIR`?** Пользовательские службы запускаются в ограниченном окружении, которое может не включать переменные сессии. LM Studio использует `XDG_RUNTIME_DIR` для IPC. Без него служба может завершаться незаметно даже при корректной работе `lms` из терминала. Это наиболее частая причина сбоя службы, когда ручной запуск работает нормально.

Включите и запустите:

```shell
systemctl --user daemon-reload
systemctl --user enable lmstudio.service
systemctl --user start lmstudio.service
```

Проверьте работу:

```shell
systemctl --user status lmstudio.service
curl http://localhost:1234/v1/models
```

> **Устранение неполадок:** Если служба не запускается, проверьте журнал:
> ```shell
> journalctl --user -xeu lmstudio.service --no-pager | tail -40
> ```
> Если сообщается «Failed to load model», запустите `lms ls` и убедитесь, что имя модели точно совпадает с тем, что указано в файле службы.

---

### Шаг 4b  (Необязательно) Исправление медленного инференса после загрузки

Некоторые пользователи сталкиваются с медленными ответами при запуске LM Studio при загрузке системы. Проблема немедленно устраняется после ручного перезапуска службы. Это связано с особенностью инициализации демона LM Studio. Первый холодный запуск может оставить среду выполнения инференса в деградированном состоянии.

Если медленные ответы появляются после перезагрузки и пропадают после ручного перезапуска, следующий таймер автоматизирует это исправление.

Создайте вспомогательную службу:

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

Создайте таймер:

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

Включите таймер:

```shell
systemctl --user daemon-reload
systemctl --user enable --now lmstudio-restart.timer
```

Таймер ждёт 2 минуты после входа в систему, однократно перезапускает службу LM Studio и затем остаётся неактивным. Если медленного инференса у вас нет, этот шаг не нужен.

---

### Шаг 5  Настройка Elite Intel

Откройте **вкладку «Настройки»** в Elite Intel:

- Оставьте поле **LLM Key** пустым (локальный LM Studio ключ не требует).
- **LLM Address**: установите `http://localhost:1234/v1/chat/completions`. Если LM Studio работает на другом компьютере, замените `localhost` на IP этого компьютера.
- **LLM Model**: вставьте строку model ID из `curl http://localhost:1234/v1/models`.
- **Command LLM**: задайте тот же model ID.
- **Query LLM**: задайте тот же model ID.
- Нажмите **Stop**, затем **Start** на вкладке AI для применения изменений.

---

Сообщество 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
