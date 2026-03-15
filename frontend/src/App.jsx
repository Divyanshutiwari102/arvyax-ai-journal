
import { useState, useEffect, useCallback } from "react";

const AMBIENCES = ["forest", "ocean", "mountain", "rain", "desert"];

const API = {
  async createEntry(userId, ambience, text) {
    const r = await fetch("/api/journal", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId, ambience, text }),
    });
    if (!r.ok) throw new Error((await r.json()).error || "Failed to save entry");
    return r.json();
  },
  async getEntries(userId) {
    const r = await fetch(`/api/journal/${userId}`);
    if (!r.ok) throw new Error("Failed to fetch entries");
    return r.json();
  },
  async analyzeText(text) {
    const r = await fetch("/api/journal/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text }),
    });
    if (!r.ok) throw new Error((await r.json()).error || "Analysis failed");
    return r.json();
  },
  async analyzeEntry(entryId) {
    const r = await fetch(`/api/journal/analyze/${entryId}`, { method: "POST" });
    if (!r.ok) throw new Error((await r.json()).error || "Analysis failed");
    return r.json();
  },
  async getInsights(userId) {
    const r = await fetch(`/api/journal/insights/${userId}`);
    if (!r.ok) throw new Error("Failed to fetch insights");
    return r.json();
  },
};

const EMOTION_COLORS = {
  calm: "#4ade80", happy: "#facc15", sad: "#60a5fa", anxious: "#f87171",
  peaceful: "#34d399", excited: "#fb923c", reflective: "#a78bfa",
  grateful: "#f472b6", unknown: "#94a3b8",
};

function EmotionBadge({ emotion }) {
  const color = EMOTION_COLORS[emotion?.toLowerCase()] || EMOTION_COLORS.unknown;
  return (
    <span style={{
      background: color + "33", color: color, border: `1px solid ${color}66`,
      borderRadius: 12, padding: "2px 10px", fontSize: 12, fontWeight: 600,
      textTransform: "capitalize"
    }}>{emotion || "unanalyzed"}</span>
  );
}

