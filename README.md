# VnExpress News — React Native + Spring WebFlux

A learning-oriented end-to-end news reader. The backend reads an RSS feed, stores article metadata in PostgreSQL, and exposes a REST API. The Expo app displays real feed entries and opens the original article URL.

> **Content and access note:** Use the official feeds listed at https://e.vnexpress.net/rss and review the publisher's current terms before using the data. This project stores/display article metadata and links; it does not download or republish article bodies or images. The publisher may restrict feed use. Do not treat this starter as permission for commercial redistribution.

## Architecture

- `mobile/`: Expo + React Native + TypeScript
- `backend/`: Java 25, Spring Boot WebFlux, R2DBC PostgreSQL, RSS parsing
- `deploy/compose.yaml`: local backend + PostgreSQL + OpenTelemetry Collector + Jaeger
- `deploy/kubernetes/`: starter Kubernetes manifests (local cluster learning; harden before production)
- `.github/workflows/ci.yml`: backend tests and mobile TypeScript check

The RSS fetch/parse operation is blocking I/O, so it is run on Reactor's bounded elastic scheduler. API handlers and database access use WebFlux and R2DBC. Scheduled collection runs in one backend instance; if you scale replicas, add leader election or a distributed lock to avoid duplicate polling.

## Requirements

- JDK 25
- Node.js LTS and npm
- Docker Desktop / Docker Engine with Compose
- Expo Go for quick local testing, or Xcode / Android Studio for native builds

## 1. Run the backend + database

From repository root:

```bash
docker compose -f deploy/compose.yaml up --build -d
```

Check health and articles:

```bash
curl -s http://localhost:8080/actuator/health
curl -s 'http://localhost:8080/api/v1/news?limit=20' | python -m json.tool
curl -s http://localhost:8080/api/v1/admin/collection/status | python -m json.tool
```

The initial collection runs on startup and then follows `NEWS_COLLECTION_INTERVAL_MS` (default 300000 = 5 minutes). The RSS URL defaults to the VnExpress English RSS feed `https://vnexpress.net/rss/tin-moi-nhat.rss`; verify current feed URLs on the official RSS page. You can override `NEWS_RSS_URL` with a permitted feed URL.

If the source is unreachable or blocks requests, the API still returns previously stored entries and collection status records the error. On a fresh database with no successful fetch, the list will be empty rather than filled with fake news.

## 2. Run the mobile app

```bash
cd mobile
npm install
```

Create `mobile/.env` from the example:

```bash
cp .env.example .env
```

Set `EXPO_PUBLIC_API_BASE_URL`:
- iOS Simulator: `http://localhost:8080`
- Android Emulator: `http://10.0.2.2:8080`
- Physical phone: `http://YOUR_COMPUTER_LAN_IP:8080` (phone and computer on same Wi-Fi; allow the port through your firewall)

Then:

```bash
npx expo start
```

Scan the QR code with Expo Go. Local HTTP is for development only. A public deployment must use HTTPS.

## API

- `GET /api/v1/news?limit=20` — latest stored articles
- `GET /api/v1/news/{id}` — one article
- `GET /api/v1/admin/collection/status` — last attempt/success/error (protect before public production use)
- `POST /api/v1/admin/collection/run` — trigger collection (protect before public production use)
- `GET /actuator/health` — health check
- `GET /actuator/prometheus` — metrics endpoint, if enabled

**Security:** The two `/api/v1/admin/**` endpoints are intentionally unauthenticated in this local learning starter. Do not expose them publicly as-is. Add authentication/authorization, restrict management endpoints, and use network-level access controls before deployment.

## Debugging exercises

1. Set `NEWS_RSS_URL` to an invalid URL; observe collection status and logs.
2. Stop PostgreSQL; inspect backend logs and readiness/health behavior.
3. Set a very short interval only against a controlled/mock feed. Do not poll the publisher every 10 seconds in production.
4. Use Jaeger at http://localhost:16686 to inspect traces. The collector accepts OTLP gRPC on 4317 and HTTP on 4318.
5. Check metrics at http://localhost:8080/actuator/prometheus.

## Observability

The backend exports OTLP/gRPC traces to the OpenTelemetry Collector (`http://otel-collector:4317`) and Micrometer metrics via Actuator. This starter traces backend requests and the collection operation; native mobile spans are not yet instrumented or propagated, so add a React Native OpenTelemetry SDK and W3C Trace Context propagation as a later milestone if you need a single mobile-to-backend trace. Compose routes traces to Jaeger. Structured JSON logging is not enabled by default; logs are plain text in this starter. For an ELK exercise, use the optional `elk` Compose profile below.

```bash
docker compose -f deploy/compose.yaml --profile elk up -d elasticsearch logstash kibana
```

The ELK services and Logstash TCP/Beats inputs are provided as a learning scaffold; application stdout is not automatically shipped into ELK by default. Wire a JSON log appender or Filebeat/Fluent Bit to Logstash for ingestion. It is resource-heavy; do not start it on a low-memory machine. ELK is optional and separate from tracing.

## Kubernetes (local learning)

The manifests in `deploy/kubernetes/` are a starting point for Kind or another cluster. They include a PostgreSQL Deployment/PVC for practice; for real cloud production use a managed database, Kubernetes Secrets, TLS ingress, resource sizing, backups, and a proper image registry. Build and load/push your backend image before applying manifests.

```bash
# Example with Kind (install Kind and kubectl first)
kind create cluster --name vnexpress-news
# Build image from repository root
 docker build -f backend/Dockerfile -t vnexpress-news-backend:local backend
kind load docker-image vnexpress-news-backend:local --name vnexpress-news
kubectl apply -f deploy/kubernetes/
kubectl get pods,svc
kubectl port-forward svc/news-backend 8080:8080
```

The sample manifest uses a local image and is not a cloud production deployment. Update it for your registry and ingress before using it elsewhere.

## Build mobile binaries

For a development build, follow Expo's current instructions: https://docs.expo.dev/build/introduction/. For store submission, configure EAS, Apple Developer/App Store Connect and Google Play Console accounts, app identifiers, signing credentials, privacy declarations, and review requirements. A successful build does not guarantee store approval.

## Production checklist

- HTTPS for all public endpoints; no public unauthenticated admin endpoints
- Secrets in a secret manager, not source control
- Restrictive IAM/security groups and database private networking
- Request timeouts, retry/backoff, bounded polling, and upstream terms compliance
- Database backups and tested restore
- CI quality gates, dependency/image scanning, rollback strategy
- Alerting on failed collection, stale data, API error rate and latency
- Review data licensing, privacy policy, app-store metadata and publisher attribution
