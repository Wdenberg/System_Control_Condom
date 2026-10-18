import LoadingState from '@/components/LoadingState';
import { Navigate, Outlet, useLocation } from 'react-router';
import type { ReactNode } from 'react';
import { useAuth } from './auth-context';
import type { Permission } from '@/types/api';
import PlaceholderPage from '@/pages/PlaceholderPage';
import { getPermissionLabel } from '@/lib/presentation/common';
import { labels } from '@/lib/presentation/labels';

export default function ProtectedRoute({ permission, children }: { permission?: Permission; children?: ReactNode }) {
  const { isAuthenticated, isLoading, hasPermission } = useAuth();
  const location = useLocation();

  if (isLoading) return <LoadingState label="Validando sessão…" />;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (permission && !hasPermission(permission)) {
    return <PlaceholderPage title={labels.accessDenied.title} description={labels.accessDenied.description.replace('{label}', getPermissionLabel(permission))} />;
  }

  return children ?? <Outlet />;
}
