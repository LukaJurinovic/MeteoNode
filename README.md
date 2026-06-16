# MeteoNode

Self-hosted environmental monitoring platform for sensor nodes that have **no WiFi**.
Nodes talk to the world exclusively over **SMS** (SIM800L); a single ESP32 gateway
bridges the cellular network to a Spring Boot REST API, and a React dashboard gives
operators a live view of every station.

```
[Sensor node] ──SMS──► [Gateway] ──HTTP──► [Backend] ◄──HTTP── [React frontend]
 ESP32 + SIM800L         ESP32              Spring Boot          browser
 BMP280 · DHT11          WiFi + SIM         MySQL
 GUVA · BH1750
```

## Components

| Component | Stack | Role |
|---|---|---|
| **Node** | ESP32 + SIM800L (Arduino/C++) | Reads sensors, sends one SMS per cycle |
| **Gateway** | ESP32 + SIM800L (Arduino/C++) | SMS ⇄ HTTP bridge, command relay |
| **Backend** | Spring Boot 3 · MySQL · Flyway · JWT | REST API, ingestion, alarms, command queue |
| **Frontend** | React · Vite · TS · Tailwind · React Query | Operator dashboard |
| **Shared lib** | `firmware/lib/sms_format` (pure C++) | SMS parse/build, unit-tested off-device |

## Data flow

- **Node → gateway** — a node wakes on its interval, reads sensors, and sends
  `SERIAL,temp,pressure,humidity,uv,lux`. Failed readings are sent as `nan` and dropped.
- **Gateway → server** — the gateway registers unknown nodes (`CAPS:` SMS), then posts
  measurement batches and polls each node's command queue every 30 s.
- **Server → node** — pending commands (`REQUEST_READINGS`, `REBOOT`, `SET_INTERVAL`) are
  batched into one SMS; the node acknowledges with `CONF:<id>` on its next data SMS.

## Features

- Dual auth: **JWT** (users) + **API key** (gateway) on one filter chain
- Roles: `ADMIN`, `OPERATOR`, `USER`
- Self-registering nodes via dynamic sensor advertisement (`CAPS:`)
- Alarm rules with atomic, race-free cooldown firing
- At-least-once command delivery with a lease/visibility window
- Historical charts, live station/node counts, per-user notification read state

## Running

```bash
# Everything via Docker
docker compose up --build
# Frontend → http://localhost:3000
# API + docs → http://localhost:8080/scalar.html

# Backend only (local MySQL on port 3308)
./mvnw spring-boot:run

# Backend tests
./mvnw test

# Firmware unit tests (no hardware)
cd firmware && pio test -e native
```

Seed users (password `admin123`): `admin` (ADMIN), `operator` (OPERATOR), `viewer` (USER).

## Layout

```
backend/    Spring Boot API (controllers → app services → domain services → repositories)
frontend/   React + Vite dashboard
firmware/   node.ino, gateway.ino, shared sms_format lib, native tests
```
