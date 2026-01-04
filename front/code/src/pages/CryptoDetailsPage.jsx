import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import "../App.css";

/**
 * On limite les intervalles à :
 * - 1h
 * - 24h
 * - 7j
 *
 * Comme ton back renvoie une série "7 jours", on filtre côté front
 * en fonction du timestamp.
 */
const INTERVALS = [
  { label: "1h", value: "1h", ms: 1 * 60 * 60 * 1000 },
  { label: "24h", value: "24h", ms: 24 * 60 * 60 * 1000 },
  { label: "7j", value: "7d", ms: 7 * 24 * 60 * 60 * 1000 },
];

function formatTickDate(ms) {
  const d = new Date(ms);
  return d.toLocaleDateString("fr-FR", { day: "2-digit", month: "2-digit" });
}

function formatTickTime(ms) {
  const d = new Date(ms);
  return d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}

function formatTooltipLabel(ms) {
  const d = new Date(ms);
  return d.toLocaleString("fr-FR");
}

function formatPrice(v) {
  const n = Number(v);
  if (!Number.isFinite(n)) return "";
  if (Math.abs(n) >= 1000) return n.toLocaleString("en-US", { maximumFractionDigits: 0 });
  if (Math.abs(n) >= 1) return n.toLocaleString("en-US", { maximumFractionDigits: 2 });
  return n.toLocaleString("en-US", { maximumFractionDigits: 6 });
}

function computeYDomain(data, key = "price") {
  if (!Array.isArray(data) || data.length === 0) return ["auto", "auto"];

  const vals = data
    .map((p) => Number(p?.[key]))
    .filter((x) => Number.isFinite(x));

  if (vals.length === 0) return ["auto", "auto"];

  const min = Math.min(...vals);
  const max = Math.max(...vals);

  if (min === max) {
    const pad = Math.max(Math.abs(min) * 0.01, 1e-6);
    return [min - pad, max + pad];
  }

  const pad = Math.max((max - min) * 0.02, Math.abs(min) * 0.001);
  return [min - pad, max + pad];
}

function sortByTsAsc(points) {
  return [...points].sort((a, b) => Number(a.ts) - Number(b.ts));
}

