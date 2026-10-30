import http from 'k6/http';
import { check } from 'k6';

const base = __ENV.BASE_URL || 'http://localhost:58080';
export const options = {
  vus: 3,
  duration: '10s',
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<500'], checks: ['rate==1'] },
};
function request(method, path, token, body) {
  const response = http.request(method, `${base}/api/v1${path}`, body ? JSON.stringify(body) : null, {
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  });
  if (response.status >= 400) throw new Error(`Setup failed: ${method} ${path}: ${response.status}`);
  return response.json();
}
function account(role) {
  const email = `smoke-${role}-${Date.now()}-${Math.random()}@example.test`;
  const password = 'test-only-password-123';
  request('POST', '/auth/register', null, { email, password, role });
  return request('POST', '/auth/login', null, { email, password }).accessToken;
}
export function setup() {
  const organizer = account('ORGANIZER');
  const owner = account('ATHLETE');
  const team = request('POST', '/teams', owner, { name: 'Estrelas Smoke', city: 'Recife', state: 'PE' });
  const tournament = request('POST', '/tournaments', organizer, { name: 'Copa Smoke', city: 'Recife', state: 'PE', venueName: 'Arena', startDate: '2027-01-10', endDate: '2027-01-12', registrationDeadline: '2027-01-01', visibility: 'PRIVATE' });
  const category = request('POST', `/tournaments/${tournament.id}/categories`, organizer, { name: 'Livre', modality: 'FUTSAL', minAthletes: 7, maxAthletes: 12, minTeams: 2, maxTeams: 16, paymentModel: 'TEAM', priceInCents: 0, competitionFormat: 'GROUP_STAGE', matchDurationMinutes: 40, intervalBetweenMatchesMinutes: 10, minimumRestMinutes: 60, schedulingStrategy: 'BALANCED_REST' });
  request('POST', `/tournaments/${tournament.id}/publish`, organizer);
  const invite = request('POST', `/tournaments/${tournament.id}/categories/${category.id}/invitations`, organizer, { teamId: team.id });
  const accepted = request('POST', `/teams/${team.id}/tournament-invitations/${invite.id}/accept`, owner);
  check(accepted, { 'acceptance creates accepted invitation': (value) => value.status === 'ACCEPTED' });
  return { owner, teamId: team.id, tournamentId: tournament.id };
}
export default function (data) {
  const headers = { Authorization: `Bearer ${data.owner}` };
  const overview = http.get(`${base}/api/v1/tournaments/${data.tournamentId}/overview`, { headers });
  check(overview, { 'private overview is accessible to accepted admin': (r) => r.status === 200 && r.json().tournament.visibility === 'PRIVATE' });
  const registrations = http.get(`${base}/api/v1/teams/${data.teamId}/registrations`, { headers });
  check(registrations, { 'exactly one draft registration persists': (r) => r.status === 200 && r.json().length === 1 && r.json()[0].status === 'DRAFT' });
}
