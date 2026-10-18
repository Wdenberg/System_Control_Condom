import { lazy, Suspense, useEffect, type ReactNode } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router';
import { useAuth } from '@/auth/auth-context';
import { labels } from '@/lib/presentation/labels';
import PlaceholderPage from '@/pages/PlaceholderPage';
const LoginPage = lazy(() => import('@/pages/LoginPage'));
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const PortariaPage = lazy(() => import('@/pages/PortariaPage'));
const ApartmentsPage = lazy(() => import('@/pages/ApartmentsPage'));
const VehiclesPage = lazy(() => import('@/pages/VehiclesPage'));
const ParkingPage = lazy(() => import('@/pages/ParkingPage'));
const ParkingMapPage = lazy(() => import('@/pages/ParkingMapPage'));
const MovementsPage = lazy(() => import('@/pages/MovementsPage'));
const CamerasPage = lazy(() => import('@/pages/CamerasPage'));
const UsersPage = lazy(() => import('@/pages/UsersPage'));
const RolesPage = lazy(() => import('@/pages/RolesPage'));
const UnifiedRegistrationPage = lazy(() => import('@/pages/UnifiedRegistrationPage'));
const ReportsPage = lazy(() => import('@/pages/ReportsPage'));
const AuditPage = lazy(() => import('@/pages/AuditPage'));
const ObservabilityPage = lazy(() => import('@/pages/ObservabilityPage'));
import AppLayout from '@/layout/AppLayout';
import ProtectedRoute from '@/auth/ProtectedRoute';
import type { Permission } from '@/types/api';

function UnauthorizedListener() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const handler = () => {
      logout();
      navigate('/login', { replace: true });
    };
    window.addEventListener('auth:unauthorized', handler);
    return () => window.removeEventListener('auth:unauthorized', handler);
  }, [logout, navigate]);

  return null;
}

function RootRedirect() {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />;
}

function Protected({ permission, children }: { permission?: Permission; children: ReactNode }) {
  return <ProtectedRoute permission={permission}>{children}</ProtectedRoute>;
}

function NotFound() {
  const location = useLocation();
  return <PlaceholderPage title={labels.notFound.title} description={labels.notFound.description.replace('{pathname}', location.pathname)} />;
}

export default function App() {
  return (
    <>
      <UnauthorizedListener />
      <Suspense fallback={<div role="status">Carregando página…</div>}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<RootRedirect />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="dashboard" element={<Protected permission="DASHBOARD_READ"><DashboardPage /></Protected>} />
            <Route path="portaria" element={<Protected permission="MOVEMENT_READ"><PortariaPage /></Protected>} />
            <Route path="apartments" element={<Protected permission="APARTMENT_READ"><ApartmentsPage /></Protected>} />
            <Route path="vehicles" element={<Protected permission="VEHICLE_READ"><VehiclesPage /></Protected>} />
            <Route path="parking" element={<Protected permission="PARKING_READ"><ParkingPage /></Protected>} />
            <Route path="parking-map" element={<Protected permission="PARKING_READ"><ParkingMapPage /></Protected>} />
            <Route path="movements" element={<Protected permission="MOVEMENT_READ"><MovementsPage /></Protected>} />
            <Route path="cameras" element={<Protected permission="CAMERA_READ"><CamerasPage /></Protected>} />
            <Route path="users" element={<Protected permission="USER_READ"><UsersPage /></Protected>} />
            <Route path="roles" element={<Protected permission="ROLE_READ"><RolesPage /></Protected>} />
            <Route path="reports" element={<Protected permission="REPORT_READ"><ReportsPage /></Protected>} />
            <Route path="audit" element={<Protected permission="AUDIT_READ"><AuditPage /></Protected>} />
            <Route path="observability" element={<Protected permission="DASHBOARD_READ"><ObservabilityPage /></Protected>} />
            <Route path="unified-registration" element={<Protected permission="APARTMENT_WRITE"><UnifiedRegistrationPage /></Protected>} />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Route>
      </Routes>
      </Suspense>
    </>
  );
}