export default function CryptoDetailsPage() {
  const { id } = useParams(); // externalId
  const navigate = useNavigate();

  const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

  const [allCryptos, setAllCryptos] = useState([]);

  const [crypto, setCrypto] = useState(null);

  // on garde l'historique brut (7 jours) tel que renvoyé par le back
  const [rawHistory, setRawHistory] = useState([]);

  // intervalle choisi (1h / 24h / 7j)
  const [interval, setInterval] = useState("7d");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // comparaison
  const [compareMode, setCompareMode] = useState(false);
  const [compareId, setCompareId] = useState("");
  const [compareCrypto, setCompareCrypto] = useState(null);
  const [compareRawHistory, setCompareRawHistory] = useState([]);
  const [compareLoading, setCompareLoading] = useState(false);

  const intervalMeta = useMemo(
    () => INTERVALS.find((x) => x.value === interval) || INTERVALS[2],
    [interval]
  );

  // fetch liste (dropdown comparaison)
  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/cryptos`);
        const data = await res.json();
        setAllCryptos(Array.isArray(data) ? data : []);
      } catch {
        // non bloquant
      }
    })();
  }, [API_BASE_URL]);

  // fetch crypto + historique (7j depuis BDD)
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        setLoading(true);
        setError(null);

        const listRes = await fetch(`${API_BASE_URL}/cryptos`);
        if (!listRes.ok) throw new Error("Erreur liste cryptos");
        const list = await listRes.json();

        const selected = Array.isArray(list)
          ? list.find((c) => c.externalId === id)
          : null;
        if (!selected) throw new Error("Crypto introuvable");

        const histRes = await fetch(`${API_BASE_URL}/crypto/${id}/history?vs=usd`);
        if (!histRes.ok) throw new Error("Erreur historique");
        const histData = await histRes.json();

        if (cancelled) return;

        setCrypto(selected);

        const normalized = Array.isArray(histData)
          ? histData
              .filter((p) => p && p.ts != null && p.price != null)
              .map((p) => ({ ts: Number(p.ts), price: Number(p.price) }))
          : [];

        setRawHistory(sortByTsAsc(normalized));
      } catch (e) {
        if (!cancelled) setError("Impossible de charger les détails de la crypto");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [API_BASE_URL, id]);

  // fetch comparaison (7j depuis BDD)
  useEffect(() => {
    if (!compareMode || !compareId) {
      setCompareCrypto(null);
      setCompareRawHistory([]);
      return;
    }

    let cancelled = false;

    (async () => {
      try {
        setCompareLoading(true);

        const selectedCompare = allCryptos.find((c) => c.externalId === compareId) || null;
        if (selectedCompare) setCompareCrypto(selectedCompare);

        const histRes = await fetch(`${API_BASE_URL}/crypto/${compareId}/history?vs=usd`);
        if (!histRes.ok) throw new Error("Erreur compare historique");
        const histData = await histRes.json();

        if (cancelled) return;

        const normalized = Array.isArray(histData)
          ? histData
              .filter((p) => p && p.ts != null && p.price != null)
              .map((p) => ({ ts: Number(p.ts), price: Number(p.price) }))
          : [];

        setCompareRawHistory(sortByTsAsc(normalized));
      } catch {
        if (!cancelled) {
          setCompareCrypto(null);
          setCompareRawHistory([]);
        }
      } finally {
        if (!cancelled) setCompareLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [API_BASE_URL, compareMode, compareId, allCryptos]);

  /**
   * ✅ Filtrage côté front selon l'intervalle choisi
   * On garde uniquement les points >= now - interval.ms
   * et si ça donne 0 point (ex: pas assez de données),
   * on retombe sur la série brute.
   */
  const history = useMemo(() => {
    const now = Date.now();
    const cut = now - intervalMeta.ms;

    const filtered = rawHistory.filter((p) => p.ts >= cut);
    return filtered.length > 0 ? filtered : rawHistory;
  }, [rawHistory, intervalMeta.ms]);

  const compareHistory = useMemo(() => {
    if (!compareRawHistory?.length) return [];
    const now = Date.now();
    const cut = now - intervalMeta.ms;

    const filtered = compareRawHistory.filter((p) => p.ts >= cut);
    return filtered.length > 0 ? filtered : compareRawHistory;
  }, [compareRawHistory, intervalMeta.ms]);

  // ✅ Y-domain “zoom”
  const yDomain = useMemo(() => computeYDomain(history, "price"), [history]);
  const compareYDomain = useMemo(() => computeYDomain(compareHistory, "price"), [compareHistory]);

  // ✅ Axe X : temps pour 1h/24h, date pour 7j
  const xTickFormatter = useMemo(() => {
    return interval === "7d" ? formatTickDate : formatTickTime;
  }, [interval]);

  const title = crypto?.name
    ? `${crypto.name} (${crypto.symbol?.toUpperCase() || ""})`
    : "Détails crypto";

  const intervalLabel = intervalMeta.label;

  if (loading) return <div className="app">Chargement...</div>;
  if (error) return <div className="app error">{error}</div>;

  return (
    <div className="app">
      <header className="app-header">
        <button className="page-btn" onClick={() => navigate(-1)}>
          ← Retour
        </button>

        <h1 style={{ marginTop: "12px" }}>{title}</h1>

        <div className="details-meta">
          {crypto?.imageUrl && (
            <img src={crypto.imageUrl} alt={crypto.name} className="details-logo" />
          )}
          <div>
            <div className="details-row">
              <span className="details-label">Prix actuel</span>
              <span className="details-value">
                {crypto?.currentPrice != null ? `${formatPrice(crypto.currentPrice)} $` : "-"}
              </span>
            </div>
            <div className="details-row">
              <span className="details-label">Market cap</span>
              <span className="details-value">
                {crypto?.marketCap != null ? crypto.marketCap.toLocaleString("en-US") : "-"}
              </span>
            </div>
            <div className="details-row">
              <span className="details-label">Rang</span>
              <span className="details-value">
                {crypto?.marketCapRank != null ? `#${crypto.marketCapRank}` : "-"}
              </span>
            </div>
          </div>
        </div>

        <div className="details-controls">
          <div className="details-interval">
            <span className="details-label">Intervalle</span>
            <select value={interval} onChange={(e) => setInterval(e.target.value)}>
              {INTERVALS.map((x) => (
                <option key={x.value} value={x.value}>
                  {x.label}
                </option>
              ))}
            </select>
          </div>

          <button
            className="page-btn"
            onClick={() => {
              setCompareMode((v) => !v);
              setCompareId("");
            }}
          >
            {compareMode ? "Fermer comparaison" : "Comparer à une crypto"}
          </button>
        </div>

        <p className="details-subtitle">Évolution du prix — {intervalLabel}</p>
      </header>

      <div className="card">
        <div className="chart-box">
          {history.length === 0 ? (
            <div className="app">Pas encore de données d’historique (attends le bootstrap).</div>
          ) : (
            <ResponsiveContainer width="100%" height={320}>
              <LineChart data={history}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="ts" tickFormatter={xTickFormatter} />
                <YAxis domain={yDomain} tickFormatter={formatPrice} />
                <Tooltip
                  labelFormatter={(ts) => formatTooltipLabel(ts)}
                  formatter={(value) => [`${formatPrice(value)} $`, "Prix"]}
                />
                <Line type="monotone" dataKey="price" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {compareMode && (
        <div className="card" style={{ marginTop: "12px" }}>
          <div className="compare-box">
            <div className="compare-top">
              <div className="details-interval">
                <span className="details-label">Crypto à comparer</span>
                <select value={compareId} onChange={(e) => setCompareId(e.target.value)}>
                  <option value="">— Choisir —</option>
                  {allCryptos
                    .filter((c) => c.externalId !== id)
                    .map((c) => (
                      <option key={c.externalId} value={c.externalId}>
                        {c.name} ({c.symbol?.toUpperCase()})
                      </option>
                    ))}
                </select>
              </div>

              {compareLoading && <span className="compare-loading">Chargement…</span>}
            </div>

            {compareCrypto && compareHistory?.length > 0 && (
              <>
                <p className="details-subtitle" style={{ marginTop: "10px" }}>
                  Comparaison — {compareCrypto.name} — {intervalLabel}
                </p>

                <div className="chart-box">
                  <ResponsiveContainer width="100%" height={320}>
                    <LineChart data={compareHistory}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="ts" tickFormatter={xTickFormatter} />
                      <YAxis domain={compareYDomain} tickFormatter={formatPrice} />
                      <Tooltip
                        labelFormatter={(ts) => formatTooltipLabel(ts)}
                        formatter={(value) => [`${formatPrice(value)} $`, "Prix"]}
                      />
                      <Line type="monotone" dataKey="price" dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </>
            )}

            {compareId && !compareLoading && (!compareHistory || compareHistory.length === 0) && (
              <p className="error" style={{ marginTop: "10px" }}>
                Impossible de charger l’historique de comparaison.
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
