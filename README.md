# UMRA (Unified Medical Risk Analyzer)

Client-Server application for a unified medical risk analyzer.
The application provides a unified interface for multiple risk analyzers and normalizes their outputs into a shared response.

- **Server**: Kotlin + Spring Boot (modulith-style package modules), GraphQL API
- **Client**: Vue 3 + Apollo Client + vue-graphiql
- **Deployment**: Docker Compose

## Current Analyzer Integration

- Integrated analyzers:
  - **PCPTRC** (`PCPTRC`) (<https://www.riskcalc.org/PCPTRC/>)
  - **SWOP Risk Calculator 2 (Using PSA Result)** (`SWOP_RC2`) (<https://www.prostatecancer-riskcalculator.com/2011/en/w2.html?v=2>)
  - **SWOP Risk Calculator 5 (Indolent vs aggressive)** (`SWOP_RC5`) (<https://www.prostatecancer-riskcalculator.com/2011/en/w6.html>)
  - **SWOP Future Risk Calculator (4-year risk)** (`SWOP_RC6`) (<https://www.prostatecancer-riskcalculator.com/2012/index.php>)
  - **UCLA PCRC-MRI (MRI-guided biopsy)** (`UCLA_PCRC_MRI`) (<https://www.uclahealth.org/departments/urology/iuo/research/prostate-cancer/risk-calculator-mri-guided-biopsy-pcrc-mri>)
- Client collects shared clinical factors and analyzer-specific optional factors (e.g., PCPTRC biomarkers, SWOP fields, UCLA MRI PI-RADS).
- Client can select one or more analyzers to run per analysis request.
- Server normalizes analyzer-specific outputs into a shared response model and returns aggregated risk values.
- Architecture supports adding multiple analyzers and returning an aggregated result.

### Analyzer Input Matrix

| Analyzer | ID | Additional required optional inputs | Notes |
|---|---|---|---|
| PCPTRC | `PCPTRC` | None strictly required beyond core factors | Optional: `% free PSA`, `PCA3`, `T2:ERG`, detailed family history, SNPs (with model constraints). |
| SWOP Risk Calculator 2 | `SWOP_RC2` | None | Uses core factors with PSA-focused model behavior. |
| SWOP Risk Calculator 5 | `SWOP_RC5` | `prostateVolumeCc`, `gleasonScoreLegacy`, `biopsyCancerLengthMm`, `biopsyBenignLengthMm` | Legacy indolent/aggressive model path. |
| SWOP Future Risk Calculator (4-year risk) | `SWOP_RC6` | None | Optional: `dreVolumeClassCc` (uses default class if omitted). |
| UCLA PCRC-MRI | `UCLA_PCRC_MRI` | `prostateVolumeCc`, `mriPiradsScore` | MRI-guided biopsy model; PI-RADS expected in 2..5. |

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
