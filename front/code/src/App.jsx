import { useEffect, useMemo, useState } from "react";
import "./App.css";
import { Routes, Route, Navigate, useNavigate } from "react-router-dom";

import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CryptoDetailsPage from "./pages/CryptoDetailsPage";
import UserSpacePage from "./pages/UserSpacePage";
import { isAuthenticated, getUser, logout, getToken } from "./services/authService";

/* ======================
   Pages simples
====================== */

function AdminPage() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>Admin</h1>
        <p>Page admin (à compléter)</p>
      </header>
    </div>
  );
}

/* ======================
   Guards
====================== */

function RequireAuth({ children }) {
  return isAuthenticated() ? children : <Navigate to="/login" replace />;
}

function RequireAdmin({ children }) {
  const user = getUser();
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  return isAuthenticated() && isAdmin ? children : <Navigate to="/login" replace />;
}

/* ======================
   Homeboard actions (login/register/logout + go app/admin)
====================== */

function HomeboardActions() {
  const navigate = useNavigate();
  const authed = isAuthenticated();

  const handleMain = () => {
    if (!authed) return navigate("/login");
    navigate("/me"); // ✅ tu voulais que ce bouton aille vers /me
  };

  const handleRegister = () => navigate("/register");

  const handleLogout = () => {
    logout();
    navigate("/homeboard");
  };

  return (
    <div className="homeboard-actions">
      <button className="page-btn" onClick={handleMain}>
        {authed ? "Accéder à mon espace" : "Se connecter"}
      </button>

      {!authed && (
        <button className="page-btn" onClick={handleRegister}>
          S’inscrire
        </button>
      )}

      {authed && (
        <button className="page-btn" onClick={handleLogout}>
          Logout
        </button>
      )}
    </div>
  );
}

/* ======================
   Dashboard (Homeboard + App)
====================== */

