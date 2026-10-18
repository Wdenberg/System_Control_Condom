#!/usr/bin/env node
/**
 * Reproduce from the repository root with Node 24:
 *   node --experimental-strip-types scripts/frontend-smoke.mjs
 * Save evidence:
 *   node --experimental-strip-types scripts/frontend-smoke.mjs > docs/validation/frontend-smoke.log 2>&1
 *
 * Imports the real auth-storage.ts and services/api.ts; only browser storage,
 * the Vite environment and HTTP transport are replaced with local fixtures.
 * No requests reach the backend or physical gate equipment.
 * Acorn checks plain .ts syntax after Node strips types. This does not check
 * TSX, TypeScript types, browser rendering, coverage or the full Vitest suite.
 */
import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { registerHooks, stripTypeScriptTypes } from 'node:module';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const repository = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const sourceDirectory = join(repository, 'frontend/src');

async function typescriptFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) files.push(...await typescriptFiles(path));
    else if (entry.name.endsWith('.ts') && !entry.name.endsWith('.d.ts')) files.push(path);
  }
  return files;
}

async function checkPlainTypescriptSyntax() {
  const chunksDirectory = join(repository, 'frontend/node_modules/vitest/dist/chunks');
  let chunks;
  try {
    chunks = await readdir(chunksDirectory);
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    console.log('SKIP plain .ts syntax: installed Vitest/Acorn bundle unavailable.');
    return;
  }
  const acornFile = chunks.find((name) => /^acorn\..+\.js$/.test(name));
  if (!acornFile) {
    console.log('SKIP plain .ts syntax: this Vitest distribution has no bundled Acorn chunk.');
    return;
  }
  // This is intentionally an optional offline check of the installed bundle.
  // Vitest internal filenames/exports can change; it is not a build dependency.
  const acorn = await import(pathToFileURL(join(chunksDirectory, acornFile)).href);
  const parse = acorn.parse ?? acorn.p;
  if (typeof parse !== 'function') {
    console.log('SKIP plain .ts syntax: bundled Acorn export is unsupported.');
    return;
  }
  const files = await typescriptFiles(sourceDirectory);
  for (const file of files) {
    const source = await readFile(file, 'utf8');
    const javascript = stripTypeScriptTypes(source, { mode: 'strip', sourceUrl: file });
    parse(javascript, { ecmaVersion: 'latest', sourceType: 'module' });
    console.log(`PASS syntax ${relative(repository, file)}`);
  }
  console.log(`PASS plain .ts syntax: ${files.length} files; TSX/declarations excluded; no type checking.`);
}

console.log(`Frontend offline smoke | ${new Date().toISOString()} | Node ${process.version}`);
console.log('Command: node --experimental-strip-types scripts/frontend-smoke.mjs');
console.log('Scope: real session storage and HTTP client code, local fixtures; no network or hardware.');

await checkPlainTypescriptSyntax();

const moduleHooks = registerHooks({
  resolve(specifier, context, nextResolve) {
    if (specifier === '@/config/env') {
      return { shortCircuit: true, url: 'data:text/javascript,export const env = {apiBaseUrl:"/api"}' };
    }
    if (specifier.startsWith('@/')) {
      return nextResolve(pathToFileURL(resolve(sourceDirectory, `${specifier.slice(2)}.ts`)).href, context);
    }
    return nextResolve(specifier, context);
  },
});

const originalFetch = globalThis.fetch;
const originalWindow = Object.getOwnPropertyDescriptor(globalThis, 'window');
const originalStorage = Object.getOwnPropertyDescriptor(globalThis, 'sessionStorage');
const storedValues = new Map();
Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: {
  getItem: (key) => storedValues.get(key) ?? null,
  setItem: (key, value) => storedValues.set(key, value),
  removeItem: (key) => storedValues.delete(key),
} });
Object.defineProperty(globalThis, 'window', { configurable: true, value: new EventTarget() });

