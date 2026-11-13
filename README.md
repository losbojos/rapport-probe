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

Стек стоит на VPS рядом с Jitsi (`root@195.19.195.228`, каталог `/opt/rapport-probe`).

Дашборд открыт всем: **https://status.rapport-game.online**. Логин не нужен, роль анонима — Viewer, домашняя страница — **Rapport probe**. Админка: https://status.rapport-game.online/login, пользователь `admin`, пароль из `.env` на VPS. Регистрация выключена.

Probe (`127.0.0.1:8081`) и Prometheus (`127.0.0.1:9090`) с интернета закрыты. Grafana с хоста тоже слушает только `127.0.0.1:3001`; наружу её отдаёт nginx контейнера `jitsi-web`.

Цели prod (в `compose.prod.yaml`):

| Цель | URL |
|------|-----|
| `api` | `https://rapport-api.fly.dev/api/health` |
| `jitsi` | `https://meet.rapport-game.online/` |

Проверка Jitsi — HTTP 2xx на главную страницу meet. Это «веб Jitsi жив», не полная диагностика JVB.

### Выкладка

На VPS скопируйте содержимое репозитория (как минимум `Dockerfile`, `pom.xml`, `src`, `compose.prod.yaml`, `prometheus/`, `grafana/`, `env.prod.example`).

```bash
cd /opt/rapport-probe
cp env.prod.example .env
# задайте GF_SECURITY_ADMIN_PASSWORD в .env
docker compose -f compose.prod.yaml --env-file .env up -d --build
docker compose -f compose.prod.yaml ps
```

`GF_SECURITY_ADMIN_PASSWORD` действует только при первом создании пользователя `admin`. Сеть `jitsiweb` в `compose.prod.yaml` — внешняя сеть Jitsi `jitsi_meet.jitsi`. Без неё nginx в `jitsi-web` не видит контейнер `rapport-probe-grafana-1`.

На VPS порт **8080** занят `jitsi-jvb`; probe слушает **8081** (`SERVER_PORT=8081`, проброс `127.0.0.1:8081:8081`).

Логи на VPS: `docker compose -f compose.prod.yaml logs -f probe`.

### Публичный адрес Grafana

Порты **80** и **443** заняты контейнером `jitsi-web` (`0.0.0.0:80→8000`, `0.0.0.0:443→8443`). Отдельный nginx на хосте сюда не встанет. Поддомен — свой `server` внутри этого же nginx.

| Что | Где |
|-----|-----|
| DNS | `A` `status.rapport-game.online` → `195.19.195.228` |
| Конфиг Jitsi на хосте | `/root/.jitsi-meet-cfg/web` (в контейнере это `/config`) |
| Сайт nginx | `/root/.jitsi-meet-cfg/web/nginx/site-confs/status.conf` |
| Сертификат | `/root/.jitsi-meet-cfg/web/keys/status.crt` и `status.key` |
| Каталог для certbot | `/root/.jitsi-meet-cfg/web/acme` |

`status.conf` слушает **8000** (HTTP, выпуск и продление сертификата) и **8443** (HTTPS). HTTPS проксирует на `http://rapport-probe-grafana-1:3000`. Сертификат выпущен certbot'ом:

```bash
certbot certonly --webroot -w /root/.jitsi-meet-cfg/web/acme -d status.rapport-game.online
```

Живой nginx читает копию конфига, которую контейнер собирает при старте в `/run/web/config`. Файл в `/config` сам по себе процесс не подхватывает. После правки `status.conf` перезапустите контейнер: `docker restart jitsi-web-1` (страница meet пропадёт на несколько секунд). Проверка до перезапуска:

```bash
docker exec -u root jitsi-web-1 nginx -t -c /run/web/config/nginx/nginx.conf
```

Команда `nginx -t` без `-c` смотрит чужой файл и на этой read-only файловой системе падает на `/var/log/nginx/error.log`, даже если конфиг Jitsi верный.

Продление: certbot обновляет файлы в `/etc/letsencrypt` сам. Хук `/etc/letsencrypt/renewal-hooks/deploy/status-grafana.sh` копирует их в `keys/status.crt` и `keys/status.key` и посылает nginx `HUP`. Сертификат действует до **2027-02-11**.

### SSH-туннель с ПК

Туннель нужен для JSON probe и UI Prometheus. Дашборд смотрите по публичному адресу.

```powershell
ssh -L 8081:127.0.0.1:8081 -L 9090:127.0.0.1:9090 root@195.19.195.228
```

Пока туннель открыт:

| На ПК | Сервис |
|-------|--------|
| http://127.0.0.1:8081/status | JSON probe |
| http://127.0.0.1:9090 | UI Prometheus |

Скрипт `scripts/tunnel.ps1` по-прежнему пробрасывает и порт Grafana `3001`. Для просмотра дашборда он не нужен.

## Endpoints probe

| Адрес | Назначение |
|-------|------------|
| `GET /status` | Последний результат по целям (JSON) |
| `GET /actuator/health` | Жив ли сам probe |
| `GET /actuator/prometheus` | Метрики, в т.ч. `rapport_target_*` |

Примеры локально (порт **8080**). На VPS через туннель тот же путь на порту **8081**:

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
