import { describe, expect, it } from 'vitest';
import {
  movementDirectionLabels,
  movementStatusLabels,
  accessDecisionLabels,
  getMovementDirectionLabel,
  getMovementStatusLabel,
  getAccessDecisionLabel,
  getMovementStatusClass,
  getDirectionClass,
} from '@/lib/presentation/movement';

describe('movement presentation', () => {
  it('movementDirectionLabels maps ENTRY and EXIT to pt-BR', () => {
    expect(movementDirectionLabels.ENTRY).toBe('Entrada');
    expect(movementDirectionLabels.EXIT).toBe('Saída');
  });

  it('movementStatusLabels maps all statuses to pt-BR', () => {
    expect(movementStatusLabels.AUTHORIZED).toBe('Autorizado');
    expect(movementStatusLabels.DENIED).toBe('Negado');
    expect(movementStatusLabels.OVERRIDDEN).toBe('Sobrescrito');
  });

  it('accessDecisionLabels maps all decisions to pt-BR', () => {
    expect(accessDecisionLabels.AUTHORIZED).toBe('Autorizado');
    expect(accessDecisionLabels.DENIED).toBe('Negado');
    expect(accessDecisionLabels.UNKNOWN_VEHICLE).toBe('Veículo não cadastrado');
    expect(accessDecisionLabels.DUPLICATE_ENTRY).toBe('Entrada duplicada');
    expect(accessDecisionLabels.EXIT_WITHOUT_ENTRY).toBe('Saída sem entrada registrada');
    expect(accessDecisionLabels.LOW_CONFIDENCE).toBe('Baixa confiança na leitura');
    expect(accessDecisionLabels.VEHICLE_INACTIVE).toBe('Veículo inativo');
    expect(accessDecisionLabels.RESIDENT_INACTIVE).toBe('Morador ou apartamento inativo');
    expect(accessDecisionLabels.NO_PARKING_ASSIGNED).toBe('Sem vaga atribuída');
    expect(accessDecisionLabels.PARKING_SPACE_INACTIVE).toBe('Vaga inativa');
    expect(accessDecisionLabels.PARKING_SPACE_BLOCKED).toBe('Vaga bloqueada');
    expect(accessDecisionLabels.PARKING_SPACE_OCCUPIED).toBe('Vaga ocupada');
    expect(accessDecisionLabels.INVALID_DIRECTION).toBe('Direção inválida');
    expect(accessDecisionLabels.MANUAL_AUTHORIZED).toBe('Autorização manual');
    expect(accessDecisionLabels.OVERRIDE_AUTHORIZED).toBe('Override autorizado');
  });

  it('getMovementDirectionLabel returns correct label', () => {
    expect(getMovementDirectionLabel('ENTRY')).toBe('Entrada');
    expect(getMovementDirectionLabel('EXIT')).toBe('Saída');
  });

  it('getMovementDirectionLabel falls back to raw value', () => {
    expect(getMovementDirectionLabel('UP' as any)).toBe('UP');
  });

  it('getMovementStatusLabel returns correct label', () => {
    expect(getMovementStatusLabel('AUTHORIZED')).toBe('Autorizado');
    expect(getMovementStatusLabel('DENIED')).toBe('Negado');
  });

  it('getMovementStatusLabel replaces underscores for unknown statuses', () => {
    expect(getMovementStatusLabel('SOME_OTHER')).toBe('SOME OTHER');
  });

  it('getAccessDecisionLabel returns correct label', () => {
    expect(getAccessDecisionLabel('AUTHORIZED')).toBe('Autorizado');
    expect(getAccessDecisionLabel('UNKNOWN_VEHICLE')).toBe('Veículo não cadastrado');
  });

  it('getAccessDecisionLabel replaces underscores for unknown decisions', () => {
    expect(getAccessDecisionLabel('NEW_DECISION')).toBe('NEW DECISION');
  });

  it('getMovementStatusClass returns class for known statuses', () => {
    expect(getMovementStatusClass('AUTHORIZED')).toBe('authorized');
    expect(getMovementStatusClass('DENIED')).toBe('denied');
    expect(getMovementStatusClass('OVERRIDDEN')).toBe('overridden');
  });

  it('getMovementStatusClass falls back to pending', () => {
    expect(getMovementStatusClass('UNKNOWN')).toBe('pending');
  });

  it('getDirectionClass returns entry or exit', () => {
    expect(getDirectionClass('ENTRY')).toBe('entry');
    expect(getDirectionClass('EXIT')).toBe('exit');
  });
});
