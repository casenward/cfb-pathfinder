import { useState } from "react";

function App() {
  const [startTeam, setStartTeam] = useState("");
  const [endTeam, setEndTeam] = useState("");
  const [path, setPath] = useState(null);
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setPath(null);

    try {
      const params = new URLSearchParams({ start: startTeam, end: endTeam });
      const res = await fetch(`/api/path?${params}`);
      if (!res.ok) {
        throw new Error(`Server responded with ${res.status}`);
      }
      const data = await res.json();
      setPath(data.path);
    } catch (err) {
      setError(`Could not reach the server: ${err.message}`);
    }
  }

  return (
    <div style={{ padding: 40 }}>
      <h1>CFB Pathfinder</h1>
      <p>
        CFB Pathfinder connects any two college football teams through a chain
        of game results. Choose a starting team and a target team, and the app
        finds the shortest path of wins linking them, showing how one team can
        be “better” than another through the teams they’ve beaten.
      </p>
      <form
        onSubmit={handleSubmit}
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 16,
        }}
      >
        <div style={{ display: "flex", gap: 16 }}>
          <label>
            Starting team
            <br />
            <input
              type="text"
              value={startTeam}
              onChange={(e) => setStartTeam(e.target.value)}
              placeholder="e.g. Alabama"
            />
          </label>
          <label>
            Ending team
            <br />
            <input
              type="text"
              value={endTeam}
              onChange={(e) => setEndTeam(e.target.value)}
              placeholder="e.g. Ohio State"
            />
          </label>
        </div>
        <button type="submit">Enter</button>
      </form>
      <div style={{ marginTop: 24, textAlign: "center" }}>
        {error && <p>{error}</p>}
        {path && path.length === 0 && (
          <p>
            No path of wins found from {startTeam} to {endTeam}.
          </p>
        )}
        {path && path.length > 0 && (
          <p>{path.join(" beat ")}</p>
        )}
      </div>
    </div>
  );
}

export default App;
