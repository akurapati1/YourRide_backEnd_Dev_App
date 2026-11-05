# YourRide — Cab Booking Backend (Spring Boot)

A production-style **ride-hailing backend** (Uber-like) built with **Spring Boot 3, Spring Security (JWT), PostgreSQL/PostGIS and JPA**. It covers rider/driver onboarding, geospatial driver matching, OTP-verified rides, fare strategies (incl. surge pricing), wallet/cash payments with platform commission, ratings and e-mail notifications.

## Highlights
- **Stateless auth** – short-lived JWT access tokens + HttpOnly refresh-token cookie; token *type* claims prevent refresh tokens being used as access tokens
- **Role-based access** – `RIDER`, `DRIVER`, `ADMIN` enforced with `@Secured`
- **Geospatial matching** – PostGIS (`hibernate-spatial`) finds the 10 nearest / top-rated drivers around the pickup point
- **Strategy pattern** for pricing (default vs. 2× surge 6–9 PM), driver matching (nearest vs. highest-rated for riders ≥ 4.8★) and payment (wallet vs. cash)
- **Real road distances** via the OSRM routing API
- **Ride lifecycle** – request → accept → OTP start → end → payment → mutual ratings
- Consistent API envelope and global exception handling (404 / 409 / 401 / 403 / 400 validation errors)
- Swagger UI (springdoc), Actuator, unit + Testcontainers integration tests, GitHub Actions CI

## Architecture
```
controllers (Auth / Rider / Driver)  →  services (+ strategies)  →  repositories (JPA + PostGIS)  →  PostgreSQL
              ↑ JwtAuthFilter (Bearer access token)                      ↘ OSRM (distance)  ↘ SMTP (notifications)
```
Package layout: `controllers`, `services` / `services.impl`, `strategies`, `entities`, `repositories`, `dto`, `security`, `configs`, `advices` (error handling), `exceptions`, `utils`.

## API
All responses are wrapped as `{ "timeStamp": …, "data": … }` or `{ "error": … }`. Authenticated routes need `Authorization: Bearer <accessToken>`.

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/auth/signup` | public | Register a rider (creates rider profile + wallet) |
| POST | `/auth/login` | public | Returns `accessToken`; sets HttpOnly `refreshToken` cookie |
| POST | `/auth/refresh` | cookie | Issues a new access token from the refresh cookie |
| POST | `/auth/onBoardNewDriver/{userId}` | ADMIN | Promote a user to driver (`{"vehicleId": "..."}`) |
| POST | `/riders/requestRide` | RIDER | Request a ride (pickup/drop-off points, payment method) |
| POST | `/riders/cancelRide/{rideId}` | RIDER | Cancel a confirmed ride |
| POST | `/riders/rateDriver` | RIDER | `{"rideId": 1, "rating": 5}` after the ride ends |
| GET | `/riders/getMyProfile` · `/riders/getMyRides` | RIDER | Profile · paginated ride history |
| POST | `/drivers/acceptRide/{rideRequestId}` | DRIVER | Accept a pending request |
| POST | `/drivers/startRide/{rideId}` | DRIVER | Start with the rider's OTP (`{"otp": "1234"}`) |
| POST | `/drivers/endRide/{rideId}` | DRIVER | End ride and process payment |
| POST | `/drivers/cancelRide/{rideId}` | DRIVER | Cancel a confirmed ride |
| POST | `/drivers/rateRider` | DRIVER | Rate the rider |
| GET | `/drivers/getMyProfile` · `/drivers/getMyRides` | DRIVER | Profile · paginated ride history |

Interactive docs: `http://localhost:8080/swagger-ui.html`.

## Getting started
Requirements: **JDK 21**, Maven (or `./mvnw`), Docker (for the database).

```bash
docker compose up -d                       # PostgreSQL + PostGIS
export DB_PASSWORD=postgres
export JWT_SECRET="$(openssl rand -base64 48)"
export SPRING_PROFILES_ACTIVE=dev          # optional: create schema, log SQL
./mvnw spring-boot:run
```
All settings are environment variables — see `.env.example`. **Never commit credentials**; `application.properties` only contains `${…}` placeholders and the app refuses to start without `JWT_SECRET`.

## Deploy for free
[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/akurapati1/YourRide_backEnd_Dev_App)

Everything here has a free tier (no card needed for the first two):
1. **Database** - create a free Postgres at [Neon](https://neon.tech) (PostGIS is built in), open its SQL editor and run `CREATE EXTENSION IF NOT EXISTS postgis;`.
2. **App** - click the button, sign in with GitHub; `render.yaml` + `CabBookingAppBackend/Dockerfile` create a free web service. Enter `DB_URL` (`jdbc:postgresql://HOST/DB?sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`; `JWT_SECRET` is generated. `MAIL_*` are optional.
3. Verify `https://<service>.onrender.com/actuator/health` and `/swagger-ui.html`.

Free services sleep when idle and have 512 MB RAM - the first request after a pause takes a minute. Create the first admin user directly in the database (the `/auth/onBoardNewDriver` endpoint needs an `ADMIN` role).

## Tests
```bash
./mvnw test -Dtest='JWTServiceTest,RatingServiceImplTest,StrategiesTest'   # fast unit tests, no Docker
./mvnw verify                                                              # + Testcontainers integration tests (needs Docker)
```

## Configuration reference
| Variable | Default | Description |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local Postgres | Database connection |
| `DDL_AUTO` | `update` | Hibernate schema strategy |
| `JWT_SECRET` | **required** | HMAC key (≥ 32 chars) |
| `JWT_ACCESS_MINUTES` / `JWT_REFRESH_DAYS` | `10` / `30` | Token lifetimes |
| `COOKIE_SECURE` | `false` | Set `true` behind HTTPS |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail SMTP | Notification e-mails |

## Known limitations / roadmap
Wallet top-ups (Stripe/Razorpay) and driver-location update endpoints are not exposed yet; wallet balance is not checked before a wallet-paid ride starts.
