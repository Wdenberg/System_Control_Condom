export const gateOperationStatusLabels: Record<string, string> = {
  PENDING: 'Pendente',
  PROCESSING: 'Em processamento',
  UNKNOWN: 'Resultado incerto — reconciliar',
  SENT: 'Enviado',
  EXHAUSTED: 'Esgotado',
  DISABLED: 'Desabilitado',
};

export const gateDriverStateLabels: Record<string, string> = {
  READY: 'Pronto',
  CONFIGURED: 'Configurado',
  NOT_CONFIGURED: 'Não configurado',
  DISABLED: 'Desabilitado',
  ERROR: 'Erro',
};

export const gateOperationStatusClasses: Record<string, string> = {
  PENDING: 'pending',
  PROCESSING: 'pending',
  UNKNOWN: 'denied',
  SENT: 'authorized',
  EXHAUSTED: 'denied',
  DISABLED: 'overridden',
};

export function getGateOperationStatusLabel(status: string): string {
  return gateOperationStatusLabels[status] ?? status;
}

export function getGateDriverStateLabel(state: string): string {
  return gateDriverStateLabels[state] ?? state;
}

export function getGateOperationStatusClass(status: string): string {
  return gateOperationStatusClasses[status] ?? 'pending';
}