function EntryCard({ entry, onAnalyze, analyzing }) {
  const [expanded, setExpanded] = useState(false);
  const kws = entry.keywords ? (() => { try { return JSON.parse(entry.keywords); } catch { return []; } })() : [];

  return (
    <div style={{
      background: "#1e293b", border: "1px solid #334155", borderRadius: 12,
      padding: 16, marginBottom: 12
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <span style={{ background: "#0f172a", border: "1px solid #475569", borderRadius: 8, padding: "2px 10px", fontSize: 12, color: "#94a3b8" }}>
            🌿 {entry.ambience}
          </span>
          <EmotionBadge emotion={entry.emotion} />
        </div>
        <span style={{ color: "#64748b", fontSize: 12 }}>
          {new Date(entry.createdAt).toLocaleDateString()}
        </span>
      </div>

      <p style={{ color: "#cbd5e1", margin: "8px 0", cursor: "pointer", lineHeight: 1.6 }} onClick={() => setExpanded(!expanded)}>
        {expanded ? entry.text : entry.text.slice(0, 120) + (entry.text.length > 120 ? "…" : "")}
      </p>

      {entry.summary && (
        <p style={{ color: "#94a3b8", fontStyle: "italic", fontSize: 13, margin: "6px 0" }}>
          💡 {entry.summary}
        </p>
      )}

      {kws.length > 0 && (
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap", marginTop: 6 }}>
          {kws.map((k, i) => (
            <span key={i} style={{ background: "#0f172a", color: "#7dd3fc", fontSize: 11, borderRadius: 6, padding: "1px 8px" }}>#{k}</span>
          ))}
        </div>
      )}

      {!entry.emotion && (
        <button
          onClick={() => onAnalyze(entry.id)}
          disabled={analyzing === entry.id}
          style={{
            marginTop: 10, background: "#7c3aed", color: "#fff", border: "none",
            borderRadius: 8, padding: "6px 14px", cursor: "pointer", fontSize: 13
          }}
        >
          {analyzing === entry.id ? "Analyzing…" : "🔍 Analyze"}
        </button>
      )}
    </div>
  );
}

function InsightsPanel({ insights }) {
  if (!insights) return null;
  return (
    <div style={{ background: "#1e293b", border: "1px solid #334155", borderRadius: 12, padding: 20 }}>
      <h3 style={{ color: "#f1f5f9", margin: "0 0 16px" }}>📊 Your Insights</h3>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
        <div style={{ background: "#0f172a", borderRadius: 8, padding: 12, textAlign: "center" }}>
          <div style={{ fontSize: 28, fontWeight: 700, color: "#7dd3fc" }}>{insights.totalEntries}</div>
          <div style={{ color: "#94a3b8", fontSize: 12 }}>Total Entries</div>
        </div>
        <div style={{ background: "#0f172a", borderRadius: 8, padding: 12, textAlign: "center" }}>
          <div style={{ marginBottom: 4 }}><EmotionBadge emotion={insights.topEmotion} /></div>
          <div style={{ color: "#94a3b8", fontSize: 12 }}>Top Emotion</div>
        </div>
        <div style={{ background: "#0f172a", borderRadius: 8, padding: 12, textAlign: "center" }}>
          <div style={{ fontSize: 20, fontWeight: 700, color: "#4ade80", textTransform: "capitalize" }}>🌿 {insights.mostUsedAmbience}</div>
          <div style={{ color: "#94a3b8", fontSize: 12 }}>Fav Ambience</div>
        </div>
        <div style={{ background: "#0f172a", borderRadius: 8, padding: 12 }}>
          <div style={{ color: "#94a3b8", fontSize: 12, marginBottom: 6 }}>Recent Keywords</div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
            {(insights.recentKeywords || []).map((k, i) => (
              <span key={i} style={{ background: "#1e293b", color: "#7dd3fc", fontSize: 11, borderRadius: 6, padding: "1px 7px" }}>#{k}</span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const [userId, setUserId] = useState("user_123");
  const [tab, setTab] = useState("write"); // write | entries | insights
  const [ambience, setAmbience] = useState("forest");
  const [text, setText] = useState("");
  const [entries, setEntries] = useState([]);
  const [insights, setInsights] = useState(null);
  const [saving, setSaving] = useState(false);
  const [analyzing, setAnalyzing] = useState(null); // entryId being analyzed
  const [quickAnalysis, setQuickAnalysis] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadEntries = useCallback(async () => {
    try {
      const data = await API.getEntries(userId);
      setEntries(data);
    } catch (e) { setError(e.message); }
  }, [userId]);

  const loadInsights = useCallback(async () => {
    try {
      const data = await API.getInsights(userId);
      setInsights(data);
    } catch (e) { setError(e.message); }
  }, [userId]);

  useEffect(() => { if (tab === "entries") loadEntries(); }, [tab, loadEntries]);
  useEffect(() => { if (tab === "insights") loadInsights(); }, [tab, loadInsights]);

  async function handleSave() {
    if (!text.trim()) return;
    setSaving(true); setError(""); setSuccess("");
    try {
      await API.createEntry(userId, ambience, text);
      setSuccess("Entry saved!");
      setText("");
      setQuickAnalysis(null);
    } catch (e) { setError(e.message); }
    finally { setSaving(false); }
  }

  async function handleQuickAnalyze() {
    if (!text.trim()) return;
    setQuickAnalysis(null); setError("");
    try {
      const result = await API.analyzeText(text);
      setQuickAnalysis(result);
    } catch (e) { setError(e.message); }
  }

  async function handleAnalyzeEntry(entryId) {
    setAnalyzing(entryId); setError("");
    try {
      const updated = await API.analyzeEntry(entryId);
      setEntries(prev => prev.map(e => e.id === entryId ? updated : e));
    } catch (e) { setError(e.message); }
    finally { setAnalyzing(null); }
  }

  const tabStyle = (t) => ({
    background: tab === t ? "#7c3aed" : "#1e293b",
    color: tab === t ? "#fff" : "#94a3b8",
    border: "1px solid " + (tab === t ? "#7c3aed" : "#334155"),
    borderRadius: 8, padding: "8px 20px", cursor: "pointer", fontWeight: 600, fontSize: 14
  });

  return (
    <div style={{ minHeight: "100vh", background: "#0f172a", color: "#f1f5f9", fontFamily: "system-ui, sans-serif" }}>
      <div style={{ maxWidth: 720, margin: "0 auto", padding: "24px 16px" }}>
        {/* Header */}
        <div style={{ marginBottom: 24, borderBottom: "1px solid #1e293b", paddingBottom: 16 }}>
          <h1 style={{ margin: 0, fontSize: 24, fontWeight: 700, color: "#f1f5f9" }}>
            🌿 ArvyaX Journal
          </h1>
          <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 8 }}>
            <label style={{ color: "#64748b", fontSize: 13 }}>User ID:</label>
            <input
              value={userId}
              onChange={e => setUserId(e.target.value)}
              style={{ background: "#1e293b", border: "1px solid #334155", borderRadius: 6, padding: "4px 10px", color: "#f1f5f9", fontSize: 13, width: 160 }}
            />
          </div>
        </div>

        {/* Tabs */}
        <div style={{ display: "flex", gap: 8, marginBottom: 20 }}>
          <button style={tabStyle("write")} onClick={() => setTab("write")}>✍️ Write</button>
          <button style={tabStyle("entries")} onClick={() => setTab("entries")}>📋 Entries</button>
          <button style={tabStyle("insights")} onClick={() => setTab("insights")}>📊 Insights</button>
        </div>

        {/* Messages */}
        {error && <div style={{ background: "#450a0a", border: "1px solid #f87171", borderRadius: 8, padding: "10px 14px", marginBottom: 12, color: "#fca5a5", fontSize: 13 }}>⚠️ {error}</div>}
        {success && <div style={{ background: "#052e16", border: "1px solid #4ade80", borderRadius: 8, padding: "10px 14px", marginBottom: 12, color: "#86efac", fontSize: 13 }}>✅ {success}</div>}

        {/* WRITE TAB */}
        {tab === "write" && (
          <div>
            <div style={{ marginBottom: 14 }}>
              <label style={{ color: "#94a3b8", fontSize: 13, display: "block", marginBottom: 6 }}>Ambience</label>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                {AMBIENCES.map(a => (
                  <button key={a} onClick={() => setAmbience(a)} style={{
                    background: ambience === a ? "#164e63" : "#1e293b",
                    border: "1px solid " + (ambience === a ? "#7dd3fc" : "#334155"),
                    color: ambience === a ? "#7dd3fc" : "#94a3b8",
                    borderRadius: 8, padding: "6px 14px", cursor: "pointer", textTransform: "capitalize", fontSize: 13
                  }}>{a}</button>
                ))}
              </div>
            </div>

            <div style={{ marginBottom: 14 }}>
              <label style={{ color: "#94a3b8", fontSize: 13, display: "block", marginBottom: 6 }}>Journal Entry</label>
              <textarea
                value={text}
                onChange={e => { setText(e.target.value); setQuickAnalysis(null); setSuccess(""); }}
                placeholder="How did you feel during your nature session today?"
                rows={6}
                style={{
                  width: "100%", background: "#1e293b", border: "1px solid #334155", borderRadius: 10,
                  padding: 14, color: "#f1f5f9", fontSize: 15, resize: "vertical", lineHeight: 1.6,
                  boxSizing: "border-box"
                }}
              />
            </div>

            <div style={{ display: "flex", gap: 10 }}>
              <button onClick={handleSave} disabled={saving || !text.trim()} style={{
                background: "#16a34a", color: "#fff", border: "none", borderRadius: 8,
                padding: "10px 22px", cursor: "pointer", fontWeight: 600, fontSize: 14,
                opacity: (!text.trim() || saving) ? 0.5 : 1
              }}>{saving ? "Saving…" : "💾 Save Entry"}</button>

              <button onClick={handleQuickAnalyze} disabled={!text.trim()} style={{
                background: "#7c3aed", color: "#fff", border: "none", borderRadius: 8,
                padding: "10px 22px", cursor: "pointer", fontWeight: 600, fontSize: 14,
                opacity: !text.trim() ? 0.5 : 1
              }}>🔍 Analyze</button>
            </div>

            {/* Quick Analysis Result */}
            {quickAnalysis && (
              <div style={{ background: "#1e293b", border: "1px solid #7c3aed44", borderRadius: 12, padding: 16, marginTop: 16 }}>
                <h4 style={{ margin: "0 0 12px", color: "#c4b5fd" }}>Analysis Result</h4>
                <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 8 }}>
                  <span style={{ color: "#94a3b8", fontSize: 13 }}>Emotion:</span>
                  <EmotionBadge emotion={quickAnalysis.emotion} />
                </div>
                <p style={{ color: "#94a3b8", fontStyle: "italic", fontSize: 13, margin: "0 0 10px" }}>
                  💡 {quickAnalysis.summary}
                </p>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                  {(quickAnalysis.keywords || []).map((k, i) => (
                    <span key={i} style={{ background: "#0f172a", color: "#7dd3fc", fontSize: 11, borderRadius: 6, padding: "1px 8px" }}>#{k}</span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* ENTRIES TAB */}
        {tab === "entries" && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
              <h3 style={{ margin: 0, color: "#94a3b8" }}>{entries.length} entries</h3>
              <button onClick={loadEntries} style={{ background: "#1e293b", border: "1px solid #334155", borderRadius: 8, padding: "6px 14px", color: "#94a3b8", cursor: "pointer", fontSize: 13 }}>↻ Refresh</button>
            </div>
            {entries.length === 0 ? (
              <p style={{ color: "#475569", textAlign: "center", marginTop: 40 }}>No entries yet. Start writing! 🌿</p>
            ) : (
              entries.map(e => <EntryCard key={e.id} entry={e} onAnalyze={handleAnalyzeEntry} analyzing={analyzing} />)
            )}
          </div>
        )}

        {/* INSIGHTS TAB */}
        {tab === "insights" && (
          <div>
            <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 16 }}>
              <button onClick={loadInsights} style={{ background: "#1e293b", border: "1px solid #334155", borderRadius: 8, padding: "6px 14px", color: "#94a3b8", cursor: "pointer", fontSize: 13 }}>↻ Refresh</button>
            </div>
            {insights ? <InsightsPanel insights={insights} /> : (
              <p style={{ color: "#475569", textAlign: "center", marginTop: 40 }}>Loading insights…</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
