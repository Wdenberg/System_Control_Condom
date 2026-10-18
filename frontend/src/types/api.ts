export type RoleCode = 'SUPER_ADMIN' | 'ADMIN' | 'OPERATOR' | 'ATTENDANT' | 'VIEWER';

export type Permission =
  | 'DASHBOARD_READ'
  | 'APARTMENT_READ' | 'APARTMENT_WRITE'
  | 'PARKING_READ' | 'PARKING_WRITE'
  | 'VEHICLE_READ' | 'VEHICLE_WRITE'
  | 'MOVEMENT_READ' | 'MOVEMENT_MANUAL_WRITE' | 'MOVEMENT_OVERRIDE'
  | 'CAMERA_READ' | 'CAMERA_WRITE' | 'CAMERA_TEST'
  | 'USER_READ' | 'USER_WRITE'
  | 'ROLE_READ' | 'ROLE_WRITE'
  | 'REPORT_READ'
  | 'AUDIT_READ'
  | 'GATE_READ' | 'GATE_WRITE' | 'GATE_TEST';

export interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  role: RoleCode;
  permissions: Permission[];
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  expiresAt: string;
  user: UserSummary;
}

export interface MeResponse {
  user: UserSummary;
}


export interface AdminUser {
  id: number;
  username: string;
  fullName: string;
  role: RoleCode;
  permissions: Permission[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RoleDefinition {
  id: number;
  code: RoleCode;
  active: boolean;
  permissions: Permission[];
}

export interface DashboardSummary {
  activeApartments: number;
  activeResidents: number;
  activeVehicles: number;
  vehiclesInside: number;
  vehiclesOutside: number;
  totalParkingSpaces: number;
  freeParkingSpaces: number;
  occupiedParkingSpaces: number;
  blockedParkingSpaces: number;
  movementsToday: number;
  entriesToday: number;
  exitsToday: number;
  authorizedToday: number;
  deniedToday: number;
  pendingGateOperations: number;
  exhaustedGateOperations: number;
  generatedAt: string;
}

export type ParkingSpaceType = 'ASSIGNED' | 'RENTED' | 'EXTRA' | 'VISITOR';
export type ParkingSpaceStatus = 'AVAILABLE' | 'OCCUPIED' | 'BLOCKED';

export type MovementDirection = 'ENTRY' | 'EXIT';
export type MovementStatus = 'AUTHORIZED' | 'DENIED' | 'OVERRIDDEN';

export interface MovementResult {
  movementId: number;
  plate: string;
  direction: MovementDirection;
  status: MovementStatus;
  decision: string;
  vehicleId: number | null;
  vehicleOwner: string | null;
  apartmentNumber: string | null;
  parkingSpaceId: number | null;
  parkingSpaceNumber: string | null;
  eventTime: string;
  passageState?: PassageState;
}

export interface DashboardActivity {
  movement: MovementResult;
  activityType: string;
}

export interface DashboardOccupancy {
  parkingSpaceId: number;
  parkingSpaceNumber: string;
  apartmentId: number | null;
  apartmentNumber: string | null;
  type: string;
  status: 'AVAILABLE' | 'OCCUPIED' | 'BLOCKED';
  active: boolean;
  assignedPlate: string | null;
  vehicleInside: boolean;
}

export interface DashboardCamera {
  id: number;
  name: string;
  ipAddress: string;
  direction: MovementDirection;
  vendor: string;
  model: string;
  enabled: boolean;
  lastEventAt: string | null;
  lastPlate: string | null;
}

export type GateDriverState = 'READY' | 'CONFIGURED' | 'NOT_CONFIGURED' | 'DISABLED' | 'ERROR';

export interface GateStatus {
  enabled: boolean;
  driver: string;
  entryConfigured: boolean;
  exitConfigured: boolean;
  entryState: GateDriverState;
  entryMessage: string;
  exitState: GateDriverState;
  exitMessage: string;
  safetyMode: string;
}

export interface GateOperation {
  id: number;
  operationKey: string;
  movementId: number;
  plate: string;
  direction: MovementDirection;
  command: string;
  status: string;
  driver: string;
  attempts: number;
  lastError: string | null;
  requestedAt: string;
  sentAt: string | null;
}

export interface DashboardAlert {
  id: string;
  type: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL' | string;
  title: string;
  message: string;
  occurredAt: string;
  movementId: number | null;
  gateOperationId: number | null;
}

export interface Apartment {
  id: number;
  number: string;
  active: boolean;
}

export interface Resident {
  id: number;
  fullName: string;
  apartmentId: number;
  apartmentNumber: string;
  active: boolean;
}

export interface Vehicle {
  id: number;
  plate: string;
  brand: string | null;
  model: string | null;
  color: string | null;
  parkingSpaceId: number | null;
  parkingSpaceNumber: string | null;
  apartmentId: number;
  apartmentNumber: string;
  residents: Array<{ id: number; fullName: string }>;
  active: boolean;
}

export interface ParkingSpace {
  id: number;
  number: string;
  apartmentId: number | null;
  apartmentNumber: string | null;
  type: ParkingSpaceType;
  status: ParkingSpaceStatus;
  active: boolean;
}

export interface Camera {
  id: number;
  name: string;
  ipAddress: string;
  direction: MovementDirection;
  vendor: string;
  model: string;
  channelId: number;
  enabled: boolean;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}


export interface AccessReportRow {
  movementId: number;
  eventTime: string;
  plate: string;
  direction: MovementDirection;
  source: string;
  status: MovementStatus;
  decision: string;
  confidenceLevel: number | null;
  vehicleOwner: string | null;
  apartmentNumber: string | null;
  parkingSpaceNumber: string | null;
  cameraName: string | null;
  cameraIp: string | null;
}

export interface AccessReportSummary {
  from: string;
  to: string;
  total: number;
  entries: number;
  exits: number;
  authorized: number;
  denied: number;
  overridden: number;
  decisions: Record<string, number>;
}

export interface OccupancyReportRow {
  parkingSpaceId: number;
  parkingSpaceNumber: string;
  apartmentId: number | null;
  apartmentNumber: string | null;
  type: string;
  status: 'AVAILABLE' | 'OCCUPIED' | 'BLOCKED';
  active: boolean;
  assignedPlate: string | null;
  vehicleInside: boolean;
}

export interface GateReportRow {
  id: number;
  movementId: number;
  operationKey: string;
  plate: string;
  direction: MovementDirection;
  command: string;
  status: string;
  driver: string;
  attempts: number;
  lastError: string | null;
  requestedAt: string;
  sentAt: string | null;
}

export interface AuditLogRow {
  id: number;
  userId: number | null;
  username: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  justification: string | null;
  details: string | null;
  outcome: 'SUCCESS' | 'FAILURE' | string;
  ipAddress: string | null;
  userAgent: string | null;
  correlationId: string | null;
  requestMethod: string | null;
  requestPath: string | null;
  createdAt: string;
}

export interface ObservabilitySummary {
  application: string;
  status: string;
  metricsEndpoint: string;
  healthEndpoint: string;
  realtimeConnectedClients: number;
}

export type PassageState = 'AUTHORIZED' | 'GATE_REQUESTED' | 'COMMAND_ACCEPTED' | 'COMPLETED' | 'DENIED' | 'FAILED' | 'UNKNOWN' | 'RECONCILIATION_REQUIRED';
export interface UnifiedRegistrationPayload {
  requestId: string; apartmentNumber: string; apartmentActive: boolean;
  residents: { fullName: string; active: boolean }[];
  parkingNumber: string; parkingType: string; parkingActive: boolean;
  plate: string; brand: string; model: string; color: string; vehicleActive: boolean;
}
export interface UnifiedRegistrationResult { apartmentId: number; residentIds: number[]; parkingSpaceId: number | null; vehicleId: number | null }
