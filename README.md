# rapport-probe

Отдельный Java-сервис: снаружи проверяет, отвечает ли API Раппорта (`GET /api/health`), и показывает результат в JSON и в метриках Prometheus. Игры, колоды и пользователи остаются в Nest.

Порт probe: **8080**. Nest: **3000**.

## Требования

- JDK 17 и Maven 3.6.3+ (локальный запуск)
- Docker Desktop (запуск в контейнере)
- Для проверки «цель жива» — запущенный Nest с `GET http://127.0.0.1:3000/api/health` → `{"status":"ok"}`

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

Цель по умолчанию в `src/main/resources/application.yml`: `http://127.0.0.1:3000/api/health` (не `localhost`: Java HttpClient на Windows часто уходит в IPv6). Клиент принудительно HTTP/1.1.

## Запуск в Docker

Nest должен работать **на хосте**. Локальный `mvn spring-boot:run` остановите — порт 8080 занят контейнером.

```powershell
cd dev/probe
docker compose up --build
```

Контейнер ходит на Nest через `host.docker.internal:3000` (см. `compose.yaml`).

Логи:

```powershell
docker compose logs -f probe
```

Остановка: Ctrl+C в том же терминале или `docker compose down`.

## Endpoints

| Адрес | Назначение |
|-------|------------|
| `GET http://localhost:8080/status` | Последний результат проверки целей (JSON) |
| `GET http://localhost:8080/actuator/health` | Жив ли сам probe |
| `GET http://localhost:8080/actuator/prometheus` | Метрики, в т.ч. `rapport_target_*` |
| `GET http://127.0.0.1:3000/api/health` | Цель: health Nest (не endpoint probe) |

Примеры:

```powershell
curl.exe http://localhost:8080/status
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:8080/actuator/prometheus | findstr rapport_target
```

Живой Nest: в `/status` поле `"up": true`, `"detail": "ok"`.

## Метрики

| Метрика | Смысл |
|---------|--------|
| `rapport_target_up{target="api"}` | `1` — цель ответила HTTP 2xx, `0` — нет |
| `rapport_target_latency_seconds{target="api"}` | Длительность последнего запроса (секунды) |
| `rapport_target_last_success_timestamp_seconds{target="api"}` | Unix-время последнего успеха; при падении не сбрасывается |

Интервал опроса: `probe.interval` в `application.yml` (по умолчанию 5000 мс). После смены состояния цели подождите один цикл.

## Если `rapport_target_up` стал 0

1. Откройте `http://localhost:8080/status` — смотрите `detail` (`unreachable`, `http 500`, …).
2. Проверьте Nest с хоста: `curl.exe http://127.0.0.1:3000/api/health`.
3. Если Nest ок, а probe в Docker всё ещё `0` — убедитесь, что в `compose.yaml` URL цели `http://host.docker.internal:3000/api/health`, контейнер перезапущен.
4. Логи probe: `docker compose logs probe` или вывод `mvn spring-boot:run`.
5. `GET /actuator/health` у probe при этом может оставаться `UP`: упал API, не сам пробник.

## Конфиг целей

Локально — `application.yml`:

```yaml
probe:
  interval: 5000
  targets:
    - name: api
      url: http://127.0.0.1:3000/api/health
```

В Docker те же поля можно задать переменными окружения (как в `compose.yaml`): `PROBE_TARGETS_0_NAME`, `PROBE_TARGETS_0_URL`.
