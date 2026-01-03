import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { register } from "../services/authService";

export default function RegisterPage() {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState(""); // ✅ email
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (password !== password2) {
      setError("Les mots de passe ne correspondent pas.");
      return;
    }

    setLoading(true);
    try {
      await register({ username, email, password }); // ✅ envoie email
      setSuccess("Compte créé ! Tu peux te connecter.");
      setTimeout(() => navigate("/login", { replace: true }), 400);
    } catch (err) {
      setError(err?.message || "Erreur d'inscription.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <h1 style={styles.title}>S’inscrire</h1>

        {error ? <div style={styles.error}>{error}</div> : null}
        {success ? <div style={styles.success}>{success}</div> : null}

        <form onSubmit={handleSubmit} style={styles.form}>
          <input
            style={styles.input}
            placeholder="Nom d'utilisateur"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />

          <input
            style={styles.input}
            placeholder="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            required
          />

          <input
            style={styles.input}
            type="password"
            placeholder="Mot de passe"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            required
          />

          <input
            style={styles.input}
            type="password"
            placeholder="Confirmer le mot de passe"
            value={password2}
            onChange={(e) => setPassword2(e.target.value)}
            autoComplete="new-password"
            required
          />

          <button style={styles.button} disabled={loading}>
            {loading ? "Création..." : "Créer mon compte"}
          </button>

          <button
            type="button"
            style={styles.secondary}
            onClick={() => navigate("/login")}
          >
            Déjà un compte ? Se connecter
          </button>
        </form>
      </div>
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
    width: "100%",
    maxWidth: 420,
    background: "#020617",
    borderRadius: 12,
    padding: 24,
    boxShadow: "0 0 20px rgba(0,0,0,0.6)",
  },
  title: { margin: 0, marginBottom: 12, textAlign: "center" },
  form: { display: "grid", gap: 10 },
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
    marginBottom: 10,
  },
  success: {
    background: "#064e3b",
    padding: 10,
    borderRadius: 8,
    marginBottom: 10,
  },
};
