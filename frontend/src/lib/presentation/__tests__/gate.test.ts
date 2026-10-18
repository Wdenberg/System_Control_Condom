import { describe, expect, it } from 'vitest';
import {
  gateOperationStatusLabels,
  gateDriverStateLabels,
  getGateOperationStatusLabel,
  getGateDriverStateLabel,
  getGateOperationStatusClass,
} from '@/lib/presentation/gate';

describe('gate presentation', () => {
  it('gateOperationStatusLabels maps all statuses to pt-BR', () => {
    expect(gateOperationStatusLabels.PENDING).toBe('Pendente');
    expect(gateOperationStatusLabels.PROCESSING).toBe('Em processamento');
    expect(gateOperationStatusLabels.UNKNOWN).toBe('Resultado incerto — reconciliar');
    expect(gateOperationStatusLabels.SENT).toBe('Enviado');
    expect(gateOperationStatusLabels.EXHAUSTED).toBe('Esgotado');
    expect(gateOperationStatusLabels.DISABLED).toBe('Desabilitado');
  });

  it('gateDriverStateLabels maps all states to pt-BR', () => {
    expect(gateDriverStateLabels.READY).toBe('Pronto');
    expect(gateDriverStateLabels.CONFIGURED).toBe('Configurado');
    expect(gateDriverStateLabels.NOT_CONFIGURED).toBe('Não configurado');
    expect(gateDriverStateLabels.DISABLED).toBe('Desabilitado');
    expect(gateDriverStateLabels.ERROR).toBe('Erro');
  });

  it('getGateOperationStatusLabel returns correct label', () => {
    expect(getGateOperationStatusLabel('PENDING')).toBe('Pendente');
    expect(getGateOperationStatusLabel('SENT')).toBe('Enviado');
    expect(getGateOperationStatusLabel('UNKNOWN')).toBe('Resultado incerto — reconciliar');
  });

  it('getGateOperationStatusLabel falls back to raw value', () => {
    expect(getGateOperationStatusLabel('FUTURE_STATUS')).toBe('FUTURE_STATUS');
  });

  it('getGateDriverStateLabel returns correct label', () => {
    expect(getGateDriverStateLabel('READY')).toBe('Pronto');
    expect(getGateDriverStateLabel('ERROR')).toBe('Erro');
  });

  it('getGateDriverStateLabel falls back to raw value', () => {
    expect(getGateDriverStateLabel('X')).toBe('X');
  });

  it('getGateOperationStatusClass returns class for known statuses', () => {
    expect(getGateOperationStatusClass('PENDING')).toBe('pending');
    expect(getGateOperationStatusClass('PROCESSING')).toBe('pending');
    expect(getGateOperationStatusClass('UNKNOWN')).toBe('denied');
    expect(getGateOperationStatusClass('SENT')).toBe('authorized');
    expect(getGateOperationStatusClass('EXHAUSTED')).toBe('denied');
    expect(getGateOperationStatusClass('DISABLED')).toBe('overridden');
  });

  it('getGateOperationStatusClass falls back to pending', () => {
    expect(getGateOperationStatusClass('FUTURE_STATUS')).toBe('pending');
  });
});
