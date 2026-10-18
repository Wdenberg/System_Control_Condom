import { describe, expect, it } from 'vitest';
import {
  formatProperName,
  normalizePlate,
  isValidPlate,
  LIMITS,
  validateRequired,
  validateLength,
  validateFullName,
  validateUsername,
  validatePassword,
  validateApartmentNumber,
  validateParkingNumber,
  validateResidentName,
  validatePlate,
  validateJustification,
  validateDateRange,
  validateReportDateRange,
  validateParkingTypeRequiresApartment,
} from '@/lib/validation';

describe('formatProperName', () => {
  it('normalizes unicode and trims', () => {
    expect(formatProperName('  joão  silva  ')).toBe('João Silva');
  });

  it('lowercases particles', () => {
    expect(formatProperName('MARIA DE LIMA')).toBe('Maria de Lima');
    expect(formatProperName('JOAO DA SILVA')).toBe('Joao da Silva');
    expect(formatProperName('PEDRO DOS SANTOS')).toBe('Pedro dos Santos');
    expect(formatProperName('ANA DAS CRUZES')).toBe('Ana das Cruzes');
    expect(formatProperName('CARLOS E FERNANDA')).toBe('Carlos e Fernanda');
  });

  it('handles mixed case', () => {
    expect(formatProperName('ana maria de souza')).toBe('Ana Maria de Souza');
  });

  it('returns empty for empty input', () => {
    expect(formatProperName('')).toBe('');
    expect(formatProperName('   ')).toBe('');
  });

  it('handles already formatted names', () => {
    expect(formatProperName('João Silva')).toBe('João Silva');
  });
});

describe('normalizePlate', () => {
  it('removes spaces and hyphens', () => {
    expect(normalizePlate('ABC-1D23')).toBe('ABC1D23');
    expect(normalizePlate('ABC 1D23')).toBe('ABC1D23');
  });

  it('uppercases', () => {
    expect(normalizePlate('abc1d23')).toBe('ABC1D23');
  });
});

describe('isValidPlate', () => {
  it('accepts valid plates', () => {
    expect(isValidPlate('ABC1D23')).toBe(true);
    expect(isValidPlate('abc1d23')).toBe(true);
    expect(isValidPlate('ABC-1D23')).toBe(true);
  });

  it('rejects invalid plates', () => {
    expect(isValidPlate('')).toBe(false);
    expect(isValidPlate('ABC1D2')).toBe(false);
    expect(isValidPlate('ABC1D234')).toBe(false);
  });
});

describe('validateRequired', () => {
  it('returns null for non-empty', () => {
    expect(validateRequired('hello', 'Campo')).toBeNull();
  });

  it('returns error for empty', () => {
    expect(validateRequired('', 'Campo')).toBe('Campo é obrigatório.');
    expect(validateRequired('   ', 'Campo')).toBe('Campo é obrigatório.');
  });
});

describe('validateLength', () => {
  it('returns null for valid length', () => {
    expect(validateLength('hello', 2, 10, 'Campo')).toBeNull();
  });

  it('returns error for too short', () => {
    expect(validateLength('hi', 3, 10, 'Campo')).toBe('Campo deve ter pelo menos 3 caracteres.');
  });

  it('returns error for too long', () => {
    expect(validateLength('a'.repeat(11), 2, 10, 'Campo')).toBe('Campo deve ter no máximo 10 caracteres.');
  });
});

describe('validateFullName', () => {
  it('returns null for valid name', () => {
    expect(validateFullName('João Silva')).toBeNull();
  });

  it('requires value', () => {
    expect(validateFullName('')).toBe('Nome completo é obrigatório.');
  });

  it('rejects too short', () => {
    expect(validateFullName('A')).toBe('Nome completo deve ter pelo menos 2 caracteres.');
  });

  it('rejects too long', () => {
    expect(validateFullName('A'.repeat(161))).toBe('Nome completo deve ter no máximo 160 caracteres.');
  });
});

describe('validateUsername', () => {
  it('returns null for valid username', () => {
    expect(validateUsername('admin')).toBeNull();
  });

  it('requires value', () => {
    expect(validateUsername('')).toBe('Nome de usuário é obrigatório.');
  });

  it('rejects too short', () => {
    expect(validateUsername('ab')).toBe('Nome de usuário deve ter pelo menos 3 caracteres.');
  });

  it('rejects too long', () => {
    expect(validateUsername('A'.repeat(81))).toBe('Nome de usuário deve ter no máximo 80 caracteres.');
  });
});

describe('validatePassword', () => {
  it('returns null for valid password (new)', () => {
    expect(validatePassword('senha1234567', true)).toBeNull();
  });

  it('returns null for empty password (editing)', () => {
    expect(validatePassword('', false)).toBeNull();
  });

  it('requires password for new', () => {
    expect(validatePassword('', true)).toBe('Senha é obrigatória.');
  });

  it('rejects too short', () => {
    expect(validatePassword('12345', true)).toBe('Senha deve ter pelo menos 12 caracteres.');
  });

  it('rejects spaces only', () => {
    expect(validatePassword('            ', true)).toBe('Senha não pode conter apenas espaços.');
  });
});

