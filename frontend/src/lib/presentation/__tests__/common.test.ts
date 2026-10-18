import { describe, expect, it } from 'vitest';
import {
  roleLabels,
  activeLabels,
  permissionLabels,
  getRoleLabel,
  getActiveLabel,
  getPermissionLabel,
} from '@/lib/presentation/common';

describe('common presentation', () => {
  it('roleLabels maps all roles to pt-BR', () => {
    expect(roleLabels.SUPER_ADMIN).toBe('Super Administrador');
    expect(roleLabels.ADMIN).toBe('Administrador');
    expect(roleLabels.OPERATOR).toBe('Operador');
    expect(roleLabels.ATTENDANT).toBe('Atendente');
    expect(roleLabels.VIEWER).toBe('Visualizador');
  });

  it('activeLabels maps boolean strings to pt-BR', () => {
    expect(activeLabels['true']).toBe('Ativo');
    expect(activeLabels['false']).toBe('Inativo');
  });

  it('permissionLabels maps all permissions to pt-BR', () => {
    expect(permissionLabels.DASHBOARD_READ).toBe('Painel');
    expect(permissionLabels.APARTMENT_READ).toBe('Apartamentos');
    expect(permissionLabels.PARKING_READ).toBe('Vagas');
    expect(permissionLabels.VEHICLE_READ).toBe('Veículos');
    expect(permissionLabels.MOVEMENT_READ).toBe('Movimentações');
    expect(permissionLabels.CAMERA_READ).toBe('Câmeras');
    expect(permissionLabels.GATE_READ).toBe('Cancela');
    expect(permissionLabels.USER_READ).toBe('Usuários');
    expect(permissionLabels.ROLE_READ).toBe('Perfis');
    expect(permissionLabels.REPORT_READ).toBe('Relatórios');
    expect(permissionLabels.AUDIT_READ).toBe('Auditoria');
  });

  it('getRoleLabel returns correct label', () => {
    expect(getRoleLabel('ADMIN')).toBe('Administrador');
    expect(getRoleLabel('VIEWER')).toBe('Visualizador');
  });

  it('getRoleLabel falls back to raw value', () => {
    expect(getRoleLabel('GHOST' as any)).toBe('GHOST');
  });

  it('getActiveLabel returns correct label for true', () => {
    expect(getActiveLabel(true)).toBe('Ativo');
  });

  it('getActiveLabel returns correct label for false', () => {
    expect(getActiveLabel(false)).toBe('Inativo');
  });

  it('getPermissionLabel returns correct label', () => {
    expect(getPermissionLabel('USER_READ')).toBe('Usuários');
    expect(getPermissionLabel('PARKING_WRITE')).toBe('Cadastro de vagas');
  });

  it('getPermissionLabel falls back to raw value', () => {
    expect(getPermissionLabel('UNKNOWN_PERM')).toBe('UNKNOWN_PERM');
  });
});
