# UMRA (Unified Medical Risk Analyzer)

Client-Server application for a unified medical risk analyzer.
The application does not compute the risks itself but acts as a unified interface for third-party analyzers.

- **Server**: Kotlin + Spring Boot (modulith-style package modules), GraphQL API, GraphiQL enabled
- **Client**: Vue 3 + Apollo Client
- **Database**: PostgreSQL
- **Deployment**: Docker Compose

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
- GraphQL endpoint: <http://localhost:8080/graphql>
- GraphiQL: <http://localhost:8080/graphiql>

Try in GraphiQL:

```graphql
query {
  hello
}

mutation {
  addText(text: "First note")
}

query {
  texts
}
```

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
