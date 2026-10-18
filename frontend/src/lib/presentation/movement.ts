import type { MovementDirection } from '@/types/api';

export const movementDirectionLabels: Record<MovementDirection, string> = {
  ENTRY: 'Entrada',
  EXIT: 'Saída',
};

export const movementStatusLabels: Record<string, string> = {
  AUTHORIZED: 'Autorizado',
  DENIED: 'Negado',
  OVERRIDDEN: 'Sobrescrito',
};

export const accessDecisionLabels: Record<string, string> = {
  AUTHORIZED: 'Autorizado',
  DENIED: 'Negado',
  OPERATION_PENDING: 'Passagem anterior aguardando confirmação',
  UNKNOWN_VEHICLE: 'Veículo não cadastrado',
  DUPLICATE_ENTRY: 'Entrada duplicada',
  EXIT_WITHOUT_ENTRY: 'Saída sem entrada registrada',
  LOW_CONFIDENCE: 'Baixa confiança na leitura',
  VEHICLE_INACTIVE: 'Veículo inativo',
  RESIDENT_INACTIVE: 'Morador ou apartamento inativo',
  NO_PARKING_ASSIGNED: 'Sem vaga atribuída',
  PARKING_SPACE_INACTIVE: 'Vaga inativa',
  PARKING_SPACE_BLOCKED: 'Vaga bloqueada',
  PARKING_SPACE_OCCUPIED: 'Vaga ocupada',
  INVALID_DIRECTION: 'Direção inválida',
  MANUAL_AUTHORIZED: 'Autorização manual',
  OVERRIDE_AUTHORIZED: 'Override autorizado',
};

export const movementStatusClasses: Record<string, string> = {
  AUTHORIZED: 'authorized',
  DENIED: 'denied',
  OVERRIDDEN: 'overridden',
};

export function getMovementDirectionLabel(direction: MovementDirection): string {
  return movementDirectionLabels[direction] ?? direction;
}

export function getMovementStatusLabel(status: string): string {
  return movementStatusLabels[status] ?? status.replaceAll('_', ' ');
}

export function getAccessDecisionLabel(decision: string): string {
  return accessDecisionLabels[decision] ?? decision.replaceAll('_', ' ');
}

export function getMovementStatusClass(status: string): string {
  return movementStatusClasses[status] ?? 'pending';
}

export function getDirectionClass(direction: MovementDirection): string {
  return direction === 'ENTRY' ? 'entry' : 'exit';
}

export const passageStateLabels: Record<string, string> = {
  AUTHORIZED: 'Aguardando passagem', GATE_REQUESTED: 'Abertura solicitada', COMMAND_ACCEPTED: 'Comando aceito; passagem não confirmada',
  COMPLETED: 'Passagem confirmada', DENIED: 'Negado', FAILED: 'Sem passagem', UNKNOWN: 'Resultado incerto', RECONCILIATION_REQUIRED: 'Reconciliação necessária',
};
