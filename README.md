# CFB Pathfinder

CFB Pathfinder connects any two college football teams through a chain of game results. Choose a starting team and a target team, and the app finds the shortest path of wins linking them, showing how one team can be "better" than another through the teams they've beaten.

For example: **Alabama beat Kentucky beat Oklahoma beat Texas beat Ohio State**.

## How it works

- On startup, and every Monday at 6 AM Eastern, the backend pulls every completed FBS-vs-FBS game of the season from the [College Football Data API](https://collegefootballdata.com) and builds a graph where each team points to the teams it has beaten.
- A breadth-first search over that graph finds the shortest chain of wins from the starting team to the target team.
- The season defaults to 2026. Set `SEASON=<year>` in `.env` to change it.

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
| `GET /api/teams` | FBS team names, sorted A to Z. |
| `GET /api/chain?start=<team>&target=<team>` | The games in the shortest chain of wins, in order, or an empty list if none exists. Each game has `winner`, `loser`, `winnerPoints`, `loserPoints` and `date` (e.g. `2026-10-04`, US Eastern). |
| `GET /` | The full wins graph as JSON, for debugging. |

The frontend only talks to these endpoints, so the CFBD API key never leaves the server.
