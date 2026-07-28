import http from 'k6/http';
import { check } from 'k6';

// Carga mixta contra los 4 endpoints obligatorios, aproximando el patrón de
// tráfico esperado en producción: la mayoría de las peticiones son lecturas
// de precio vigente, seguidas del historial, con una minoría de escrituras.
//   - 80% GET  /products/{id}/prices?date=  (precio vigente)
//   - 15% GET  /products/{id}/prices        (historial completo)
//   -  5% POST /products                    (alta de producto)

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = __ENV.PRODUCT_ID;

if (!PRODUCT_ID) {
  throw new Error('PRODUCT_ID env var is required (product must be seeded before running k6)');
}

// Fechas cubiertas por los 3 precios que benchmark.sh siembra antes de invocar
// este script: 2024-01-01..2024-06-30, 2024-07-01..2024-12-31, 2025-01-01..(sin fin).
const CURRENT_PRICE_DATES = ['2024-04-15', '2024-08-15', '2025-03-01'];

export const options = {
  scenarios: {
    mixed_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '20s', target: 50 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200', 'p(99)<500'],
    'http_req_duration{endpoint:current_price}': ['p(95)<100'],
    'http_req_duration{endpoint:history}': ['p(95)<150'],
    'http_req_duration{endpoint:create_product}': ['p(95)<200'],
  },
};

function getCurrentPrice() {
  const date = CURRENT_PRICE_DATES[Math.floor(Math.random() * CURRENT_PRICE_DATES.length)];
  const res = http.get(`${BASE_URL}/products/${PRODUCT_ID}/prices?date=${date}`, {
    tags: { endpoint: 'current_price' },
  });
  check(res, { 'current price status is 200': (r) => r.status === 200 });
}

function getHistory() {
  const res = http.get(`${BASE_URL}/products/${PRODUCT_ID}/prices`, {
    tags: { endpoint: 'history' },
  });
  check(res, { 'history status is 200': (r) => r.status === 200 });
}

function createProduct() {
  const payload = JSON.stringify({
    name: `k6 load test product ${__VU}-${__ITER}`,
    description: 'Producto generado por el benchmark k6',
  });
  const res = http.post(`${BASE_URL}/products`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'create_product' },
  });
  check(res, { 'create product status is 201': (r) => r.status === 201 });
}

export default function () {
  const roll = Math.random();
  if (roll < 0.8) {
    getCurrentPrice();
  } else if (roll < 0.95) {
    getHistory();
  } else {
    createProduct();
  }
}