describe('validateApartmentNumber', () => {
  it('returns null for valid number', () => {
    expect(validateApartmentNumber('101')).toBeNull();
  });

  it('requires value', () => {
    expect(validateApartmentNumber('')).toBe('Número do apartamento é obrigatório.');
  });

  it('rejects too long', () => {
    expect(validateApartmentNumber('A'.repeat(21))).toBe('Número deve ter no máximo 20 caracteres.');
  });
});

describe('validateParkingNumber', () => {
  it('returns null for valid number', () => {
    expect(validateParkingNumber('A1')).toBeNull();
  });

  it('requires value', () => {
    expect(validateParkingNumber('')).toBe('Número da vaga é obrigatório.');
  });

  it('rejects too long', () => {
    expect(validateParkingNumber('A'.repeat(21))).toBe('Número deve ter no máximo 20 caracteres.');
  });
});

describe('validateResidentName', () => {
  it('returns null for valid name', () => {
    expect(validateResidentName('João')).toBeNull();
  });

  it('requires value', () => {
    expect(validateResidentName('')).toBe('Nome do morador é obrigatório.');
  });

  it('rejects too long', () => {
    expect(validateResidentName('A'.repeat(161))).toBe('Nome deve ter no máximo 160 caracteres.');
  });
});

describe('validatePlate', () => {
  it('returns null for valid plate', () => {
    expect(validatePlate('ABC1D23')).toBeNull();
  });

  it('requires value', () => {
    expect(validatePlate('')).toBe('Placa é obrigatória.');
  });

  it('rejects invalid format', () => {
    expect(validatePlate('AB12')).toBe('A placa deve conter exatamente 7 caracteres alfanuméricos.');
  });
});

describe('validateJustification', () => {
  it('returns null for valid justification', () => {
    expect(validateJustification('Teste de cancela', 5, 500)).toBeNull();
  });

  it('requires value', () => {
    expect(validateJustification('', 5, 500)).toBe('Justificativa é obrigatória.');
  });

  it('rejects too short', () => {
    expect(validateJustification('Abc', 5, 500)).toBe('Justificativa deve ter pelo menos 5 caracteres.');
  });

  it('rejects too long', () => {
    expect(validateJustification('A'.repeat(501), 5, 500)).toBe('Justificativa deve ter no máximo 500 caracteres.');
  });
});

describe('validateDateRange', () => {
  it('returns null when no dates', () => {
    expect(validateDateRange('', '')).toBeNull();
  });

  it('returns null for valid range', () => {
    expect(validateDateRange('2026-01-01', '2026-01-31')).toBeNull();
  });

  it('returns error when start after end', () => {
    expect(validateDateRange('2026-01-31', '2026-01-01')).toBe('A data inicial deve ser anterior à data final.');
  });
});

describe('validateReportDateRange', () => {
  it('returns null for valid range', () => {
    expect(validateReportDateRange('2026-01-01', '2026-01-15')).toBeNull();
  });

  it('returns error for reversed dates', () => {
    expect(validateReportDateRange('2026-01-31', '2026-01-01')).toBe('A data inicial deve ser anterior à data final.');
  });

  it('returns error for range > 366 days', () => {
    expect(validateReportDateRange('2026-01-01', '2027-06-01')).toBe('O período não pode exceder 366 dias.');
  });

  it('accepts exactly 366 days', () => {
    expect(validateReportDateRange('2026-01-01', '2027-01-01')).toBeNull();
  });
});

describe('validateParkingTypeRequiresApartment', () => {
  it('returns null for COMMON type without apartment', () => {
    expect(validateParkingTypeRequiresApartment('COMMON', '')).toBeNull();
  });

  it('returns null for ASSIGNED type with apartment', () => {
    expect(validateParkingTypeRequiresApartment('ASSIGNED', '1')).toBeNull();
  });

  it('returns error for ASSIGNED type without apartment', () => {
    expect(validateParkingTypeRequiresApartment('ASSIGNED', '')).toBe('Vagas do tipo Própria exigem um apartamento vinculado.');
  });
});

describe('LIMITS', () => {
  it('has correct values', () => {
    expect(LIMITS.fullName.min).toBe(2);
    expect(LIMITS.fullName.max).toBe(160);
    expect(LIMITS.userName.min).toBe(3);
    expect(LIMITS.userName.max).toBe(80);
    expect(LIMITS.password.min).toBe(12);
    expect(LIMITS.password.max).toBe(200);
    expect(LIMITS.apartmentNumber.max).toBe(20);
    expect(LIMITS.parkingNumber.max).toBe(20);
    expect(LIMITS.residentName.max).toBe(160);
    expect(LIMITS.brand.max).toBe(80);
    expect(LIMITS.model.max).toBe(80);
    expect(LIMITS.color.max).toBe(40);
    expect(LIMITS.justificationGate.min).toBe(5);
    expect(LIMITS.justificationGate.max).toBe(500);
    expect(LIMITS.justificationManual.min).toBe(5);
    expect(LIMITS.justificationManual.max).toBe(1000);
    expect(LIMITS.reportMaxDays).toBe(366);
  });
});
