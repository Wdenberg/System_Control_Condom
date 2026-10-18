import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
const scenario = __ENV.SCENARIO || 'new';
if (__ENV.GATE_DRIVER !== 'SIMULATED' || __ENV.GATE_ENABLED !== 'false') throw new Error('Load tests require GATE_DRIVER=SIMULATED and GATE_ENABLED=false');
const scenarios = {
  new: { executor: 'constant-arrival-rate', rate: Number(__ENV.RATE || 20), timeUnit: '1s', duration: __ENV.DURATION || '30s', preAllocatedVUs: 10, maxVUs: 50 },
  duplicate: { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: '30s', preAllocatedVUs: 10, maxVUs: 50 },
  concurrent: { executor: 'shared-iterations', vus: 20, iterations: 100, maxDuration: '30s' },
  cameras: { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: '30s', preAllocatedVUs: 10, maxVUs: 50 },
  burst: { executor: 'ramping-arrival-rate', startRate: 1, timeUnit: '1s', preAllocatedVUs: 10, maxVUs: 50, stages: [{ target: 40, duration: '10s' }, { target: 1, duration: '10s' }] },
  invalid: { executor: 'constant-arrival-rate', rate: 5, timeUnit: '1s', duration: '10s', preAllocatedVUs: 5 },
};
if (!scenarios[scenario]) throw new Error('Unknown SCENARIO');
export const options = { scenarios: { [scenario]: scenarios[scenario] }, thresholds: { business_error_rate: ['rate<0.01'], webhook_latency: ['p(95)<500'] } };
const latency = new Trend('webhook_latency');
const failures = new Rate('business_error_rate');
const duplicates = new Counter('duplicate_events');
const processed = new Counter('processed_events');
const fixture = open('../backend/src/test/resources/hikvision/anpr-entry.xml');
export function setup() {
  if (!__ENV.LOAD_TEST_ACCESS_TOKEN) throw new Error('Supply LOAD_TEST_ACCESS_TOKEN for the isolated target preflight');
  const response = http.get(`${__ENV.BASE_URL || 'http://localhost:8080'}/api/v1/gates/status`, { headers: { Authorization: `Bearer ${__ENV.LOAD_TEST_ACCESS_TOKEN}` } });
  if (response.status !== 200) throw new Error('Cannot verify target gate safety');
  const status = response.json();
  if (status.enabled !== false || status.driver !== 'SIMULATED') throw new Error('Target must have physical automation disabled and simulated driver');
}
export default function () {
  const exit = scenario === 'cameras' && __ITER % 2 === 1;
  const token = exit ? __ENV.HIKVISION_EXIT_WEBHOOK_TOKEN : __ENV.HIKVISION_ENTRY_WEBHOOK_TOKEN;
  if (!token) throw new Error('Supply distinct camera tokens');
  const uuid = scenario === 'duplicate' || scenario === 'concurrent' ? `load-${scenario}` : `load-${scenario}-${Date.now()}-${__VU}-${__ITER}`;
  let xml = fixture.replace(/<UUID>[^<]*<\/UUID>/, `<UUID>${uuid}</UUID>`);
  if (scenario !== 'duplicate' && scenario !== 'concurrent') {
    const plate = 'K' + ((Date.now() + __VU * 100000 + __ITER) % 2176782336).toString(36).toUpperCase().padStart(6, '0');
    xml = xml.replace(/SOO3H61/g, plate);
  }
  if (exit) xml = xml.replace(/192\.168\.15\.43/g, '192.168.15.44');
  const response = http.post(`${__ENV.BASE_URL || 'http://localhost:8080'}/api/v1/integrations/hikvision/events`, xml, { headers: { 'Content-Type': 'application/xml', 'X-Hikvision-Webhook-Token': scenario === 'invalid' ? 'invalid' : token } });
  latency.add(response.timings.duration);
  const accepted = scenario === 'invalid' ? response.status === 401 : response.status === 200;
  failures.add(!accepted); check(response, { 'expected webhook response': () => accepted });
  if (response.status === 200) { const body = response.json(); if (body.status === 'DUPLICATE') duplicates.add(1); if (body.status === 'RECEIVED') processed.add(1); }
}
