import { expect, type APIRequestContext } from '@playwright/test';

export interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  role: string;
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  expiresAt: string;
  user: UserSummary;
}

export async function loginApi(request: APIRequestContext, username: string, password: string): Promise<LoginResponse> {
  const response = await request.post('/api/v1/auth/login', {
    data: { username, password },
  });
  await expect(response).toBeOK();
  return response.json() as Promise<LoginResponse>;
}

export async function authorizedRequest(
  request: APIRequestContext,
  accessToken: string,
  method: string,
  path: string,
  data?: unknown,
) {
  const response = await request.fetch(path, {
    method,
    headers: { Authorization: `Bearer ${accessToken}`, Accept: 'application/json' },
    data,
  });
  return response;
}

export async function seedParkingScenario(request: APIRequestContext, accessToken: string) {
  const suffix = Date.now().toString().slice(-5);
  const apartmentResponse = await authorizedRequest(request, accessToken, 'POST', '/api/v1/apartments', {
    number: `E2E-${suffix}`,
    active: true,
  });
  await expect(apartmentResponse).toBeOK();
  const apartment = await apartmentResponse.json() as { id: number; number: string };

  const residentResponse = await authorizedRequest(request, accessToken, 'POST', `/api/v1/apartments/${apartment.id}/residents`, {
    fullName: `Morador E2E ${suffix}`,
    active: true,
  });
  await expect(residentResponse).toBeOK();
  const resident = await residentResponse.json() as { id: number; fullName: string };

  const spaceResponse = await authorizedRequest(request, accessToken, 'POST', '/api/v1/parking-spaces', {
    number: `E2E-${suffix}`,
    apartmentId: apartment.id,
    type: 'ASSIGNED',
    active: true,
  });
  await expect(spaceResponse).toBeOK();
  const space = await spaceResponse.json() as { id: number; number: string };

  const plate = `E2E${suffix.slice(-4)}`.padEnd(7, 'X').slice(0, 7);
  const vehicleResponse = await authorizedRequest(request, accessToken, 'POST', '/api/v1/vehicles', {
    plate,
    brand: 'Hikvision Test',
    model: 'E2E',
    color: 'White',
    residentIds: [resident.id],
    parkingSpaceId: space.id,
    active: true,
  });
  await expect(vehicleResponse).toBeOK();
  const vehicle = await vehicleResponse.json() as { id: number; plate: string };

  return { apartment, resident, space, vehicle, plate };
}
