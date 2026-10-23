import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
if (!BASE_URL) {
  throw new Error('Set BASE_URL before running the load test.');
}

const TARGET_IP = __ENV.TARGET_IP;

export const options = {
  ...(TARGET_IP ? { hosts: { 'jdsskbus.duckdns.org': TARGET_IP } } : {}),
  scenarios: {
    normal_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 5 },
        { duration: '3m', target: 20 },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

const pages = [
  { name: 'home', path: '/' },
  { name: 'notices', path: '/notices' },
  { name: 'complaints', path: '/complaints' },
  { name: 'village-bus', path: '/route/village-bus' },
];

export default function () {
  const page = pages[Math.floor(Math.random() * pages.length)];
  const response = http.get(`${BASE_URL}${page.path}`, {
    tags: { page: page.name },
    timeout: '10s',
  });

  check(response, {
    [`${page.name}: HTTP 200~399`]: (result) =>
      result.status >= 200 && result.status < 400,
  });

  sleep(Math.random() * 2 + 1);
}

export function handleSummary(data) {
  const requestCount = data.metrics.http_reqs?.values?.count ?? 0;
  const failureRate = (data.metrics.http_req_failed?.values?.rate ?? 0) * 100;
  const checkRate = (data.metrics.checks?.values?.rate ?? 0) * 100;
  const p95 = data.metrics.http_req_duration?.values?.['p(95)'] ?? 0;
  const terminalSummary = [
    '',
    '=== SGComplaint load test summary ===',
    `requests: ${requestCount}`,
    `failed: ${failureRate.toFixed(2)}%`,
    `checks passed: ${checkRate.toFixed(2)}%`,
    `response p95: ${p95.toFixed(2)} ms`,
    'JSON: performance/results/load-summary.json',
    '',
  ].join('\n');

  return {
    stdout: terminalSummary,
    'performance/results/load-summary.json': JSON.stringify(data, null, 2),
  };
}