try {
  const { clearSession, readSession, saveSession } = await import(pathToFileURL(join(sourceDirectory, 'auth/auth-storage.ts')).href);
  const { api, ApiError } = await import(pathToFileURL(join(sourceDirectory, 'services/api.ts')).href);
  const fixtureSession = {
    accessToken: 'local-fixture-token', tokenType: 'Bearer',
    expiresAt: new Date(Date.now() + 60_000).toISOString(),
    user: { id: 1, username: 'fixture', fullName: 'Usuário de teste', role: 'VIEWER', permissions: [] },
  };
  saveSession(fixtureSession);
  assert.deepEqual(readSession(), fixtureSession);
  saveSession({ ...fixtureSession, expiresAt: 'invalid-date' });
  assert.equal(readSession(), null);
  saveSession({ ...fixtureSession, expiresAt: new Date(Date.now() - 1).toISOString() });
  assert.equal(readSession(), null);
  sessionStorage.setItem('garage-control.auth', '{invalid');
  assert.equal(readSession(), null);
  clearSession();
  assert.equal(storedValues.size, 0);
  console.log('PASS auth-storage: valid session, invalid expiry, expired session, corrupt JSON, clear.');

  saveSession(fixtureSession);
  const registration = {
    requestId: 'e876af92-5080-4508-948d-5ab4fe6d13ab', apartmentNumber: '101', apartmentActive: true,
    residents: [{ fullName: 'Maria Silva', active: true }], parkingNumber: '127', parkingType: 'ASSIGNED',
    parkingActive: true, plate: 'ABC1D23', brand: 'Fiat', model: 'Uno', color: 'Branco', vehicleActive: false,
  };
  const result = { apartmentId: 1, residentIds: [2], parkingSpaceId: 3, vehicleId: 4 };
  let calls = 0;
  globalThis.fetch = async (url, init) => {
    calls++;
    assert.equal(url, '/api/v1/registrations');
    assert.equal(init.method, 'POST');
    assert.equal(init.headers.get('Authorization'), `Bearer ${fixtureSession.accessToken}`);
    assert.deepEqual(JSON.parse(init.body), registration);
    assert.ok(init.signal instanceof AbortSignal);
    return Response.json(result, { status: 201 });
  };
  assert.deepEqual(await api.registrations.create(registration), result);
  assert.equal(calls, 1);
  console.log('PASS API registration: single POST, exact atomic payload, bearer and cancellation signal.');

  globalThis.fetch = async () => Response.json({ message: 'Conflict fixture' }, { status: 409 });
  await assert.rejects(api.registrations.create(registration), (error) =>
    error instanceof ApiError && error.status === 409 && error.body.message === 'Conflict fixture');
  console.log('PASS API conflict: structured 409 is preserved.');

  const controller = new AbortController();
  const timeout = new DOMException('Local timeout fixture', 'TimeoutError');
  calls = 0;
  globalThis.fetch = async (_url, init) => {
    calls++;
    assert.equal(init.signal, controller.signal);
    throw timeout;
  };
  await assert.rejects(api.registrations.create(registration, controller.signal), (error) => error === timeout);
  assert.equal(calls, 1);
  console.log('PASS API failure: supplied signal preserved; uncertain response has no automatic retry.');

  let unauthorizedEvents = 0;
  window.addEventListener('auth:unauthorized', () => unauthorizedEvents++);
  globalThis.fetch = async (url, init) => {
    assert.equal(url, '/api/v1/auth/revoke');
    assert.equal(init.method, 'POST');
    assert.equal(new Headers(init.headers).get('Authorization'), 'Bearer original-fixture-token');
    return new Response(null, { status: 401 });
  };
  await api.revoke('original-fixture-token');
  assert.equal(unauthorizedEvents, 0);
  console.log('PASS API revoke: original token supplied; 401 does not trigger an unauthorized event loop.');

  globalThis.fetch = async () => new Response(null, { status: 401 });
  await assert.rejects(api.me(), /UNAUTHORIZED/);
  assert.equal(unauthorizedEvents, 1);
  assert.equal(readSession(), null);
  console.log('PASS API unauthorized: 401 clears the session and emits one unauthorized event.');
  console.log('PASS frontend offline smoke. Full Vitest/RTL, TSX, typecheck, build and coverage remain separate checks.');
} finally {
  globalThis.fetch = originalFetch;
  for (const [name, descriptor] of [['window', originalWindow], ['sessionStorage', originalStorage]]) {
    if (descriptor) Object.defineProperty(globalThis, name, descriptor);
    else delete globalThis[name];
  }
  moduleHooks.deregister();
}
