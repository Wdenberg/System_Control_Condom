import { useEffect, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import type { RoleDefinition } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import ActiveStatusBadge from '@/components/ui/ActiveStatusBadge';
import { getRoleLabel, getPermissionLabel } from '@/lib/presentation/common';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';

export default function RolesPage() {
  const [roles, setRoles] = useState<RoleDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<RoleDefinition | null>(null);

  useEffect(() => { void load(); }, []);
  async function load() {
    setLoading(true);
    try { const result = await api.roles.list(); setRoles(result); setSelected(result[0] ?? null); setError(''); }
    catch (err) { setError(getApiErrorMessage(err, messages.errors.loadRoles)); }
    finally { setLoading(false); }
  }

  return (
    <div className="page-content">
      <PageHeader title="Perfis e permissões" description="Catálogo de autorização da aplicação. As permissões são derivadas da matriz oficial do servidor." />

      {error && <div className="banner warning" role="alert">{error}</div>}

      {loading ? <LoadingState label={labels.common.loading} /> : roles.length === 0 ? (
        <EmptyState title={labels.roles.emptyTitle} description={labels.roles.emptyDesc} />
      ) : (
        <div className="role-layout">
          <section className="panel">
            <div className="panel-header"><div><h3>Perfis</h3><span>Catálogo fixo do sistema</span></div></div>
            <div className="role-list">
              {roles.map((role) => (
                <button key={role.code} className={`role-card ${selected?.code === role.code ? 'selected' : ''}`} aria-selected={selected?.code === role.code} onClick={() => setSelected(role)}>
                  <div>
                    <strong>{getRoleLabel(role.code)}</strong>
                    <span>{role.permissions.length} permissões</span>
                  </div>
                  <ActiveStatusBadge active={role.active} />
                </button>
              ))}
            </div>
          </section>

          <section className="panel">
            <div className="panel-header"><div><h3>{selected ? getRoleLabel(selected.code) : '—'}</h3><span>Permissões efetivas calculadas no servidor</span></div></div>
            {selected && (
              <div className="permission-grid">
                {selected.permissions.map((permission) => (
                  <span key={permission} className="permission-chip">{getPermissionLabel(permission)}</span>
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
