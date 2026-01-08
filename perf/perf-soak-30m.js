import http from "k6/http";
import { check } from "k6";
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";

const API_URL = __ENV.API_URL || "http://localhost:8080";
const COLLECTOR_URL = __ENV.COLLECTOR_URL || "http://localhost:8081";
const FRONT_URL = __ENV.FRONT_URL || "http://localhost:3000";

// débit stable du soak (override via env)
const SOAK_RPS = Number(__ENV.SOAK_RPS || 150);

// répartition du trafic
const API_SHARE = Number(__ENV.API_SHARE || 0.7);
const FRONT_SHARE = Number(__ENV.FRONT_SHARE || 0.2);
const COLLECTOR_SHARE = Number(__ENV.COLLECTOR_SHARE || 0.1);

function split(rps) {
  const api = Math.max(1, Math.round(rps * API_SHARE));
  const front = Math.max(1, Math.round(rps * FRONT_SHARE));
  const collector = Math.max(1, rps - api - front);
  return { api, front, collector };
}

const R = split(SOAK_RPS);

export const options = {
  scenarios: {
    api_soak: {
      executor: "constant-arrival-rate",
      rate: R.api,
      timeUnit: "1s",
      duration: "30m",
      preAllocatedVUs: 200,
      maxVUs: 800,
      exec: "apiScenario",
      tags: { endpoint: "api_cryptos", test: "soak" },
    },
    front_soak: {
      executor: "constant-arrival-rate",
      rate: R.front,
      timeUnit: "1s",
      duration: "30m",
      preAllocatedVUs: 100,
      maxVUs: 500,
      exec: "frontScenario",
      tags: { endpoint: "front_home", test: "soak" },
    },
    collector_soak: {
      executor: "constant-arrival-rate",
      rate: R.collector,
      timeUnit: "1s",
      duration: "30m",
      preAllocatedVUs: 40,
      maxVUs: 200,
      exec: "collectorScenario",
      tags: { endpoint: "collector_health", test: "soak" },
    },
  },

  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{endpoint:api_cryptos}": ["p(95)<200", "p(99)<500"],
    "http_req_duration{endpoint:front_home}": ["p(95)<500", "p(99)<1200"],
    "http_req_duration{endpoint:collector_health}": ["p(95)<150"],
    checks: ["rate>0.99"],
  },

  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
};

export function apiScenario() {
  const res = http.get(`${API_URL}/api/cryptos`, { tags: { endpoint: "api_cryptos" } });

  check(res, {
    "API status 200": (r) => r.status === 200,
    "API is json": (r) => (r.headers["Content-Type"] || "").includes("application/json"),
    "API is array": (r) => {
      try {
        return Array.isArray(r.json());
      } catch (e) {
        return false;
      }
    },
  });
}

export function collectorScenario() {
  let res = http.get(`${COLLECTOR_URL}/actuator/health`, { tags: { endpoint: "collector_health" } });
  if (res.status === 404) {
    res = http.get(`${COLLECTOR_URL}/health`, { tags: { endpoint: "collector_health" } });
  }

  check(res, { "Collector status 200": (r) => r.status === 200 });
}

export function frontScenario() {
  const res = http.get(`${FRONT_URL}/`, { tags: { endpoint: "front_home" } });

  check(res, {
    "Front status 200/304": (r) => r.status === 200 || r.status === 304,
    "Front has html": (r) => (r.headers["Content-Type"] || "").includes("text/html"),
  });
}

export function handleSummary(data) {
  return {
    "/results/report.html": htmlReport(data),
    "/results/summary.pretty.json": JSON.stringify(data, null, 2),
  };
}
