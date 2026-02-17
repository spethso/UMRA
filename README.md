# UMRA (Unified Medical Risk Analyzer)

Client-Server application for a unified medical risk analyzer.
The application does not compute the risks itself but acts as a unified interface for third-party analyzers.

- **Server**: Kotlin + Spring Boot (modulith-style package modules), GraphQL API
- **Client**: Vue 3 + Apollo Client + vue-graphiql
- **Deployment**: Docker Compose

## Current Analyzer Integration

- Integrated analyzers:
  - **PCPTRC** (<https://www.riskcalc.org/PCPTRC/>)
  - **SWOP RC2** (<https://www.prostatecancer-riskcalculator.com/2011/en/w2.html?v=2>)
- Client collects PCPTRC-required factors (race, age, PSA, family history, DRE, prior biopsy, optional biomarkers).
- Client can select one or more analyzers to run per analysis request.
- Server normalizes analyzer-specific outputs into a shared response model and returns aggregated risk values.
- Architecture supports adding multiple analyzers and returning an aggregated result.

### Online Forwarding Setting

- PCPTRC remote forwarding can be controlled via:
  - `umra.analyzers.pcptrc.online-forwarding-enabled` (default: `true`)

## Project Structure

- `server/`: Kotlin GraphQL server
- `client/`: Vue client app
- `docker-compose.yml`: full deployment stack

## Run with Docker Compose

From the repository root:

```bash
docker compose up --build
```

### URLs

- Client: <http://localhost:4173>
- GraphiQL (Vue): <http://localhost:4173/graphiql>

## Local Development (optional)

### Server

```bash
cd server
gradle bootRun
```

### Client

```bash
cd client
npm install
npm run dev
```
