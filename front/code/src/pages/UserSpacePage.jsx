import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
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
import { getToken } from "../services/authService";

const VS = "usd";

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
  return new Date(ms).toLocaleString("fr-FR");
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
  const vals = data.map((p) => Number(p?.[key])).filter((x) => Number.isFinite(x));
  if (!vals.length) return ["auto", "auto"];
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

/** Modal très simple, sans lib */
function Modal({ open, title, children, onClose }) {
  if (!open) return null;

  return (
    <div
      onMouseDown={onClose}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.55)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 16,
        zIndex: 9999,
      }}
    >
      <div
        onMouseDown={(e) => e.stopPropagation()}
        className="card"
        style={{
          width: "min(560px, 100%)",
          margin: 0,
          boxShadow: "0 10px 30px rgba(0,0,0,0.35)",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}>
          <h2 style={{ margin: 0 }}>{title}</h2>
          <button className="page-btn" onClick={onClose}>
            ✕
          </button>
        </div>
        <div style={{ marginTop: 12 }}>{children}</div>
      </div>
    </div>
  );
}

// --- petit helper fetch ---
async function readErrorMessage(res) {
  try {
    const ct = res.headers.get("content-type") || "";
    if (ct.includes("application/json")) {
      const j = await res.json();
      return j?.message || j?.error || JSON.stringify(j);
    }
    const t = await res.text();
    return t || `HTTP ${res.status}`;
  } catch {
    return `HTTP ${res.status}`;
  }
}

