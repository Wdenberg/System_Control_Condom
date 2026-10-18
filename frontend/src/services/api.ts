import { env } from '@/config/env';
import { clearSession, readSession } from '@/auth/auth-storage';
import type {
  UnifiedRegistrationPayload,
  UnifiedRegistrationResult,
  Apartment,
  Camera,
  DashboardActivity,
  DashboardAlert,
  DashboardCamera,
  DashboardOccupancy,
  DashboardSummary,
  LoginResponse,
  MeResponse,
  PageResponse,
  GateStatus,
  GateOperation,
  ParkingSpace,
  Resident,
  Vehicle,
  AdminUser,
  RoleDefinition,
  RoleCode,
  AccessReportRow,
  AccessReportSummary,
  OccupancyReportRow,
  GateReportRow,
  AuditLogRow,
  ObservabilitySummary,
} from '@/types/api';

export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(message: string, status: number, body?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

export function getApiErrorMessage(error: unknown, fallback = 'Não foi possível concluir a operação.') {
  if (typeof error === 'object' && error !== null && 'name' in error && (error.name === 'TimeoutError' || error.name === 'AbortError')) return 'Não foi possível confirmar a resposta. Tente novamente sem alterar os dados para verificar o resultado com segurança.';
  if (error instanceof ApiError) {
    const body = error.body as { message?: string; detail?: string; error?: string } | null;
    return body?.message || body?.detail || (error.status === 409 ? 'O registro já existe ou está em conflito.' : fallback);
  }
  return error instanceof Error && error.message !== 'UNAUTHORIZED' ? error.message : fallback;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = readSession();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (session?.accessToken) headers.set('Authorization', `Bearer ${session.accessToken}`);

  const response = await fetch(`${env.apiBaseUrl}${path}`, { ...init, headers });
  if (response.status === 401) {
    clearSession();
    window.dispatchEvent(new Event('auth:unauthorized'));
    throw new Error('UNAUTHORIZED');
  }

  const contentType = response.headers.get('content-type') ?? '';
  const data = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    const message = typeof data === 'object' && data !== null && 'message' in data && typeof data.message === 'string'
      ? data.message
      : `Requisição falhou com status ${response.status}`;
    throw new ApiError(message, response.status, data);
  }

  return data as T;
}

async function requestBlob(path: string): Promise<Blob> {
  const session = readSession();
  const headers = new Headers();
  headers.set('Accept', 'text/csv');
  if (session?.accessToken) headers.set('Authorization', `Bearer ${session.accessToken}`);
  const response = await fetch(`${env.apiBaseUrl}${path}`, { headers });
  if (response.status === 401) {
    clearSession();
    window.dispatchEvent(new Event('auth:unauthorized'));
    throw new Error('UNAUTHORIZED');
  }
  if (!response.ok) {
    const text = await response.text();
    throw new ApiError(text || `Requisição falhou com status ${response.status}`, response.status, text);
  }
  return response.blob();
}

