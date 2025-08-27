import http from 'k6/http';
import { sleep, check } from 'k6';
export const options = { vus: 10, duration: '1m' };
export default function () {
  const payload = JSON.stringify({ showId:'got', targetSeason:6, immersion:3, language:'en' });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.post('http://localhost:8080/recommendations', payload, params);
  check(res, { 'status is 200/503': r => [200,503].includes(r.status) });
  sleep(0.1);
}