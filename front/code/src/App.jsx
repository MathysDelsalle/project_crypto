import { useEffect, useMemo, useState } from "react";
import "./App.css";

export default function App() {
  const [cryptos, setCryptos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(null);

  // tri
  const [sortConfig, setSortConfig] = useState({
    key: null,        
    direction: "asc", 
  });

  // pagination
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 25;

  const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

  useEffect(() => {
    let isCancelled = false;

    async function fetchCryptos(showLoader = false) {
      try {
        if (showLoader) {
          setLoading(true);
        }
        const response = await fetch(`${API_BASE_URL}/cryptos`);
        if (!response.ok) {
          throw new Error(`Erreur HTTP: ${response.status}`);
        }
        const data = await response.json();

        if (!isCancelled) {
          setCryptos(data);
          setError(null);
          setLastUpdate(new Date());
        }
      } catch (err) {
        console.error(err);
        if (!isCancelled) {
          setError("Impossible de charger les cryptos");
        }
      } finally {
        if (!isCancelled && showLoader) {
          setLoading(false);
        }
      }
    }

    fetchCryptos(true);

    const intervalId = setInterval(() => {
      fetchCryptos(false);
    }, 10_000);

    return () => {
      isCancelled = true;
      clearInterval(intervalId);
    };
  }, [API_BASE_URL]);

  // tri
  const sortedCryptos = useMemo(() => {
    const data = [...cryptos];

    if (sortConfig.key) {
      data.sort((a, b) => {
        const { key, direction } = sortConfig;

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
    }

    return data;
  }, [cryptos, sortConfig]);

  //pages avec 50 elt par page
  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(sortedCryptos.length / ITEMS_PER_PAGE)),
    [sortedCryptos.length]
  );
    //fait en sorte que si on a moins de 50 elt ca ne plante pas  
  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  //fait en sorte de n'afficher que 50 cryptos et pas 100 comme nous renvoie coin gecko
  const paginatedCryptos = useMemo(() => {
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const endIndex = startIndex + ITEMS_PER_PAGE;
    return sortedCryptos.slice(startIndex, endIndex);
  }, [sortedCryptos, currentPage]);

  //changement du tri en grand -> petit ==> petit -> grand par exemple
  const handleSort = (key) => {
    setSortConfig((prev) => {
      if (prev.key === key) {
        return {
          key,
          direction: prev.direction === "asc" ? "desc" : "asc",
        };
      }
      return {
        key,
        direction: "asc",
      };
    });
  };

  if (loading) {
    return (
      <div className="app">
        <p>Chargement des cryptos...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="app">
        <p className="error">{error}</p>
      </div>
    );
  }

  const getArrow = (key) =>
    sortConfig.key === key
      ? sortConfig.direction === "asc"
        ? "▲"
        : "▼"
      : "";

  const goToPage = (page) => {
    if (page < 1 || page > totalPages) return;
    setCurrentPage(page);
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>Crypto Dashboard</h1>
        <p>Top cryptos mises à jour depuis CoinGecko</p>
        {lastUpdate && (
          <p
            style={{
              fontSize: "0.85rem",
              color: "#9ca3af",
              marginTop: "0.5rem",
            }}
          >
            Dernière mise à jour :{" "}
            {lastUpdate.toLocaleTimeString("fr-FR", {
              hour: "2-digit",
              minute: "2-digit",
              second: "2-digit",
            })}
          </p>
        )}
      </header>

      <div className="card">
        <table className="crypto-table">
          <thead>
            <tr>
              <th>Logo</th>

              <th className="sortable" onClick={() => handleSort("name")}>
                Nom <span className="sort-arrow">{getArrow("name")}</span>
              </th>

              <th>Symbole</th>

              <th
                className="sortable"
                onClick={() => handleSort("currentPrice")}
              >
                Prix{" "}
                <span className="sort-arrow">{getArrow("currentPrice")}</span>
              </th>

              <th
                className="sortable"
                onClick={() => handleSort("marketCap")}
              >
                Market Cap{" "}
                <span className="sort-arrow">{getArrow("marketCap")}</span>
              </th>
            </tr>
          </thead>

          <tbody>
            {paginatedCryptos.map((c) => (
              <tr key={c.id}>
                <td>
                  {c.imageUrl && (
                    <img
                      className="crypto-logo"
                      src={c.imageUrl}
                      alt={c.name}
                    />
                  )}
                </td>
                <td>{c.name}</td>
                <td>{c.symbol?.toUpperCase()}</td>
                <td>{c.currentPrice != null ? `${c.currentPrice} $` : "-"}</td>
                <td>
                  {c.marketCap != null
                    ? c.marketCap.toLocaleString("en-US")
                    : "-"}
                </td>
              </tr>
            ))}
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
                className={`page-btn ${
                  currentPage === page ? "active" : ""
                }`}
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
