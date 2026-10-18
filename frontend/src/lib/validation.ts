const PARTICLES = new Set(['de', 'da', 'do', 'das', 'dos', 'e']);

export function formatProperName(value: string): string {
  const trimmed = value.trim().replace(/\s+/g, ' ');
  if (!trimmed) return '';
  const normalized = trimmed.normalize('NFC');
  return normalized
    .split(/([ -'\u2019])/)
    .map((segment, i, arr) => {
      if (segment === ' ' || segment === '-' || segment === "'" || segment === '\u2019') return segment;
      const lower = segment.toLowerCase();
      if (i === 0) return lower.charAt(0).toUpperCase() + lower.slice(1);
      if (PARTICLES.has(lower) && i < arr.length - 1) return lower;
      return lower.charAt(0).toUpperCase() + lower.slice(1);
    })
    .join('');
}

export function normalizePlate(value: string): string {
  return value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
}

export function isValidPlate(value: string): boolean {
  return /^[A-Z0-9]{7}$/.test(normalizePlate(value));
}

export const LIMITS = {
  apartmentNumber: { min: 1, max: 20 },
  residentName: { min: 1, max: 160 },
  userName: { min: 3, max: 80 },
  fullName: { min: 2, max: 160 },
  password: { min: 12, max: 200 },
  brand: { max: 80 },
  model: { max: 80 },
  color: { max: 40 },
  plate: { exact: 7 },
  justificationManual: { min: 5, max: 1000 },
  justificationGate: { min: 5, max: 500 },
  parkingNumber: { min: 1, max: 20 },
  reportMaxDays: 366,
} as const;

export type FieldErrors = Record<string, string>;

export function validateRequired(value: string, fieldName: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return `${fieldName} é obrigatório.`;
  return null;
}

export function validateLength(value: string, min: number, max: number, fieldName: string): string | null {
  const trimmed = value.trim();
  if (trimmed.length < min) return `${fieldName} deve ter pelo menos ${min} caractere${min > 1 ? 's' : ''}.`;
  if (trimmed.length > max) return `${fieldName} deve ter no máximo ${max} caracteres.`;
  return null;
}

export function validateFullName(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return 'Nome completo é obrigatório.';
  if (trimmed.length < LIMITS.fullName.min) return `Nome completo deve ter pelo menos ${LIMITS.fullName.min} caracteres.`;
  if (trimmed.length > LIMITS.fullName.max) return `Nome completo deve ter no máximo ${LIMITS.fullName.max} caracteres.`;
  return null;
}

export function validateUsername(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return 'Nome de usuário é obrigatório.';
  if (trimmed.length < LIMITS.userName.min) return `Nome de usuário deve ter pelo menos ${LIMITS.userName.min} caracteres.`;
  if (trimmed.length > LIMITS.userName.max) return `Nome de usuário deve ter no máximo ${LIMITS.userName.max} caracteres.`;
  return null;
}

export function validatePassword(value: string, isNew = true): string | null {
  if (!isNew) return null;
  if (!value) return 'Senha é obrigatória.';
  if (value.length < LIMITS.password.min) return `Senha deve ter pelo menos ${LIMITS.password.min} caracteres.`;
  if (value.length > LIMITS.password.max) return `Senha deve ter no máximo ${LIMITS.password.max} caracteres.`;
  if (/^\s+$/.test(value)) return 'Senha não pode conter apenas espaços.';
  return null;
}

export function validateApartmentNumber(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return 'Número do apartamento é obrigatório.';
  if (trimmed.length > LIMITS.apartmentNumber.max) return `Número deve ter no máximo ${LIMITS.apartmentNumber.max} caracteres.`;
  return null;
}

export function validateParkingNumber(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return 'Número da vaga é obrigatório.';
  if (trimmed.length > LIMITS.parkingNumber.max) return `Número deve ter no máximo ${LIMITS.parkingNumber.max} caracteres.`;
  return null;
}

export function validateResidentName(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return 'Nome do morador é obrigatório.';
  if (trimmed.length > LIMITS.residentName.max) return `Nome deve ter no máximo ${LIMITS.residentName.max} caracteres.`;
  return null;
}

export function validatePlate(value: string): string | null {
  if (!value.trim()) return 'Placa é obrigatória.';
  if (!isValidPlate(value)) return 'A placa deve conter exatamente 7 caracteres alfanuméricos.';
  return null;
}

export function validateJustification(value: string, min: number, max: number): string | null {
  const trimmed = value.trim();
  if (!trimmed) return 'Justificativa é obrigatória.';
  if (trimmed.length < min) return `Justificativa deve ter pelo menos ${min} caracteres.`;
  if (trimmed.length > max) return `Justificativa deve ter no máximo ${max} caracteres.`;
  return null;
}

export function validateDateRange(from: string, to: string): string | null {
  if (from && to) {
    const fromDate = new Date(from);
    const toDate = new Date(to);
    if (fromDate > toDate) return 'A data inicial deve ser anterior à data final.';
  }
  return null;
}

export function validateReportDateRange(from: string, to: string): string | null {
  const dateError = validateDateRange(from, to);
  if (dateError) return dateError;
  if (from && to) {
    const fromDate = new Date(from);
    const toDate = new Date(to);
    const diffDays = (toDate.getTime() - fromDate.getTime()) / (1000 * 60 * 60 * 24);
    if (diffDays > LIMITS.reportMaxDays) return `O período não pode exceder ${LIMITS.reportMaxDays} dias.`;
  }
  return null;
}

export function validateParkingTypeRequiresApartment(type: string, apartmentId: string): string | null {
  if (type === 'ASSIGNED' && !apartmentId) return 'Vagas do tipo Própria exigem um apartamento vinculado.';
  return null;
}
