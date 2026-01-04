import React, { useState } from "react";
import { login, isAuthenticated, getUser } from "../services/authService";
import { Navigate, useNavigate } from "react-router-dom";

export default function LoginPage() {
  const navigate = useNavigate();

  // Déjà connecté -> admin => /admin, sinon /app
  if (isAuthenticated()) {
    const user = getUser();
    const isAdmin = user?.roles?.includes("ROLE_ADMIN");
    return <Navigate to={isAdmin ? "/admin" : "/app"} replace />;
  }

  const [identifier, setIdentifier] = useState(""); // ✅ email OU username
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      // ✅ On envoie l'identifiant dans "username"
      // (backend: tu peux faire une recherche username OU email)
      const user = await login({ username: identifier, password });

      const isAdmin = user.roles?.includes("ROLE_ADMIN");
      navigate(isAdmin ? "/admin" : "/app", { replace: true });
    } catch (err) {
      setError(err?.message || "Erreur de connexion");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <form onSubmit={handleSubmit} style={styles.card}>
        <h2 style={styles.title}>Connexion</h2>

        {error && <div style={styles.error}>{error}</div>}

        <input
          style={styles.input}
          placeholder="Email ou nom d'utilisateur"
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          autoComplete="username"
          required
        />

        <input
          style={styles.input}
          type="password"
          placeholder="Mot de passe"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />

        <button style={styles.button} disabled={loading}>
          {loading ? "Connexion..." : "Se connecter"}
        </button>

        {/* ✅ Bouton vers inscription */}
        <button
          type="button"
          style={styles.secondary}
          onClick={() => navigate("/register")}
        >
          S’inscrire
        </button>
      </form>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "100vh",
    display: "grid",
    placeItems: "center",
    padding: 16,
    background: "#0f172a",
    color: "white",
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, Arial",
  },
  card: {
    width: 360,
    padding: 24,
    borderRadius: 12,
    background: "#020617",
    display: "grid",
    gap: 10,
    boxShadow: "0 0 20px rgba(0,0,0,0.6)",
  },
  title: { margin: 0, marginBottom: 6, textAlign: "center" },
  input: {
    padding: 12,
    borderRadius: 8,
    border: "1px solid #1e293b",
    background: "#020617",
    color: "white",
  },
  button: {
    marginTop: 6,
    padding: 12,
    borderRadius: 8,
    border: "none",
    fontWeight: "bold",
    background: "#38bdf8",
    color: "#020617",
    cursor: "pointer",
  },
  secondary: {
    padding: 12,
    borderRadius: 8,
    border: "1px solid #1e293b",
    background: "transparent",
    color: "white",
    cursor: "pointer",
  },
  error: {
    background: "#7f1d1d",
    padding: 10,
    borderRadius: 8,
  },
};
