const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const STORAGE = {
  token: "auth_token",
  user: "auth_user",
};

export function getToken() {
  return localStorage.getItem(STORAGE.token);
}

export function getUser() {
  const raw = localStorage.getItem(STORAGE.user);
  return raw ? JSON.parse(raw) : null;
}

export function isAuthenticated() {
  return !!getToken();
}

export function logout() {
  localStorage.removeItem(STORAGE.token);
  localStorage.removeItem(STORAGE.user);
}

export async function login({ username, password }) {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    let message = "Identifiants invalides";
    try {
      const err = await response.json();
      message = err?.message || err?.error || message;
    } catch {}
    throw new Error(message);
  }

  const data = await response.json();

  const token = data.token;
  const user = {
    username: data.username || username,
    roles: data.roles || [],
  };

  if (!token) throw new Error("Token manquant dans la réponse");

  localStorage.setItem(STORAGE.token, token);
  localStorage.setItem(STORAGE.user, JSON.stringify(user));

  return user;
}


export async function register({ username, email, password }) {
  const response = await fetch(`${API_BASE_URL}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, email, password }),
  });

  let payload = null;
  try {
    payload = await response.json();
  } catch {
  }

  if (!response.ok) {
    const msg =
      payload?.message ||
      payload?.error ||
      `Erreur serveur (${response.status})`;
    throw new Error(msg);
  }

  return payload ?? { ok: true };
}