function CryptoDashboard() {
  const navigate = useNavigate();

  const [cryptos, setCryptos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(null);

  // tri
  const [sortConfig, setSortConfig] = useState({
    key: "marketCapRank",
    direction: "asc",
  });

  // pagination
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 25;

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

  // ✅ Favoris (depuis BDD) : on stocke la liste des externalId
  const [favorites, setFavorites] = useState([]); // string[] externalId

  // ✅ Filtre favoris uniquement
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false);

  // ✅ Toast
  const [toast, setToast] = useState(null);

  const authed = isAuthenticated();

  function authHeaders() {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  const showToast = (message) => {
    setToast(message);
    if (window.__toastTimer) window.clearTimeout(window.__toastTimer);
    window.__toastTimer = window.setTimeout(() => setToast(null), 2000);
  };

  // ✅ Charger favoris depuis la BDD quand connecté
  useEffect(() => {
    if (!authed) {
      setFavorites([]);
      setShowFavoritesOnly(false);
      return;
    }

    let cancelled = false;

    (async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/me/favorites`, {
          headers: { ...authHeaders() },
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!cancelled) setFavorites(Array.isArray(data) ? data : []);
      } catch {
        // ignore
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [API_BASE_URL, authed]);

  const isFav = (externalId) => favorites.includes(externalId);

  const toggleFavorite = async (crypto) => {
    if (!authed) return;

    const extId = crypto.externalId;
    const exists = favorites.includes(extId);

    try {
      const url = `${API_BASE_URL}/me/favorites/${extId}`;
      const res = await fetch(url, {
        method: exists ? "DELETE" : "POST",
        headers: { ...authHeaders() },
      });
      if (!res.ok) throw new Error();

      setFavorites((prev) =>
        exists ? prev.filter((x) => x !== extId) : [...prev, extId]
      );

      showToast(
        exists ? `${crypto.name} retirée des favoris` : `${crypto.name} a été placé dans les favoris`
      );
    } catch {
      showToast("Erreur favoris (API)");
    }
  };

  useEffect(() => {
    let cancelled = false;

    async function fetchCryptos(showLoader = false) {
      try {
        if (showLoader) setLoading(true);

        const response = await fetch(`${API_BASE_URL}/cryptos`);
        if (!response.ok) throw new Error(`Erreur HTTP: ${response.status}`);

        const data = await response.json();
        if (!cancelled) {
          setCryptos(Array.isArray(data) ? data : []);
          setLastUpdate(new Date());
          setError(null);
        }
      } catch (e) {
        if (!cancelled) setError("Impossible de charger les cryptos");
      } finally {
        if (!cancelled && showLoader) setLoading(false);
      }
    }

    fetchCryptos(true);
    const id = setInterval(() => fetchCryptos(false), 10_000);

    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [API_BASE_URL]);

  // tri
  const sortedCryptos = useMemo(() => {
    const data = [...cryptos];
    const { key, direction } = sortConfig;

    data.sort((a, b) => {
      let aVal = a[key];
      let bVal = b[key];

      if (key === "name") {
        aVal = (aVal || "").toLowerCase();
        bVal = (bVal || "").toLowerCase();
      }

      if (key === "currentPrice" || key === "marketCap") {
        aVal = Number(aVal || 0);
        bVal = Number(bVal || 0);
      }

      if (aVal < bVal) return direction === "asc" ? -1 : 1;
      if (aVal > bVal) return direction === "asc" ? 1 : -1;
      return 0;
    });

    return data;
  }, [cryptos, sortConfig]);

  // ✅ filtre favoris uniquement (si activé et connecté)
  const filteredCryptos = useMemo(() => {
    if (!authed) return sortedCryptos;
    if (!showFavoritesOnly) return sortedCryptos;
    return sortedCryptos.filter((c) => favorites.includes(c.externalId));
  }, [sortedCryptos, showFavoritesOnly, favorites, authed]);

  // pagination
  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(filteredCryptos.length / ITEMS_PER_PAGE)),
    [filteredCryptos.length]
  );

  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages);
  }, [currentPage, totalPages]);

  useEffect(() => {
    setCurrentPage(1);
  }, [showFavoritesOnly]);

  const paginatedCryptos = useMemo(() => {
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const endIndex = startIndex + ITEMS_PER_PAGE;
    return filteredCryptos.slice(startIndex, endIndex);
  }, [filteredCryptos, currentPage]);

  const handleSort = (key) => {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === "asc" ? "desc" : "asc",
    }));
  };

  const goToDetails = (externalId) => {
    navigate(`/crypto/${externalId}`);
  };

  if (loading) return <div className="app">Chargement...</div>;
  if (error) return <div className="app error">{error}</div>;

  const arrow = (k) =>
    sortConfig.key === k ? (sortConfig.direction === "asc" ? "▲" : "▼") : "";

  const goToPage = (page) => {
    if (page < 1 || page > totalPages) return;
    setCurrentPage(page);
  };

  const favoritesCount = favorites.length;

  return (
    <div className="app">
      {toast && <div className="toast">{toast}</div>}

      <header className="app-header">
        <h1>Crypto Dashboard</h1>
        <p>Top cryptos mises à jour depuis CoinGecko</p>

        <HomeboardActions />

        {authed && (
          <div className="favorites-controls">
            <label className="favorites-checkbox">
              <input
                type="checkbox"
                checked={showFavoritesOnly}
                onChange={(e) => setShowFavoritesOnly(e.target.checked)}
              />
              Favoris uniquement
            </label>

            <span className="favorites-info">
              Favoris: {favoritesCount}
              {showFavoritesOnly ? ` • affichés: ${filteredCryptos.length}` : ""}
            </span>

            {showFavoritesOnly && favoritesCount === 0 && (
              <span className="favorites-warning">Aucun favori pour l’instant.</span>
            )}
          </div>
        )}

        {lastUpdate && (
          <p className="last-update">
            Dernière mise à jour : {lastUpdate.toLocaleTimeString("fr-FR")}
          </p>
        )}
      </header>

      <div className="card">
        <table className="crypto-table">
          <thead>
            <tr>
              <th onClick={() => handleSort("marketCapRank")}>Rang {arrow("marketCapRank")}</th>
              <th>Logo</th>
              <th onClick={() => handleSort("name")}>Nom {arrow("name")}</th>
              <th>Symbole</th>
              <th onClick={() => handleSort("currentPrice")}>Prix {arrow("currentPrice")}</th>
              <th onClick={() => handleSort("marketCap")}>Market Cap {arrow("marketCap")}</th>
              {authed && <th>Fav</th>}
            </tr>
          </thead>

          <tbody>
            {paginatedCryptos.map((c) => (
              <tr
                key={c.id}
                className="crypto-row-clickable"
                onClick={() => goToDetails(c.externalId)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === "Enter") goToDetails(c.externalId);
                }}
              >
                <td>#{c.marketCapRank}</td>
                <td>{c.imageUrl && <img className="crypto-logo" src={c.imageUrl} alt={c.name} />}</td>
                <td>{c.name}</td>
                <td>{c.symbol?.toUpperCase()}</td>
                <td>{c.currentPrice != null ? `${c.currentPrice} $` : "-"}</td>
                <td>{c.marketCap != null ? c.marketCap.toLocaleString("en-US") : "-"}</td>

                {authed && (
                  <td>
                    <button
                      className="heart-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleFavorite(c);
                      }}
                      title={isFav(c.externalId) ? "Retirer des favoris" : "Ajouter aux favoris"}
                      aria-label={isFav(c.externalId) ? "Retirer des favoris" : "Ajouter aux favoris"}
                    >
                      {isFav(c.externalId) ? "❤️" : "🤍"}
                    </button>
                  </td>
                )}
              </tr>
            ))}

            {authed && showFavoritesOnly && filteredCryptos.length === 0 && (
              <tr>
                <td colSpan={authed ? 7 : 6} className="empty-row">
                  Aucune crypto favorite à afficher.
                </td>
              </tr>
            )}
          </tbody>
        </table>

        <div className="pagination">
          <button
            className="page-btn"
            onClick={() => goToPage(currentPage - 1)}
            disabled={currentPage === 1}
          >
            ◀ Précédent
          </button>

          {[...Array(totalPages)].map((_, index) => {
            const page = index + 1;
            return (
              <button
                key={page}
                className={`page-btn ${currentPage === page ? "active" : ""}`}
                onClick={() => goToPage(page)}
              >
                {page}
              </button>
            );
          })}

          <button
            className="page-btn"
            onClick={() => goToPage(currentPage + 1)}
            disabled={currentPage === totalPages}
          >
            Suivant ▶
          </button>
        </div>
      </div>
    </div>
  );
}

/* ======================
   Routes
====================== */

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/homeboard" replace />} />
      <Route path="/homeboard" element={<CryptoDashboard />} />

      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route path="/crypto/:id" element={<CryptoDetailsPage />} />

      <Route
        path="/app"
        element={
          <RequireAuth>
            <CryptoDashboard />
          </RequireAuth>
        }
      />

      <Route
        path="/admin"
        element={
          <RequireAdmin>
            <AdminPage />
          </RequireAdmin>
        }
      />

      <Route
        path="/me"
        element={
          <RequireAuth>
            <UserSpacePage />
          </RequireAuth>
        }
      />

      <Route path="*" element={<Navigate to="/homeboard" replace />} />
    </Routes>
  );
}
