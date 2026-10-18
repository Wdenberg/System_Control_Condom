import Badge from './Badge';
import { getRoleLabel } from '@/lib/presentation/common';
import type { RoleCode } from '@/types/api';

export default function RoleBadge({ role }: { role: RoleCode }) {
  return (
    <Badge variant="info" aria-label={`Perfil: ${getRoleLabel(role)}`}>
      {getRoleLabel(role)}
    </Badge>
  );
}
