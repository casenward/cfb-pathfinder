import { useEffect, useRef, useState } from "react";
import "./App.css";

const TEAMS_ERROR = "Couldn't load teams. Refresh the page to try again.";
const SAME_TEAM = "Pick two different teams.";
const NO_CHAIN = "No chain of wins connects these teams.";
const SERVER_ERROR = "Couldn't reach the server. Try again.";

// Formats a date like 2026-10-04 as "Oct 4, 2026" (or "October 4, 2026" with month "long").
function formatDate(isoDate, month = "short") {
  const [year, monthIndex, day] = isoDate.split("-").map(Number);
  return new Date(year, monthIndex - 1, day).toLocaleDateString("en-US", {
    month,
    day: "numeric",
    year: "numeric",
  });
}

function describeGame(game) {
  return `${game.winner} beat ${game.loser} ${game.winnerPoints} to ${game.loserPoints} on ${formatDate(game.date, "long")}.`;
}

function gameCount(n) {
  return `${n} ${n === 1 ? "game" : "games"}`;
}

function App() {
  const [teams, setTeams] = useState([]);
  const [teamsStatus, setTeamsStatus] = useState("loading"); // loading | ready | error
  const [startTeam, setStartTeam] = useState("");
  const [targetTeam, setTargetTeam] = useState("");
  const [searchStatus, setSearchStatus] = useState("idle"); // idle | searching | done | error
  const [result, setResult] = useState(null);
  const [announcement, setAnnouncement] = useState("");
  const latestSearch = useRef(0);

  useEffect(() => {
    let cancelled = false;
    fetch("/api/teams")
      .then((res) => {
        if (!res.ok) throw new Error(`Server responded with ${res.status}`);
        return res.json();
      })
      .then((data) => {
        if (cancelled) return;
        if (data.length === 0) throw new Error("No teams returned");
        setTeams(data);
        setTeamsStatus("ready");
      })
      .catch(() => {
        if (cancelled) return;
        setTeamsStatus("error");
        setAnnouncement(TEAMS_ERROR);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sameTeam = startTeam !== "" && startTeam === targetTeam;
  const canSearch =
    teamsStatus === "ready" &&
    startTeam !== "" &&
    targetTeam !== "" &&
    !sameTeam &&
    searchStatus !== "searching";

  // Changing either dropdown clears the previous result and ignores any search still in flight.
  function changeTeam(setTeam, value, otherValue) {
    setTeam(value);
    latestSearch.current++;
    setSearchStatus("idle");
    setResult(null);
    setAnnouncement(value !== "" && value === otherValue ? SAME_TEAM : "");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!canSearch) return;

    const searchId = ++latestSearch.current;
    const start = startTeam;
    const target = targetTeam;
    setSearchStatus("searching");
    setResult(null);
    setAnnouncement("");

    try {
      const params = new URLSearchParams({ start, target });
      const res = await fetch(`/api/chain?${params}`);
      if (!res.ok) throw new Error(`Server responded with ${res.status}`);
      const games = await res.json();
      if (searchId !== latestSearch.current) return;

      setResult({ start, target, games });
      setSearchStatus("done");
      setAnnouncement(
        games.length === 0
          ? `${start} to ${target}. ${NO_CHAIN}`
          : `${start} to ${target}, ${gameCount(games.length)}. ${games.map(describeGame).join(" ")}`,
      );
    } catch {
      if (searchId !== latestSearch.current) return;
      setSearchStatus("error");
      setAnnouncement(SERVER_ERROR);
    }
  }

  const placeholder = teamsStatus === "loading" ? "Loading teams" : "Choose a team";

  function teamSelect(id, label, value, setValue, otherValue) {
    return (
      <div className="select-bar slant">
        <div className="unslant">
          <label htmlFor={id}>{label}</label>
          <select
            id={id}
            value={value}
            disabled={teamsStatus !== "ready"}
            onChange={(e) => changeTeam(setValue, e.target.value, otherValue)}
          >
            <option value="">{placeholder}</option>
            {teams.map((team) => (
              <option key={team} value={team}>
                {team}
              </option>
            ))}
          </select>
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      <header>
        <div className="logo slant">
          <span className="unslant">CFB Pathfinder</span>
        </div>
      </header>

      <main>
        <section className="intro">
          <h1>Who beat who beat who?</h1>
          <p>Pick two teams. We'll find the shortest chain of wins from one to the other.</p>
        </section>

        <form className="picker" onSubmit={handleSubmit}>
          <div className="picker-row">
            {teamSelect("start-team", "Starting team", startTeam, setStartTeam, targetTeam)}
            <span className="vs" aria-hidden="true">
              vs
            </span>
            {teamSelect("target-team", "Target team", targetTeam, setTargetTeam, startTeam)}
          </div>
          <p className="note">FBS teams only.</p>
          {teamsStatus === "error" && <p className="picker-message">{TEAMS_ERROR}</p>}
          {sameTeam && <p className="picker-message">{SAME_TEAM}</p>}
          <button type="submit" className="find-button slant" disabled={!canSearch}>
            <span className="unslant">
              {searchStatus === "searching" ? "Finding chain" : "Find the chain"}
            </span>
          </button>
        </form>

        {searchStatus === "error" && (
          <section className="results">
            <p className="results-message">{SERVER_ERROR}</p>
          </section>
        )}

        {searchStatus === "done" && result && (
          <section className="results" aria-labelledby="results-heading">
            <div className="results-heading">
              <h2 id="results-heading">
                {result.start} to {result.target}
              </h2>
              {result.games.length > 0 && (
                <span className="game-count">{gameCount(result.games.length)}</span>
              )}
            </div>

            {result.games.length === 0 ? (
              <p className="results-message">{NO_CHAIN}</p>
            ) : (
              <div
                className="chain-scroll"
                tabIndex={0}
                role="region"
                aria-label={`Chain of wins from ${result.start} to ${result.target}`}
              >
                <ol className="chain">
                  {result.games.map((game, i) => (
                    <li className="link" key={i} style={{ "--i": i }}>
                      <span className="sr-only">{describeGame(game)}</span>
                      <span className="team-bar slant" aria-hidden="true">
                        <span className="unslant">{game.winner}</span>
                      </span>
                      <span className="arrow" aria-hidden="true">
                        <span className="score">
                          {game.winnerPoints}-{game.loserPoints}
                        </span>
                        <span className="arrow-line" />
                        <span className="date">{formatDate(game.date)}</span>
                      </span>
                    </li>
                  ))}
                  <li className="link" style={{ "--i": result.games.length }} aria-hidden="true">
                    <span className="team-bar team-bar--target slant">
                      <span className="unslant">{result.target}</span>
                    </span>
                  </li>
                </ol>
              </div>
            )}
          </section>
        )}

        <div className="sr-only" aria-live="polite">
          {announcement}
        </div>
      </main>
    </div>
  );
}

export default App;
