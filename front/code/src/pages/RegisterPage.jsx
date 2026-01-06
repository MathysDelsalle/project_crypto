import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { register } from "../services/authService";

/* ================== REGEX ================== */
const PASSWORD_REGEX =
  /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_\-+=\[\]{};':"\\|,.<>\/?]).{8,}$/;

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.(com|fr)$/i;
/* =========================================== */

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

    // ✅ validations front (on laisse le bouton cliquable, mais on refuse au submit)
    if (!EMAIL_REGEX.test(email)) {
      setError("Email invalide (ex: exemple@site.com ou .fr)");
      return;
    }

    if (!PASSWORD_REGEX.test(password)) {
      setError(
        "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial."
      );
      return;
    }

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
      // ✅ récupère le format standard du back
      setError(err?.response?.data?.message || "Erreur d'inscription.");
    } finally {
      setLoading(false);
    }
  }

  const passwordWeak = password.length > 0 && !PASSWORD_REGEX.test(password);
  const emailInvalid = email.length > 0 && !EMAIL_REGEX.test(email);

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

          {emailInvalid ? (
            <small style={styles.warning}>
              Email invalide (ex: exemple@site.com ou .fr)
            </small>
          ) : null}

          <input
            style={styles.input}
            type="password"
            placeholder="Mot de passe"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            required
          />

          <small style={styles.helper}>
            8 caractères min • 1 majuscule • 1 chiffre • 1 caractère spécial
          </small>

          {passwordWeak ? (
            <small style={styles.warning}>
              Mot de passe trop faible (majuscule + chiffre + spécial, 8+)
            </small>
          ) : null}

          <input
            style={styles.input}
            type="password"
            placeholder="Confirmer le mot de passe"
            value={password2}
            onChange={(e) => setPassword2(e.target.value)}
            autoComplete="new-password"
            required
          />

          {/* ✅ bouton cliquable (seulement bloqué pendant loading) */}
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
  helper: {
    fontSize: 12,
    color: "#94a3b8",
    marginTop: -4,
  },
  warning: {
    fontSize: 12,
    color: "#fbbf24",
    marginTop: -6,
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
