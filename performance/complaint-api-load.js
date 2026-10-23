import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
if (!BASE_URL) {
  throw new Error('Set BASE_URL before running the complaint API load test.');
}

const TARGET_IP = __ENV.TARGET_IP;
const complaintApiDuration = new Trend('complaint_api_duration', true);

export const options = {
  ...(TARGET_IP ? { hosts: { 'jdsskbus.duckdns.org': TARGET_IP } } : {}),
  scenarios: {
    complaint_api_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 5 },
        { duration: '2m', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: [
      { threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '30s' },
    ],
    complaint_api_duration: [
      { threshold: 'p(95)<800', abortOnFail: true, delayAbortEval: '30s' },
    ],
    checks: [
      { threshold: 'rate>0.99', abortOnFail: true, delayAbortEval: '30s' },
    ],
  },
};

const categories = ['ALL', 'PRAISE', 'COMPLAINT', 'LOST'];

export default function () {
  const category = categories[Math.floor(Math.random() * categories.length)];
  const page = Math.random() < 0.8 ? 0 : 1;
  const url = `${BASE_URL}/api/public/complaints?category=${category}&keyword=&page=${page}`;
  const response = http.get(url, {
    tags: { endpoint: 'complaints-api', category },
    timeout: '10s',
  });

  complaintApiDuration.add(response.timings.duration, { category });

  let body;
  try {
    body = response.json();
  } catch (_) {
    body = null;
  }

  check(response, {
    'complaints API: HTTP 200': (result) => result.status === 200,
    'complaints API: JSON response': () => body !== null,
    'complaints API: items array': () => Array.isArray(body?.items),
    'complaints API: paging fields': () =>
      Number.isInteger(body?.page) && Number.isInteger(body?.totalPages),
  });

  sleep(Math.random() * 2 + 1);
}

export function handleSummary(data) {
  const requestCount = data.metrics.http_reqs?.values?.count ?? 0;
  const failureRate = (data.metrics.http_req_failed?.values?.rate ?? 0) * 100;
  const checkRate = (data.metrics.checks?.values?.rate ?? 0) * 100;
  const p95 = data.metrics.complaint_api_duration?.values?.['p(95)'] ?? 0;
  const terminalSummary = [
    '',
    '=== SGComplaint complaint API load test summary ===',
    `requests: ${requestCount}`,
    `failed: ${failureRate.toFixed(2)}%`,
    `checks passed: ${checkRate.toFixed(2)}%`,
    `complaint API p95: ${p95.toFixed(2)} ms`,
    'JSON: performance/results/complaint-api-summary.json',
    '',
  ].join('\n');

  return {
    stdout: terminalSummary,
    'performance/results/complaint-api-summary.json': JSON.stringify(data, null, 2),
  };
}