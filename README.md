# rapport-probe

Отдельный Java-сервис: снаружи проверяет, отвечают ли API Раппорта и Jitsi, и показывает результат в JSON и в метриках Prometheus. Игры, колоды и пользователи остаются в Nest.

Порт probe: **8080**. Локальный Nest: **3000**.

## Требования

- JDK 17 и Maven 3.6.3+ (локальный запуск)
- Docker Desktop (локальный контейнер) или Docker на VPS (prod)
- Для локальной проверки API — Nest с `GET http://127.0.0.1:3000/api/health` → `{"status":"ok"}`

## Сборка и тесты

```powershell
cd dev/probe
mvn test
mvn -DskipTests package
```

Ожидание: `BUILD SUCCESS`. Jar: `target/probe-0.0.1.jar`.

## Локальный запуск (без Docker)

```powershell
cd dev/probe
mvn spring-boot:run
```

В `application.yml` две цели:

- `api` → `http://127.0.0.1:3000/api/health` (не `localhost`: на Windows HttpClient часто уходит в IPv6)
- `jitsi` → `https://meet.rapport-game.online/`

Клиент принудительно HTTP/1.1.

## Локальный Docker (только probe)

Nest на **хосте**. Локальный `mvn spring-boot:run` остановите.

```powershell
cd dev/probe
docker compose -f compose.dev.yaml up --build
```

Контейнер ходит на Nest через `host.docker.internal:3000` (см. `compose.dev.yaml`). В этом файле по умолчанию одна цель `api`; Jitsi добавляйте env при необходимости.

Логи: `docker compose -f compose.dev.yaml logs -f probe`. Остановка: Ctrl+C или `docker compose -f compose.dev.yaml down`.

## Prod на VPS (probe + Prometheus + Grafana)

Стек рассчитан на VPS рядом с Jitsi. Порты слушаются **только на 127.0.0.1** — с интернета Grafana/Prometheus/probe не открыты. Доступ с ПК — SSH-туннель.

Цели prod (в `compose.prod.yaml`):

| Цель | URL |
|------|-----|
| `api` | `https://rapport-api.fly.dev/api/health` |
| `jitsi` | `https://meet.rapport-game.online/` |

Проверка Jitsi — HTTP 2xx на главную страницу meet. Это «веб Jitsi жив», не полная диагностика JVB.

### Выкладка

На VPS (пример каталога `/opt/rapport-probe`): скопируйте содержимое `dev/probe` (как минимум `Dockerfile`, `pom.xml`, `src`, `compose.prod.yaml`, `prometheus/`, `grafana/`, `env.prod.example`).

```bash
cd /opt/rapport-probe
cp env.prod.example .env
# задайте GF_SECURITY_ADMIN_PASSWORD в .env
docker compose -f compose.prod.yaml --env-file .env up -d --build
docker compose -f compose.prod.yaml ps
```

### SSH-туннель с ПК

```powershell
ssh -L 8081:127.0.0.1:8081 -L 9090:127.0.0.1:9090 -L 3001:127.0.0.1:3001 user@YOUR_VPS

user@YOUR_VPS : root@195.19.195.228
```

На VPS порт **8080** занят `jitsi-jvb`; probe слушает **8081** и снаружи, и внутри контейнера (`SERVER_PORT=8081`, проброс `127.0.0.1:8081:8081`).

Пока туннель открыт:

| На ПК | Сервис |
|-------|--------|
| http://127.0.0.1:3001 | Grafana (логин `admin`, пароль из `.env`) |
| http://127.0.0.1:8081/status | JSON probe |
| http://127.0.0.1:9090 | UI Prometheus |

В Grafana: меню **Dashboards** → **Browse** → папка **Rapport** → **Rapport probe** (не домашний экран). Datasource Prometheus подключается сам.

Логи на VPS: `docker compose -f compose.prod.yaml logs -f probe`.

## Endpoints probe

| Адрес | Назначение |
|-------|------------|
| `GET /status` | Последний результат по целям (JSON) |
| `GET /actuator/health` | Жив ли сам probe |
| `GET /actuator/prometheus` | Метрики, в т.ч. `rapport_target_*` |

Примеры (локально или через туннель):

```powershell
curl.exe http://127.0.0.1:8080/status
curl.exe http://127.0.0.1:8080/actuator/health
curl.exe http://127.0.0.1:8080/actuator/prometheus | findstr rapport_target
```

## Метрики

| Метрика | Смысл |
|---------|--------|
| `rapport_target_up{target=...}` | `1` — HTTP 2xx, `0` — нет |
| `rapport_target_latency_seconds{target=...}` | Длительность последнего запроса (с) |
| `rapport_target_last_success_timestamp_seconds{target=...}` | Unix-время последнего успеха |

Интервал: локально `probe.interval` (5000 мс), в prod compose — `PROBE_INTERVAL=15000`.

## Если `rapport_target_up` стал 0

1. `/status` — смотрите `detail` и имя цели (`api` / `jitsi`).
2. С хоста или туннеля проверьте URL цели напрямую (`curl`).
3. Для Docker lokal: URL API должен быть `host.docker.internal`, не `127.0.0.1` внутри контейнера.
4. Логи probe: `docker compose -f compose.dev.yaml logs` / `docker compose -f compose.prod.yaml logs`.
5. `/actuator/health` у probe может оставаться `UP`: упала цель, не сам пробник.

## Конфиг целей

Локально — `application.yml`. В Docker — переменные `PROBE_TARGETS_0_NAME`, `PROBE_TARGETS_0_URL`, `PROBE_TARGETS_1_…` (см. `compose.dev.yaml` / `compose.prod.yaml`).
