// App.jsx
import { useEffect, useMemo, useState } from "react";
import "./App.css";
import { Routes, Route, Navigate, useNavigate } from "react-router-dom";

import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CryptoDetailsPage from "./pages/CryptoDetailsPage";
import { isAuthenticated, getUser, logout } from "./services/authService";

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
  const user = authed ? getUser() : null;
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const handleMain = () => {
    if (!authed) return navigate("/login");
    navigate(isAdmin ? "/admin" : "/app");
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

  // ✅ Favoris (persistés)
  const [favorites, setFavorites] = useState(() => {
    try {
      const raw = localStorage.getItem("crypto_favorites");
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  });

  // ✅ Filtre favoris uniquement
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false);

  // ✅ Toast (alerte temporaire)
  const [toast, setToast] = useState(null);

  const authed = isAuthenticated();

  // Persist favoris
  useEffect(() => {
    localStorage.setItem("crypto_favorites", JSON.stringify(favorites));
  }, [favorites]);

  const isFav = (cryptoId) => favorites.includes(cryptoId);

  const showToast = (message) => {
    setToast(message);
    if (window.__toastTimer) window.clearTimeout(window.__toastTimer);
    window.__toastTimer = window.setTimeout(() => setToast(null), 2000);
  };

  const toggleFavorite = (crypto) => {
    setFavorites((prev) => {
      const exists = prev.includes(crypto.id);
      const next = exists ? prev.filter((id) => id !== crypto.id) : [...prev, crypto.id];

      showToast(exists ? `${crypto.name} retirée des favoris` : `${crypto.name} a été placé dans les favoris`);
      return next;
    });
  };

  // Si l’utilisateur se déconnecte, on enlève le filtre (sinon ça “cache” tout)
  useEffect(() => {
    if (!authed) setShowFavoritesOnly(false);
  }, [authed]);

  useEffect(() => {
    let cancelled = false;

    async function fetchCryptos(showLoader = false) {
      try {
        if (showLoader) setLoading(true);

        const response = await fetch(`${API_BASE_URL}/cryptos`);
        if (!response.ok) throw new Error(`Erreur HTTP: ${response.status}`);

        const data = await response.json();
        if (!cancelled) {
          setCryptos(data);
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
    return sortedCryptos.filter((c) => favorites.includes(c.id));
  }, [sortedCryptos, showFavoritesOnly, favorites, authed]);

  // pagination sur la liste filtrée
  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(filteredCryptos.length / ITEMS_PER_PAGE)),
    [filteredCryptos.length]
  );

  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages);
  }, [currentPage, totalPages]);

  // si on active/désactive le filtre, on revient page 1
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

  const goToDetails = (cryptoId) => {
    navigate(`/crypto/${cryptoId}`);
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
                onClick={() => goToDetails(c.id)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === "Enter") goToDetails(c.id);
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
                      title={isFav(c.id) ? "Retirer des favoris" : "Ajouter aux favoris"}
                      aria-label={isFav(c.id) ? "Retirer des favoris" : "Ajouter aux favoris"}
                    >
                      {isFav(c.id) ? "❤️" : "🤍"}
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

      {/* ✅ Page détails crypto */}
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

      <Route path="*" element={<Navigate to="/homeboard" replace />} />
    </Routes>
  );
}