export const api = {
  login: (username: string, password: string) => request<LoginResponse>('/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  }),

  me: () => request<MeResponse>('/v1/auth/me'),
  revoke: async (token: string): Promise<void> => {
    const response = await fetch(`${env.apiBaseUrl}/v1/auth/revoke`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } });
    if (!response.ok && response.status !== 401) throw new ApiError('Não foi possível revogar a sessão.', response.status);
  },
  registrations: {
    create: (payload: UnifiedRegistrationPayload, signal?: AbortSignal) => request<UnifiedRegistrationResult>('/v1/registrations', {
      method: 'POST', body: JSON.stringify(payload), signal: signal ?? AbortSignal.timeout(15_000),
    }),
  },

  dashboard: {
    summary: () => request<DashboardSummary>('/v1/dashboard/summary'),
    activity: (limit = 20) => request<DashboardActivity[]>(`/v1/dashboard/activity?limit=${limit}`),
    occupancy: () => request<DashboardOccupancy[]>('/v1/dashboard/occupancy'),
    cameras: () => request<DashboardCamera[]>('/v1/dashboard/cameras'),
    alerts: (limit = 20) => request<DashboardAlert[]>(`/v1/dashboard/alerts?limit=${limit}`),
  },

  apartments: {
    list: () => request<Apartment[]>('/v1/apartments'),
    get: (id: number) => request<Apartment>(`/v1/apartments/${id}`),
    create: (payload: { number: string; active: boolean }) => request<Apartment>('/v1/apartments', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
    update: (id: number, payload: { number: string; active: boolean }) => request<Apartment>(`/v1/apartments/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
    residents: (id: number) => request<Resident[]>(`/v1/apartments/${id}/residents`),
    createResident: (apartmentId: number, payload: { fullName: string; active: boolean }) =>
      request<Resident>(`/v1/apartments/${apartmentId}/residents`, {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    updateResident: (apartmentId: number, residentId: number, payload: { fullName: string; active: boolean }) =>
      request<Resident>(`/v1/apartments/${apartmentId}/residents/${residentId}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      }),
  },

  vehicles: {
    list: () => request<Vehicle[]>('/v1/vehicles'),
    get: (id: number) => request<Vehicle>(`/v1/vehicles/${id}`),
    findByPlate: (plate: string) => request<Vehicle>(`/v1/vehicles/plate/${encodeURIComponent(plate)}`),
    create: (payload: {
      plate: string;
      brand?: string;
      model?: string;
      color?: string;
      residentIds: number[];
      parkingSpaceId?: number | null;
      active: boolean;
    }) => request<Vehicle>('/v1/vehicles', { method: 'POST', body: JSON.stringify(payload) }),
    update: (id: number, payload: {
      plate: string;
      brand?: string;
      model?: string;
      color?: string;
      residentIds: number[];
      parkingSpaceId?: number | null;
      active: boolean;
    }) => request<Vehicle>(`/v1/vehicles/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  },

  parking: {
    list: () => request<ParkingSpace[]>('/v1/parking-spaces'),
    get: (id: number) => request<ParkingSpace>(`/v1/parking-spaces/${id}`),
    listByApartment: (apartmentId: number) => request<ParkingSpace[]>(`/v1/parking-spaces/apartment/${apartmentId}`),
    create: (payload: { number: string; apartmentId: number | null; type: string; active: boolean }) =>
      request<ParkingSpace>('/v1/parking-spaces', { method: 'POST', body: JSON.stringify(payload) }),
    update: (id: number, payload: { number: string; apartmentId: number | null; type: string; active: boolean }) =>
      request<ParkingSpace>(`/v1/parking-spaces/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  },

  movements: {
    confirmPassage: (id: number, passed: boolean, justification: string, controllerSettled = false) => request<import('@/types/api').MovementResult>(`/v1/movements/${id}/passage-confirmation`, { method: 'POST', body: JSON.stringify({ passed, justification, controllerSettled }) }),
    list: (page = 0, size = 30) => request<PageResponse<DashboardActivity['movement']>>(`/v1/movements?page=${page}&size=${size}`),
    createManual: (payload: { plate: string; direction: 'ENTRY' | 'EXIT'; justification: string }) =>
      request<DashboardActivity['movement']>('/v1/movements/manual', { method: 'POST', body: JSON.stringify(payload) }),
    override: (id: number, justification: string) =>
      request<DashboardActivity['movement']>(`/v1/movements/${id}/override`, { method: 'POST', body: JSON.stringify({ justification }) }),
  },

  gates: {
    status: () => request<GateStatus>('/v1/gates/status'),
    operations: (page = 0, size = 10) => request<PageResponse<GateOperation>>(`/v1/gates/operations?page=${page}&size=${size}`),
    test: (direction: 'ENTRY' | 'EXIT', justification: string) => request<GateStatus>(`/v1/gates/test/${direction}`, { method: 'POST', body: JSON.stringify({ justification }) }),
    retry: (id: number, justification: string) => request<GateOperation>(`/v1/gates/operations/${id}/retry`, { method: 'POST', body: JSON.stringify({ justification }) }),
  },

  reports: {
    access: (params: Record<string, string | number | undefined> = {}, page = 0, size = 30) => {
      const query = new URLSearchParams();
      for (const [key, value] of Object.entries({ ...params, page, size })) {
        if (value !== undefined && String(value) !== '') query.set(key, String(value));
      }
      return request<PageResponse<AccessReportRow>>(`/v1/reports/access?${query.toString()}`);
    },
    accessSummary: (params: Record<string, string | number | undefined> = {}) => {
      const query = new URLSearchParams();
      for (const [key, value] of Object.entries(params)) {
        if (value !== undefined && String(value) !== '') query.set(key, String(value));
      }
      const suffix = query.toString();
      return request<AccessReportSummary>(`/v1/reports/access/summary${suffix ? `?${suffix}` : ''}`);
    },
    exportAccessCsv: (params: Record<string, string | number | undefined> = {}) => {
      const query = new URLSearchParams();
      for (const [key, value] of Object.entries(params)) {
        if (value !== undefined && String(value) !== '') query.set(key, String(value));
      }
      const suffix = query.toString();
      return requestBlob(`/v1/reports/access.csv${suffix ? `?${suffix}` : ''}`);
    },
    occupancy: () => request<OccupancyReportRow[]>('/v1/reports/occupancy'),
    gates: (params: { from?: string; to?: string; status?: string } = {}, page = 0, size = 30) => {
      const query = new URLSearchParams({ page: String(page), size: String(size) });
      for (const [key, value] of Object.entries(params)) {
        if (value) query.set(key, value);
      }
      return request<PageResponse<GateReportRow>>(`/v1/reports/gates?${query.toString()}`);
    },
  },

  cameras: {
    list: () => request<Camera[]>('/v1/cameras'),
  },

  users: {
    list: () => request<AdminUser[]>('/v1/users'),
    get: (id: number) => request<AdminUser>(`/v1/users/${id}`),
    create: (payload: { username: string; password: string; fullName: string; role: RoleCode; active: boolean }) =>
      request<AdminUser>('/v1/users', { method: 'POST', body: JSON.stringify(payload) }),
    update: (id: number, payload: { fullName: string; role: RoleCode; active: boolean }) =>
      request<AdminUser>(`/v1/users/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
    resetPassword: (id: number, newPassword: string) =>
      request<AdminUser>(`/v1/users/${id}/password`, { method: 'POST', body: JSON.stringify({ newPassword }) }),
  },


  system: {
    observability: () => request<ObservabilitySummary>('/v1/system/observability'),
  },

  audit: {
    list: (params: {
      from?: string;
      to?: string;
      username?: string;
      action?: string;
      entityType?: string;
      entityId?: string;
      ipAddress?: string;
      correlationId?: string;
    } = {}, page = 0, size = 30) => {
      const query = new URLSearchParams({ page: String(page), size: String(size), sort: 'createdAt,desc' });
      for (const [key, value] of Object.entries(params)) {
        if (value) query.set(key, value);
      }
      return request<PageResponse<AuditLogRow>>(`/v1/audit?${query.toString()}`);
    },
    csv: (params: {
      from?: string;
      to?: string;
      username?: string;
      action?: string;
      entityType?: string;
      entityId?: string;
      ipAddress?: string;
      correlationId?: string;
    } = {}) => {
      const query = new URLSearchParams();
      for (const [key, value] of Object.entries(params)) {
        if (value) query.set(key, value);
      }
      const suffix = query.toString();
      return requestBlob(`/v1/audit/csv${suffix ? `?${suffix}` : ''}`);
    },
  },
  roles: {
    list: () => request<RoleDefinition[]>('/v1/roles'),
  },
};
