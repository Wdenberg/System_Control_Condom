import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import { useAuth } from '@/auth/auth-context';
import type { AdminUser, RoleCode } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import Modal from '@/components/Modal';
import SearchInput from '@/components/ui/SearchInput';
import FilterBar from '@/components/ui/FilterBar';
import RoleBadge from '@/components/ui/RoleBadge';
import ActiveStatusBadge from '@/components/ui/ActiveStatusBadge';
import { roleLabels } from '@/lib/presentation/common';
import { labels } from '@/lib/presentation/labels';
import { formatProperName, validateFullName, validateUsername, validatePassword, LIMITS } from '@/lib/validation';
import type { FieldErrors } from '@/lib/validation';

const roles: RoleCode[] = ['SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'ATTENDANT', 'VIEWER'];
const emptyCreate = { username: '', password: '', fullName: '', role: 'ATTENDANT' as RoleCode, active: true };

export default function UsersPage() {
  const { user: currentUser, hasPermission } = useAuth();
  const canWrite = hasPermission('USER_WRITE');
  const isSuperAdmin = currentUser?.role === 'SUPER_ADMIN';
  const [items, setItems] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [modal, setModal] = useState<'form' | 'password' | null>(null);
  const [editing, setEditing] = useState<AdminUser | null>(null);
  const [form, setForm] = useState(emptyCreate);
  const [newPassword, setNewPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const usernameInputRef = useRef<HTMLInputElement>(null);
  const fullNameInputRef = useRef<HTMLInputElement>(null);
  const passwordInputRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { setItems(await api.users.list()); setError(''); }
    catch (err) { setError(getApiErrorMessage(err)); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return items.filter((item) => {
      const matchesQuery = !q || item.username.includes(q) || item.fullName.toLowerCase().includes(q);
      const matchesRole = roleFilter === 'ALL' || item.role === roleFilter;
      const matchesStatus = statusFilter === 'ALL' || (statusFilter === 'ACTIVE' ? item.active : !item.active);
      return matchesQuery && matchesRole && matchesStatus;
    });
  }, [items, query, roleFilter, statusFilter]);

  function openNew() {
    setEditing(null); setForm(emptyCreate); setError(''); setFieldErrors({}); setModal('form');
    setTimeout(() => usernameInputRef.current?.focus(), 50);
  }
  function openEdit(item: AdminUser) {
    setEditing(item); setForm({ username: item.username, password: '', fullName: item.fullName, role: item.role, active: item.active }); setError(''); setFieldErrors({}); setModal('form');
    setTimeout(() => fullNameInputRef.current?.focus(), 50);
  }
  function openPassword(item: AdminUser) { setEditing(item); setNewPassword(''); setError(''); setFieldErrors({}); setModal('password'); setTimeout(() => passwordInputRef.current?.focus(), 50); }

  function handleFullNameBlur() {
    const formatted = formatProperName(form.fullName);
    if (formatted !== form.fullName) setForm((prev) => ({ ...prev, fullName: formatted }));
  }

  function validateUserForm(): boolean {
    const errors: FieldErrors = {};
    if (!editing) {
      const usernameErr = validateUsername(form.username);
      if (usernameErr) errors.username = usernameErr;
      const passwordErr = validatePassword(form.password, true);
      if (passwordErr) errors.password = passwordErr;
    }
    const nameErr = validateFullName(form.fullName);
    if (nameErr) errors.fullName = nameErr;
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      if (errors.username) usernameInputRef.current?.focus();
      else if (errors.fullName) fullNameInputRef.current?.focus();
      else if (errors.password) passwordInputRef.current?.focus();
      return false;
    }
    return true;
  }

  async function save() {
    if (!validateUserForm()) return;
    const formattedName = formatProperName(form.fullName);
    setSaving(true);
    try {
      const result = editing
        ? await api.users.update(editing.id, { fullName: formattedName, role: form.role, active: form.active })
        : await api.users.create({ username: form.username.trim(), password: form.password, fullName: formattedName, role: form.role, active: form.active });
      setItems((prev) => editing ? prev.map((item) => item.id === result.id ? result : item) : [...prev, result].sort((a, b) => a.username.localeCompare(b.username)));
      setModal(null);
    }     catch (err) { setError(getApiErrorMessage(err)); }
    finally { setSaving(false); }
  }

  function validatePasswordForm(): boolean {
    const errors: FieldErrors = {};
    const pwErr = validatePassword(newPassword, true);
    if (pwErr) errors.newPassword = pwErr;
    setFieldErrors(errors);
    if (pwErr) { passwordInputRef.current?.focus(); return false; }
    return true;
  }

  async function resetPassword() {
    if (!editing || !validatePasswordForm()) return;
    setSaving(true); setError('');
    try { const result = await api.users.resetPassword(editing.id, newPassword); setItems((prev) => prev.map((item) => item.id === result.id ? result : item)); setModal(null); }
    catch (err) { setError(getApiErrorMessage(err)); }
    finally { setSaving(false); }
  }

  return (
    <div className="page-content">
      <PageHeader
        title={labels.users.title}
        description={labels.users.description}
        action={canWrite ? <button className="primary-button" onClick={openNew}>{labels.users.newButton}</button> : undefined}
      />

      <FilterBar count={filtered.length} countLabel={`de ${items.length}`}>
        <SearchInput value={query} onChange={setQuery} placeholder={labels.users.searchPlaceholder} label="Buscar usuário" />
        <select aria-label={labels.users.roleFilterLabel} value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
          <option value="ALL">{labels.users.allRoles}</option>
          {roles.filter((role) => role !== 'SUPER_ADMIN' || isSuperAdmin).map((role) => (
            <option key={role} value={role}>{roleLabels[role]}</option>
          ))}
        </select>
        <select aria-label={labels.users.statusFilterLabel} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="ALL">{labels.users.allStatus}</option>
          <option value="ACTIVE">{labels.common.activeLabel}</option>
          <option value="INACTIVE">{labels.common.inactiveLabel}</option>
        </select>
      </FilterBar>

      {error && <div className="banner warning" role="alert">{error}</div>}

      <section className="panel">
        <div className="panel-header"><div><h3>{labels.users.listTitle}</h3><span>{labels.users.listHint}</span></div></div>
        {loading ? <LoadingState /> : filtered.length === 0 ? (
          <EmptyState title={labels.users.emptyTitle} description={labels.users.emptyDesc} />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{labels.users.thUser}</th>
                  <th>{labels.users.thRole}</th>
                  <th>{labels.users.thStatus}</th>
                  <th>{labels.users.thPermissions}</th>
                  <th>{labels.users.thActions}</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.username}</strong>
                      <small>{item.fullName}{item.id === currentUser?.id ? ` · ${labels.users.youLabel}` : ''}</small>
                    </td>
                    <td><RoleBadge role={item.role} /></td>
                    <td><ActiveStatusBadge active={item.active} /></td>
                    <td><span className="permission-count">{item.permissions.length} {labels.users.permissionsCount}</span></td>
                    <td>
                      <div className="row-actions">
                        {canWrite && (item.role !== 'SUPER_ADMIN' || isSuperAdmin) && (
                          <>
                            <button className="secondary-button small" onClick={() => openEdit(item)}>{labels.users.editButton}</button>
                            <button className="secondary-button small" onClick={() => openPassword(item)}>{labels.users.passwordButton}</button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modal === 'form' && (
        <Modal title={editing ? labels.users.editModalTitle : labels.users.newModalTitle} description={editing ? labels.users.editModalDesc : labels.users.newModalDesc} onClose={() => setModal(null)}>
          <div className="form-grid">
            <label>
              <span>{labels.users.usernameLabel}</span>
              <input
                ref={usernameInputRef}
                value={form.username}
                disabled={Boolean(editing)}
                onChange={(e) => { setForm({ ...form, username: e.target.value }); if (fieldErrors.username) setFieldErrors((p) => { const n = { ...p }; delete n.username; return n; }); }}
                autoFocus={!editing}
                maxLength={LIMITS.userName.max}
                aria-invalid={Boolean(fieldErrors.username)}
                aria-describedby={fieldErrors.username ? 'username-error' : undefined}
              />
              {fieldErrors.username && <small className="field-error" id="username-error" role="alert">{fieldErrors.username}</small>}
            </label>
            <label>
              <span>{labels.users.fullNameLabel}</span>
              <input
                ref={fullNameInputRef}
                autoFocus={Boolean(editing)}
                value={form.fullName}
                onChange={(e) => { setForm({ ...form, fullName: e.target.value }); if (fieldErrors.fullName) setFieldErrors((p) => { const n = { ...p }; delete n.fullName; return n; }); }}
                onBlur={handleFullNameBlur}
                maxLength={LIMITS.fullName.max}
                aria-invalid={Boolean(fieldErrors.fullName)}
                aria-describedby={fieldErrors.fullName ? 'fullname-error' : undefined}
              />
              {fieldErrors.fullName && <small className="field-error" id="fullname-error" role="alert">{fieldErrors.fullName}</small>}
            </label>
            {!editing && (
              <label>
                <span>{labels.users.initialPasswordLabel}</span>
                <input
                  ref={passwordInputRef}
                  type="password"
                  value={form.password}
                  onChange={(e) => { setForm({ ...form, password: e.target.value }); if (fieldErrors.password) setFieldErrors((p) => { const n = { ...p }; delete n.password; return n; }); }}
                  maxLength={LIMITS.password.max}
                  aria-invalid={Boolean(fieldErrors.password)}
                  aria-describedby={fieldErrors.password ? 'password-error' : undefined}
                />
                {fieldErrors.password && <small className="field-error" id="password-error" role="alert">{fieldErrors.password}</small>}
              </label>
            )}
            <label>
              <span>{labels.users.roleLabel}</span>
              <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as RoleCode })} aria-label="Perfil">
                {roles.filter((role) => role !== 'SUPER_ADMIN' || isSuperAdmin).map((role) => (
                  <option key={role} value={role}>{roleLabels[role]}</option>
                ))}
              </select>
            </label>
            <label className="checkbox-field">
              <input type="checkbox" checked={form.active} disabled={Boolean(editing && editing.id === currentUser?.id)} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
              <span>{labels.users.activeLabel}</span>
            </label>
          </div>
          {editing?.id === currentUser?.id && <div className="banner warning" style={{ marginTop: 14 }}>{labels.users.selfEditWarning}</div>}
          {error && <div className="form-error" role="alert">{error}</div>}
          <div className="modal-actions">
            <button className="secondary-button" onClick={() => setModal(null)}>{labels.common.cancel}</button>
            <button className="primary-button" onClick={() => void save()} disabled={saving}>{saving ? labels.common.saving : labels.common.save}</button>
          </div>
        </Modal>
      )}

      {modal === 'password' && editing && (
        <Modal title={`${labels.users.passwordModalTitle} ${editing.username}`} description={labels.users.passwordModalDesc} onClose={() => setModal(null)} width="small">
          <div className="form-grid">
            <label>
              <span>{labels.users.newPasswordLabel}</span>
              <input
                ref={passwordInputRef}
                type="password"
                value={newPassword}
                onChange={(e) => { setNewPassword(e.target.value); if (fieldErrors.newPassword) setFieldErrors((p) => { const n = { ...p }; delete n.newPassword; return n; }); }}
                maxLength={LIMITS.password.max}
                aria-invalid={Boolean(fieldErrors.newPassword)}
                aria-describedby={fieldErrors.newPassword ? 'new-password-error' : undefined}
              />
              {fieldErrors.newPassword && <small className="field-error" id="new-password-error" role="alert">{fieldErrors.newPassword}</small>}
            </label>
          </div>
          {error && <div className="form-error" role="alert">{error}</div>}
          <div className="modal-actions">
            <button className="secondary-button" onClick={() => setModal(null)}>{labels.common.cancel}</button>
            <button className="primary-button" onClick={() => void resetPassword()} disabled={saving}>{saving ? labels.common.saving : labels.users.changePasswordButton}</button>
          </div>
        </Modal>
      )}
    </div>
  );
}
