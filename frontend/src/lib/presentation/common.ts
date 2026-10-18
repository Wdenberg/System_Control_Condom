import type { RoleCode } from '@/types/api';

export function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value));
}

export function localIso(date: string, end = false): string | undefined {
  if (!date) return undefined;
  const suffix = end ? 'T23:59:59.999' : 'T00:00:00';
  return new Date(`${date}${suffix}`).toISOString();
}

export function today(): string {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export const roleLabels: Record<RoleCode, string> = {
  SUPER_ADMIN: 'Super Administrador',
  ADMIN: 'Administrador',
  OPERATOR: 'Operador',
  ATTENDANT: 'Atendente',
  VIEWER: 'Visualizador',
};

export const activeLabels: Record<string, string> = {
  true: 'Ativo',
  false: 'Inativo',
};

export const permissionLabels: Record<string, string> = {
  DASHBOARD_READ: 'Painel',
  APARTMENT_READ: 'Apartamentos',
  APARTMENT_WRITE: 'Cadastro de apartamentos',
  PARKING_READ: 'Vagas',
  PARKING_WRITE: 'Cadastro de vagas',
  VEHICLE_READ: 'Veículos',
  VEHICLE_WRITE: 'Cadastro de veículos',
  MOVEMENT_READ: 'Movimentações',
  MOVEMENT_MANUAL_WRITE: 'Movimentação manual',
  MOVEMENT_OVERRIDE: 'Override de movimentação',
  CAMERA_READ: 'Câmeras',
  CAMERA_WRITE: 'Cadastro de câmeras',
  CAMERA_TEST: 'Teste de câmera',
  GATE_READ: 'Cancela',
  GATE_WRITE: 'Controle de cancela',
  GATE_TEST: 'Teste de cancela',
  USER_READ: 'Usuários',
  USER_WRITE: 'Cadastro de usuários',
  ROLE_READ: 'Perfis',
  ROLE_WRITE: 'Cadastro de perfis',
  REPORT_READ: 'Relatórios',
  AUDIT_READ: 'Auditoria',
};

export function getRoleLabel(role: RoleCode): string {
  return roleLabels[role] ?? role;
}

export function getActiveLabel(active: boolean): string {
  return activeLabels[String(active)] ?? (active ? 'Ativo' : 'Inativo');
}

export function getPermissionLabel(permission: string): string {
  return permissionLabels[permission] ?? permission;
}
