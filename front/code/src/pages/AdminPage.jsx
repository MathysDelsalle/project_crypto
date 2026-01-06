import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getToken } from "../services/authService";

export default function AdminPage() {
  const navigate = useNavigate();
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

  const [users, setUsers] = useState([]);
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState(null);

  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  // funds
  const [mode, setMode] = useState("add"); // add/remove
  const [amount, setAmount] = useState("");

  const authHeaders = () => {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  };

  const showToast = (msg) => {
    setToast(msg);
    if (window.__t) clearTimeout(window.__t);
    window.__t = setTimeout(() => setToast(null), 2000);
  };

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/admin/users`, {
        headers: { ...authHeaders() },
      });
      if (!res.ok) throw new Error();
      const data = await res.json();
      setUsers(Array.isArray(data) ? data : []);
    } catch {
      showToast("Erreur chargement users");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [API_BASE_URL]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return users;
    return users.filter((u) => {
      return (
        String(u.id).includes(q) ||
        (u.username || "").toLowerCase().includes(q) ||
        (u.email || "").toLowerCase().includes(q)
      );
    });
  }, [users, query]);

  const isAdmin = (u) => (u?.roles || []).includes("ROLE_ADMIN");

  const promoteDemote = async () => {
    if (!selected) return;
    const username = selected.username;

    try {
      const url = isAdmin(selected)
        ? `${API_BASE_URL}/admin/demote/${username}`
        : `${API_BASE_URL}/admin/promote/${username}`;

      const res = await fetch(url, {
        method: "POST",
        headers: { ...authHeaders() },
      });
      if (!res.ok) throw new Error();

      showToast(isAdmin(selected) ? "Rétrogradé ✅" : "Promu admin ✅");
      await fetchUsers();
    } catch {
      showToast("Erreur promote/demote");
    }
  };

  const applyFunds = async () => {
    if (!selected) return;

    const n = Number(String(amount).replace(",", "."));
    if (!Number.isFinite(n) || n <= 0) return showToast("Montant invalide");

    const delta = mode === "add" ? n : -n;

    try {
      const res = await fetch(`${API_BASE_URL}/admin/funds/${selected.username}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json", ...authHeaders() },
        body: JSON.stringify({ delta }),
      });
      if (!res.ok) throw new Error();

      const updated = await res.json();
      showToast(mode === "add" ? "Fonds ajoutés ✅" : "Fonds retirés ✅");

      setUsers((prev) => prev.map((u) => (u.username === updated.username ? updated : u)));
      setSelected(updated);
      setAmount("");
    } catch {
      showToast("Erreur funds");
    }
  };

  if (loading) return <div className="app">Chargement...</div>;

  return (
    <div className="app">
      {toast && <div className="toast">{toast}</div>}

      <header className="app-header">
        <h1>Admin</h1>
        <p>Recherche un utilisateur, puis gère son rang et son solde.</p>

        <div style={{ display: "flex", gap: 10, justifyContent: "center", flexWrap: "wrap" }}>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Recherche: username / email / id"
            style={{
              padding: 10,
              borderRadius: 10,
              border: "1px solid #ddd",
              width: 420,
              maxWidth: "100%",
            }}
          />

          <button className="page-btn" onClick={fetchUsers}>Rafraîchir</button>

          {/* ✅ NOUVEAU bouton retour */}
          <button className="page-btn" onClick={() => navigate("/homeboard")}>
            ← Retour dashboard
          </button>
        </div>
      </header>

      <div className="card" style={{ display: "grid", gridTemplateColumns: "1.2fr 1fr", gap: 16 }}>
        {/* List */}
        <div>
          <h2>Utilisateurs ({filtered.length})</h2>
          <div style={{ display: "grid", gap: 8 }}>
            {filtered.map((u) => {
              const active = selected?.username === u.username;
              return (
                <button
                  key={u.id}
                  onClick={() => setSelected(u)}
                  style={{
                    textAlign: "left",
                    padding: 12,
                    borderRadius: 12,
                    border: active ? "2px solid #999" : "1px solid #ddd",
                    background: active ? "rgba(0,0,0,0.04)" : "white",
                    cursor: "pointer",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
                    <div style={{ minWidth: 0 }}>
                      <div style={{ fontWeight: 800 }}>
                        {u.username} <span style={{ opacity: 0.65, fontWeight: 400 }}>#{u.id}</span>
                      </div>
                      <div style={{ opacity: 0.8, overflow: "hidden", textOverflow: "ellipsis" }}>
                        {u.email}
                      </div>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <div style={{ fontSize: 12, opacity: 0.75 }}>{isAdmin(u) ? "ADMIN" : "USER"}</div>
                      <div style={{ fontWeight: 800 }}>{Number(u.balance ?? 0).toLocaleString("fr-FR")} $</div>
                    </div>
                  </div>
                </button>
              );
            })}
            {filtered.length === 0 && <div style={{ opacity: 0.8 }}>Aucun résultat.</div>}
          </div>
        </div>

        {/* Actions */}
        <div>
          <h2>Actions</h2>

          {!selected ? (
            <div style={{ opacity: 0.8 }}>Sélectionne un utilisateur.</div>
          ) : (
            <div style={{ display: "grid", gap: 12 }}>
              <div style={{ border: "1px solid #ddd", borderRadius: 12, padding: 12 }}>
                <div style={{ fontWeight: 900 }}>{selected.username}</div>
                <div style={{ opacity: 0.8 }}>{selected.email}</div>
                <div style={{ marginTop: 8, fontSize: 12, opacity: 0.75 }}>
                  Rôles: {(selected.roles || []).join(", ") || "—"}
                </div>
                <div style={{ marginTop: 6, fontWeight: 800 }}>
                  Solde: {Number(selected.balance ?? 0).toLocaleString("fr-FR")} $
                </div>
              </div>

              <div style={{ border: "1px solid #ddd", borderRadius: 12, padding: 12 }}>
                <div style={{ fontWeight: 900, marginBottom: 8 }}>Rang</div>
                <button className="page-btn" onClick={promoteDemote}>
                  {isAdmin(selected) ? "Demote (retirer admin)" : "Promote (mettre admin)"}
                </button>
              </div>

              <div style={{ border: "1px solid #ddd", borderRadius: 12, padding: 12 }}>
                <div style={{ fontWeight: 900, marginBottom: 8 }}>Fonds</div>

                <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <select
                    value={mode}
                    onChange={(e) => setMode(e.target.value)}
                    style={{ padding: 10, borderRadius: 10, border: "1px solid #ddd" }}
                  >
                    <option value="add">Ajouter</option>
                    <option value="remove">Enlever</option>
                  </select>

                  <input
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="Montant (ex: 50)"
                    style={{
                      padding: 10,
                      borderRadius: 10,
                      border: "1px solid #ddd",
                      flex: 1,
                      minWidth: 140,
                    }}
                  />

                  <button className="page-btn" onClick={applyFunds}>
                    Valider
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
