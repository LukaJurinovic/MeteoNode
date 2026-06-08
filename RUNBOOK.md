# Firmware Runbook — rerun after Docker rebuild + DB wipe

> Git-ignored. Erase wipes NVS+LittleFS → both boards re-register vs the fresh DB
> (no DEPLOY_ID bump). After erase `config.h` rules: ensure `API_KEY=esp32-test-key-001`
> + correct WiFi. Set the COM port per board — `python -m platformio device list`.

```powershell
# 1. Fresh backend + DB (run from repo root)
docker compose down -v
docker compose up -d --build
docker compose logs -f backend                 # wait for Flyway + :8080

# 2. Gateway server IP — only if it changed (returns to root)
cd firmware\gateway; .\set-server-url.ps1 -Ip <server-ip>; cd ..\..

# 3. Erase + flash both boards (from repo root; one at a time, adjust COMx per board)
cd firmware\gateway; python -m platformio run -e gateway -t erase -t upload -t monitor --upload-port COM3 --monitor-port COM3
cd ..\node;          python -m platformio run -e node    -t erase -t upload -t monitor --upload-port COM3 --monitor-port COM3

# Monitor only: python -m platformio device monitor -e gateway -p COM3
```
