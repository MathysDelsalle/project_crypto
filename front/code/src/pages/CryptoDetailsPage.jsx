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

const INTERVALS = [
  { label: "24h", days: 1 },
  { label: "7j", days: 7 },
  { label: "30j", days: 30 },
  { label: "90j", days: 90 },
  { label: "1 an", days: 365 },
  { label: "Max", days: "max" },
];

export default function CryptoDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

  const [allCryptos, setAllCryptos] = useState([]);

  const [crypto, setCrypto] = useState(null);
  const [history, setHistory] = useState([]);

  const [intervalDays, setIntervalDays] = useState(7);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // comparaison
  const [compareMode, setCompareMode] = useState(false);
  const [compareId, setCompareId] = useState("");
  const [compareCrypto, setCompareCrypto] = useState(null);
  const [compareHistory, setCompareHistory] = useState([]);
  const [compareLoading, setCompareLoading] = useState(false);

  // fetch liste (pour dropdown comparaison)
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

  // fetch crypto + historique
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        setLoading(true);
        setError(null);

        const [cryptoRes, histRes] = await Promise.all([
          fetch(`${API_BASE_URL}/cryptos/${id}`),
          fetch(`${API_BASE_URL}/cryptos/${id}/history?days=${intervalDays}`),
        ]);

        if (!cryptoRes.ok) throw new Error("Erreur crypto");
        if (!histRes.ok) throw new Error("Erreur historique");

        const cryptoData = await cryptoRes.json();
        const histData = await histRes.json();

        if (cancelled) return;

        setCrypto(cryptoData);
        setHistory(histData || []);
      } catch (e) {
        if (!cancelled) setError("Impossible de charger les détails de la crypto");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [API_BASE_URL, id, intervalDays]);

  // fetch comparaison quand sélectionnée
  useEffect(() => {
    if (!compareMode || !compareId) {
      setCompareCrypto(null);
      setCompareHistory([]);
      return;
    }

    let cancelled = false;

    (async () => {
      try {
        setCompareLoading(true);

        const [cryptoRes, histRes] = await Promise.all([
          fetch(`${API_BASE_URL}/cryptos/${compareId}`),
          fetch(`${API_BASE_URL}/cryptos/${compareId}/history?days=${intervalDays}`),
        ]);

        if (!cryptoRes.ok) throw new Error("Erreur compare crypto");
        if (!histRes.ok) throw new Error("Erreur compare historique");

        const cryptoData = await cryptoRes.json();
        const histData = await histRes.json();

        if (cancelled) return;

        setCompareCrypto(cryptoData);
        setCompareHistory(histData || []);
      } catch {
        if (!cancelled) {
          setCompareCrypto(null);
          setCompareHistory([]);
        }
      } finally {
        if (!cancelled) setCompareLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [API_BASE_URL, compareMode, compareId, intervalDays]);

  const title = crypto?.name ? `${crypto.name} (${crypto.symbol?.toUpperCase() || ""})` : "Détails crypto";

  const intervalLabel = useMemo(() => {
    const found = INTERVALS.find((x) => String(x.days) === String(intervalDays));
    return found?.label || "";
  }, [intervalDays]);

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
          {crypto?.imageUrl && <img src={crypto.imageUrl} alt={crypto.name} className="details-logo" />}
          <div>
            <div className="details-row">
              <span className="details-label">Prix actuel</span>
              <span className="details-value">{crypto?.currentPrice != null ? `${crypto.currentPrice} $` : "-"}</span>
            </div>
            <div className="details-row">
              <span className="details-label">Market cap</span>
              <span className="details-value">
                {crypto?.marketCap != null ? crypto.marketCap.toLocaleString("en-US") : "-"}
              </span>
            </div>
            <div className="details-row">
              <span className="details-label">Rang</span>
              <span className="details-value">{crypto?.marketCapRank != null ? `#${crypto.marketCapRank}` : "-"}</span>
            </div>
          </div>
        </div>

        <div className="details-controls">
          <div className="details-interval">
            <span className="details-label">Intervalle</span>
            <select value={String(intervalDays)} onChange={(e) => setIntervalDays(e.target.value)}>
              {INTERVALS.map((x) => (
                <option key={String(x.days)} value={String(x.days)}>
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
          <ResponsiveContainer width="100%" height={320}>
            <LineChart data={history}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                dataKey="timestamp"
                tickFormatter={(ts) => new Date(ts).toLocaleDateString("fr-FR")}
              />
              <YAxis tickFormatter={(v) => `${v}`} />
              <Tooltip
                labelFormatter={(ts) => new Date(ts).toLocaleString("fr-FR")}
                formatter={(value) => [`${value} $`, "Prix"]}
              />
              <Line type="monotone" dataKey="price" dot={false} />
            </LineChart>
          </ResponsiveContainer>
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
                    .filter((c) => c.id !== id)
                    .map((c) => (
                      <option key={c.id} value={c.id}>
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
                  Comparaison — {compareCrypto.name}
                </p>

                <div className="chart-box">
                  <ResponsiveContainer width="100%" height={320}>
                    <LineChart data={compareHistory}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis
                        dataKey="timestamp"
                        tickFormatter={(ts) => new Date(ts).toLocaleDateString("fr-FR")}
                      />
                      <YAxis tickFormatter={(v) => `${v}`} />
                      <Tooltip
                        labelFormatter={(ts) => new Date(ts).toLocaleString("fr-FR")}
                        formatter={(value) => [`${value} $`, "Prix"]}
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
