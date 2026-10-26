# CFB Pathfinder

CFB Pathfinder connects any two college football teams through a chain of game results. Choose a starting team and a target team, and the app finds the shortest path of wins linking them, showing how one team can be "better" than another through the teams they've beaten.

For example: **Alabama beat Kentucky beat Oklahoma beat Texas beat Ohio State**.

## How it works

- On startup, the backend pulls every completed game of the current season from the [College Football Data API](https://collegefootballdata.com) and builds a graph where each team points to the teams it has beaten.
- A breadth-first search over that graph finds the shortest chain of wins from the starting team to the target team.

## Tech stack

- **Backend:** Java 25, Spring Boot (`server/`)
- **Frontend:** React + Vite (`client/`)

## Running locally

1. Get a free API key from [collegefootballdata.com/key](https://collegefootballdata.com/key).
2. Copy `.env.example` to `.env` in the project root and set `API_KEY`.
3. Start the backend (runs on port 8080):
   ```
   cd server
   ./mvnw spring-boot:run
   ```
4. In another terminal, start the frontend:
   ```
   cd client
   npm install
   npm run dev
   ```
5. Open the URL Vite prints (usually http://localhost:5173).

## API

| Endpoint | Description |
|---|---|
| `GET /api/path?start=<team>&end=<team>` | Returns `{"path": [...]}`, the shortest chain of wins, or an empty list if none exists. |
| `GET /` | Returns the full wins graph as JSON. |

Team names must match the API's spelling and capitalization exactly (e.g. `Ohio State`, `Texas A&M`).