export default function UserSpacePage() {
  const navigate = useNavigate();
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

  // ✅ IMPORTANT: on lit le token UNE seule fois au montage
  const token = useMemo(() => getToken(), []);

  const [balance, setBalance] = useState(0);
  const [amountToAdd, setAmountToAdd] = useState("");

  const [favorites, setFavorites] = useState([]); // externalId[]
  const [allCryptos, setAllCryptos] = useState([]);

  // ✅ holdings: { externalId: qty }
  const [holdingsMap, setHoldingsMap] = useState({});

  const [interval, setInterval] = useState("7d");
  const intervalMeta = useMemo(
    () => INTERVALS.find((x) => x.value === interval) || INTERVALS[2],
    [interval]
  );

  const [loading, setLoading] = useState(true);
  const [historyMap, setHistoryMap] = useState({}); // { externalId: [{ts,price}] }

  // ✅ Alertes: map externalId -> true (pour activer/désactiver le bouton "Supprimer alerte")
  const [alertsMap, setAlertsMap] = useState({});

  // ✅ Modal alerte
  const [alertOpen, setAlertOpen] = useState(false);
  const [alertCrypto, setAlertCrypto] = useState(null);
  const [thresholdHigh, setThresholdHigh] = useState("");
  const [thresholdLow, setThresholdLow] = useState("");
  const [alertError, setAlertError] = useState("");
  const [pageError, setPageError] = useState("");

  function authHeaders() {
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  // ✅ refresh holdings
  const refreshHoldings = async () => {
    if (!token) return;
    const res = await fetch(`${API_BASE_URL}/me/holdings`, { headers: { ...authHeaders() } });
    if (!res.ok) return;
    const data = await res.json();
    const map = {};
    (Array.isArray(data) ? data : []).forEach((h) => {
      if (h?.externalId) map[h.externalId] = Number(h.quantity || 0);
    });
    setHoldingsMap(map);
  };

  const buyOne = async (crypto) => {
    if (!token) {
      setPageError("Tu dois être connecté pour acheter.");
      return;
    }

    const price = Number(crypto?.currentPrice || 0);
    if (!Number.isFinite(price) || price <= 0) {
      setPageError("Prix indisponible.");
      return;
    }
    if (balance < price) {
      setPageError("Solde insuffisant.");
      return;
    }

    try {
      setPageError("");
      const res = await fetch(`${API_BASE_URL}/me/trade/buy/${crypto.externalId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders() },
        body: JSON.stringify({ qty: 1 }),
      });

      if (!res.ok) throw new Error(await readErrorMessage(res));
      const data = await res.json();

      setBalance(Number(data?.balance ?? balance));
      await refreshHoldings();
    } catch (e) {
      setPageError(e?.message || "Erreur achat");
    }
  };

  const sellOne = async (crypto) => {
    if (!token) {
      setPageError("Tu dois être connecté pour vendre.");
      return;
    }

    const owned = Number(holdingsMap[crypto.externalId] || 0);
    if (owned <= 0) {
      setPageError("Tu ne possèdes pas cette crypto.");
      return;
    }

    try {
      setPageError("");
      const res = await fetch(`${API_BASE_URL}/me/trade/sell/${crypto.externalId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders() },
        body: JSON.stringify({ qty: 1 }),
      });

      if (!res.ok) throw new Error(await readErrorMessage(res));
      const data = await res.json();

      setBalance(Number(data?.balance ?? balance));
      await refreshHoldings();
    } catch (e) {
      setPageError(e?.message || "Erreur vente");
    }
  };

  // Charger solde + favoris + holdings + liste cryptos + mes alertes
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        setLoading(true);
        setPageError("");

        // 🔒 si pas de token, on évite d'appeler /me/*
        const balPromise = token
          ? fetch(`${API_BASE_URL}/me/balance`, { headers: { ...authHeaders() } })
          : Promise.resolve(null);

        const favPromise = token
          ? fetch(`${API_BASE_URL}/me/favorites`, { headers: { ...authHeaders() } })
          : Promise.resolve(null);

        const alertsPromise = token
          ? fetch(`${API_BASE_URL}/me/alerts`, { headers: { ...authHeaders() } })
          : Promise.resolve(null);

        const holdingsPromise = token
          ? fetch(`${API_BASE_URL}/me/holdings`, { headers: { ...authHeaders() } })
          : Promise.resolve(null);

        const listPromise = fetch(`${API_BASE_URL}/cryptos`);

        const [balRes, favRes, alertsRes, holdingsRes, listRes] = await Promise.all([
          balPromise,
          favPromise,
          alertsPromise,
          holdingsPromise,
          listPromise,
        ]);

        if (!listRes || !listRes.ok) throw new Error(await readErrorMessage(listRes));
        const listData = await listRes.json();

        if (cancelled) return;

        setAllCryptos(Array.isArray(listData) ? listData : []);

        if (token && balRes && favRes) {
          if (!balRes.ok) throw new Error(await readErrorMessage(balRes));
          if (!favRes.ok) throw new Error(await readErrorMessage(favRes));

          const balData = await balRes.json();
          const favData = await favRes.json();

          if (cancelled) return;

          setBalance(Number(balData?.balance ?? 0) || 0);
          setFavorites(Array.isArray(favData) ? favData : []);
        } else {
          // pas connecté: valeurs par défaut
          setBalance(0);
          setFavorites([]);
        }

        // ✅ holdings
        if (token && holdingsRes) {
          if (!holdingsRes.ok) throw new Error(await readErrorMessage(holdingsRes));
          const holdData = await holdingsRes.json();
          if (cancelled) return;

          const map = {};
          (Array.isArray(holdData) ? holdData : []).forEach((h) => {
            if (h?.externalId) map[h.externalId] = Number(h.quantity || 0);
          });
          setHoldingsMap(map);
        } else {
          setHoldingsMap({});
        }

        // ✅ Mes alertes (pour activer le bouton supprimer)
        if (token && alertsRes) {
          if (!alertsRes.ok) throw new Error(await readErrorMessage(alertsRes));
          const alertsData = await alertsRes.json();

          if (cancelled) return;

          const map = {};
          (Array.isArray(alertsData) ? alertsData : []).forEach((a) => {
            if (a?.externalId) map[a.externalId] = true;
          });
          setAlertsMap(map);
        } else {
          setAlertsMap({});
        }
      } catch (e) {
        if (!cancelled) setPageError(e?.message || "Erreur chargement");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [API_BASE_URL, token]);

  // Charger historique pour chaque favori
  useEffect(() => {
    let cancelled = false;

    async function loadOne(externalId) {
      const res = await fetch(`${API_BASE_URL}/crypto/${externalId}/history?vs=${VS}`);
      if (!res.ok) throw new Error(await readErrorMessage(res));
      const histData = await res.json();
      const normalized = Array.isArray(histData)
        ? histData
            .filter((p) => p && p.ts != null && p.price != null)
            .map((p) => ({ ts: Number(p.ts), price: Number(p.price) }))
        : [];
      return sortByTsAsc(normalized);
    }

    (async () => {
      try {
        const missing = favorites.filter((extId) => !historyMap[extId]);
        if (missing.length === 0) return;

        const updates = {};
        for (const extId of missing) {
          try {
            const series = await loadOne(extId);
            if (!cancelled) updates[extId] = series;
          } catch {
            if (!cancelled) updates[extId] = [];
          }
        }

        if (!cancelled) setHistoryMap((prev) => ({ ...prev, ...updates }));
      } catch {
        // ignore
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [API_BASE_URL, favorites]); // historyMap volontairement absent

  const favoritesDetails = useMemo(() => {
    const byExternalId = new Map(allCryptos.map((c) => [c.externalId, c]));
    return favorites.map((extId) => byExternalId.get(extId)).filter(Boolean);
  }, [favorites, allCryptos]);

  const xTickFormatter = useMemo(() => {
    return interval === "7d" ? formatTickDate : formatTickTime;
  }, [interval]);

  const filterByInterval = (series) => {
    const now = Date.now();
    const cut = now - intervalMeta.ms;
    const filtered = (series || []).filter((p) => p.ts >= cut);
    return filtered.length > 0 ? filtered : (series || []);
  };

  const addBalance = async () => {
    if (!token) {
      setPageError("Tu dois être connecté pour ajouter du solde.");
      return;
    }

    const n = Number(String(amountToAdd).replace(",", "."));
    if (!Number.isFinite(n) || n <= 0) return;

    try {
      const res = await fetch(`${API_BASE_URL}/me/balance/add`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...authHeaders(),
        },
        body: JSON.stringify({ amount: n }),
      });

      if (!res.ok) throw new Error(await readErrorMessage(res));

      const data = await res.json();
      setBalance(Number(data?.balance ?? 0) || 0);
      setAmountToAdd("");
    } catch (e) {
      setPageError(e?.message || "Erreur ajout solde");
    }
  };

  const removeFavorite = async (externalId) => {
    if (!token) return;

    try {
      const res = await fetch(`${API_BASE_URL}/me/favorites/${externalId}`, {
        method: "DELETE",
        headers: { ...authHeaders() },
      });

      if (!res.ok) throw new Error(await readErrorMessage(res));

      setFavorites((prev) => prev.filter((x) => x !== externalId));
      setHistoryMap((prev) => {
        const next = { ...prev };
        delete next[externalId];
        return next;
      });

      // optionnel: aussi enlever l'affichage holdings du coin (juste UI)
      setHoldingsMap((prev) => {
        const next = { ...prev };
        delete next[externalId];
        return next;
      });
    } catch {
      // ignore
    }
  };

  // ✅ Ouvrir modal alerte
  const openAlertModal = (crypto) => {
    setAlertCrypto(crypto);
    setThresholdHigh("");
    setThresholdLow("");
    setAlertError("");
    setAlertOpen(true);
  };

  const closeAlertModal = () => {
    setAlertOpen(false);
    setAlertCrypto(null);
    setThresholdHigh("");
    setThresholdLow("");
    setAlertError("");
  };

  // ✅ Supprimer alerte
  const deleteAlert = async (externalId) => {
    if (!token) {
      setPageError("Tu dois être connecté pour supprimer une alerte.");
      return;
    }

    try {
      const res = await fetch(`${API_BASE_URL}/me/alerts/${externalId}`, {
        method: "DELETE",
        headers: { ...authHeaders() },
      });

      // 204 = ok, 200 = ok, 404/400 -> message backend
      if (!res.ok && res.status !== 204) throw new Error(await readErrorMessage(res));

      setAlertsMap((prev) => {
        const next = { ...prev };
        delete next[externalId];
        return next;
      });
    } catch (e) {
      setPageError(e?.message || "Impossible de supprimer l’alerte.");
    }
  };

  // ✅ Enregistrer alerte (branché API)
  const saveAlert = async () => {
    if (!token) {
      setAlertError("Tu dois être connecté pour créer une alerte.");
      return;
    }
    if (!alertCrypto) return;

    const high = thresholdHigh.trim() === "" ? null : Number(thresholdHigh.replace(",", "."));
    const low = thresholdLow.trim() === "" ? null : Number(thresholdLow.replace(",", "."));

    if (high == null && low == null) {
      setAlertError("Renseigne au moins un seuil (haut ou bas).");
      return;
    }
    if ((high != null && !Number.isFinite(high)) || (low != null && !Number.isFinite(low))) {
      setAlertError("Seuil invalide.");
      return;
    }
    if (high != null && high <= 0) {
      setAlertError("Le seuil haut doit être > 0.");
      return;
    }
    if (low != null && low <= 0) {
      setAlertError("Le seuil bas doit être > 0.");
      return;
    }
    if (high != null && low != null && low >= high) {
      setAlertError("Le seuil bas doit être inférieur au seuil haut.");
      return;
    }

    try {
      const payload = {
        externalId: alertCrypto.externalId,
        thresholdHigh: high,
        thresholdLow: low,
      };

      // ⚠️ Ton backend accepte POST et PUT, tu utilises POST ici
      const res = await fetch(`${API_BASE_URL}/me/alerts`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...authHeaders(),
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error(await readErrorMessage(res));

      // ✅ Marquer l’alerte comme existante (active le bouton supprimer)
      setAlertsMap((prev) => ({ ...prev, [alertCrypto.externalId]: true }));

      closeAlertModal();
    } catch (e) {
      setAlertError(e?.message || "Impossible d'enregistrer l'alerte.");
    }
  };

  // ✅ return OK (aucun hook après)
  if (loading) return <div className="app">Chargement…</div>;

  return (
    <div className="app">
      <header className="app-header">
        <button className="page-btn" onClick={() => navigate(-1)}>
          ← Retour
        </button>

        <h1 style={{ marginTop: "12px" }}>Espace personnel</h1>
        <p>Solde + suivi de tes cryptos favorites</p>
      </header>

      {pageError && (
        <div className="card" style={{ marginBottom: 12, border: "1px solid #ff6b6b" }}>
          <b>Erreur :</b> {pageError}
        </div>
      )}

      {/* Solde */}
      <div className="card" style={{ marginBottom: "12px" }}>
        <h2 style={{ marginTop: 0 }}>Mon solde</h2>

        <div className="details-row">
          <span className="details-label">Solde</span>
          <span className="details-value">{balance.toLocaleString("fr-FR")} $</span>
        </div>

        <div style={{ display: "flex", gap: "10px", marginTop: "10px", alignItems: "center" }}>
          <input
            type="number"
            min="0"
            step="0.01"
            value={amountToAdd}
            onChange={(e) => setAmountToAdd(e.target.value)}
            placeholder="Ajouter un montant"
            style={{ padding: "10px", borderRadius: "10px", border: "1px solid #333", width: 220 }}
          />
          <button className="page-btn" onClick={addBalance}>
            Ajouter
          </button>
        </div>
      </div>

      {/* Intervalle charts */}
      <div className="card" style={{ marginBottom: "12px" }}>
        <div className="details-controls">
          <div className="details-interval">
            <span className="details-label">Intervalle courbes</span>
            <select value={interval} onChange={(e) => setInterval(e.target.value)}>
              {INTERVALS.map((x) => (
                <option key={x.value} value={x.value}>
                  {x.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Favoris + charts */}
      <div className="card">
        <h2 style={{ marginTop: 0 }}>Mes favoris</h2>

        {!token ? (
          <div>
            Tu n’es pas connecté. <br />
            Connecte-toi pour voir tes favoris et ton solde.
          </div>
        ) : favoritesDetails.length === 0 ? (
          <div>
            Aucun favori pour l’instant. <br />
            Ajoute des favoris sur le dashboard ❤️
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "1fr", gap: "12px" }}>
            {favoritesDetails.map((c) => {
              const raw = historyMap[c.externalId] || [];
              const series = filterByInterval(raw);
              const yDomain = computeYDomain(series, "price");

              const hasAlert = !!alertsMap[c.externalId];
              const ownedQty = Number(holdingsMap[c.externalId] || 0);

              return (
                <div key={c.externalId} className="card" style={{ margin: 0 }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                      {c.imageUrl && (
                        <img
                          src={c.imageUrl}
                          alt={c.name}
                          style={{ width: 32, height: 32, borderRadius: 999 }}
                        />
                      )}
                      <div>
                        <div style={{ fontWeight: 700 }}>
                          {c.name} ({c.symbol?.toUpperCase()})
                        </div>
                        <div style={{ opacity: 0.85 }}>
                          Prix actuel: {c.currentPrice != null ? `${formatPrice(c.currentPrice)} $` : "-"}
                        </div>
                      </div>
                    </div>

                    {/* ✅ Actions + holdings + buy/sell */}
                    <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 8 }}>
                      <div style={{ fontSize: 13, opacity: 0.9 }}>
                        Possédé : <b>{ownedQty.toLocaleString("fr-FR")}</b>
                      </div>

                      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "flex-end" }}>
                        <button className="page-btn" onClick={() => buyOne(c)}>
                          Acheter
                        </button>
                        <button className="page-btn" onClick={() => sellOne(c)} disabled={ownedQty <= 0}>
                          Vendre
                        </button>

                        <button className="page-btn" onClick={() => openAlertModal(c)}>
                          Alerte
                        </button>

                        <button
                          className="page-btn"
                          disabled={!hasAlert}
                          onClick={() => deleteAlert(c.externalId)}
                          title={hasAlert ? "Supprimer l’alerte" : "Aucune alerte à supprimer"}
                        >
                          Supprimer alerte
                        </button>

                        <button className="page-btn" onClick={() => navigate(`/crypto/${c.externalId}`)}>
                          Détails
                        </button>

                        <button className="page-btn" onClick={() => removeFavorite(c.externalId)}>
                          Retirer
                        </button>
                      </div>
                    </div>
                  </div>

                  <div className="chart-box" style={{ marginTop: 10 }}>
                    {series.length === 0 ? (
                      <div style={{ opacity: 0.85 }}>
                        Pas encore d’historique en BDD pour {c.name}.
                      </div>
                    ) : (
                      <ResponsiveContainer width="100%" height={260}>
                        <LineChart data={series}>
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
              );
            })}
          </div>
        )}
      </div>

      {/* ✅ Modal Alerte */}
      <Modal
        open={alertOpen}
        title={alertCrypto ? `Alerte – ${alertCrypto.name} (${alertCrypto.symbol?.toUpperCase()})` : "Alerte"}
        onClose={closeAlertModal}
      >
        <div style={{ opacity: 0.85, marginBottom: 10 }}>
          Définis un seuil haut et/ou un seuil bas. Un email sera envoyé quand le prix l’atteint.
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
          <div>
            <div className="details-label" style={{ marginBottom: 6 }}>Seuil haut ($)</div>
            <input
              type="number"
              min="0"
              step="0.000001"
              value={thresholdHigh}
              onChange={(e) => setThresholdHigh(e.target.value)}
              placeholder="ex: 50000"
              style={{ padding: "10px", borderRadius: "10px", border: "1px solid #333", width: "100%" }}
            />
          </div>

          <div>
            <div className="details-label" style={{ marginBottom: 6 }}>Seuil bas ($)</div>
            <input
              type="number"
              min="0"
              step="0.000001"
              value={thresholdLow}
              onChange={(e) => setThresholdLow(e.target.value)}
              placeholder="ex: 42000"
              style={{ padding: "10px", borderRadius: "10px", border: "1px solid #333", width: "100%" }}
            />
          </div>
        </div>

        {alertCrypto?.currentPrice != null && (
          <div style={{ marginTop: 10, opacity: 0.85 }}>
            Prix actuel : <b>{formatPrice(alertCrypto.currentPrice)} $</b>
          </div>
        )}

        {alertError && (
          <div style={{ marginTop: 10, color: "#ffb3b3" }}>
            {alertError}
          </div>
        )}

        <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 14 }}>
          <button className="page-btn" onClick={closeAlertModal}>
            Annuler
          </button>
          <button className="page-btn" onClick={saveAlert}>
            Enregistrer
          </button>
        </div>
      </Modal>
    </div>
  );
}
