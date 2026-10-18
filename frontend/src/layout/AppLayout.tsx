import { useState, useCallback } from 'react';
import { NavLink, Outlet } from 'react-router';
import { useAuth } from '@/auth/auth-context';
import { type Permission } from '@/types/api';
import RealtimeStatus from '@/components/RealtimeStatus';
import ThemeToggle from '@/components/ThemeToggle';
import Footer from '@/components/Footer';
import { useTheme } from '@/lib/theme';
import { getRoleLabel } from '@/lib/presentation/common';
import { labels } from '@/lib/presentation/labels';
import {
  LayoutDashboard,
  ArrowRightLeft,
  Building2,
  Car,
  ParkingSquare,
  Map,
  ArrowUpDown,
  Camera,
  UserCog,
  Shield,
  BarChart3,
  ClipboardList,
  Activity,
  ClipboardPlus,
  Menu,
  X,
  type LucideIcon,
} from 'lucide-react';

type NavItem = { label: string; path: string; permission: Permission; Icon: LucideIcon };

const navItems: NavItem[] = [
  { label: labels.nav.dashboard, path: '/dashboard', permission: 'DASHBOARD_READ', Icon: LayoutDashboard },
  { label: labels.nav.portaria, path: '/portaria', permission: 'MOVEMENT_READ', Icon: ArrowRightLeft },
  { label: labels.nav.apartments, path: '/apartments', permission: 'APARTMENT_READ', Icon: Building2 },
  { label: labels.nav.unifiedRegistration, path: '/unified-registration', permission: 'APARTMENT_WRITE', Icon: ClipboardPlus },
  { label: labels.nav.vehicles, path: '/vehicles', permission: 'VEHICLE_READ', Icon: Car },
  { label: labels.nav.parking, path: '/parking', permission: 'PARKING_READ', Icon: ParkingSquare },
  { label: labels.nav.parkingMap, path: '/parking-map', permission: 'PARKING_READ', Icon: Map },
  { label: labels.nav.movements, path: '/movements', permission: 'MOVEMENT_READ', Icon: ArrowUpDown },
  { label: labels.nav.cameras, path: '/cameras', permission: 'CAMERA_READ', Icon: Camera },
  { label: labels.nav.users, path: '/users', permission: 'USER_READ', Icon: UserCog },
  { label: labels.nav.roles, path: '/roles', permission: 'ROLE_READ', Icon: Shield },
  { label: labels.nav.reports, path: '/reports', permission: 'REPORT_READ', Icon: BarChart3 },
  { label: labels.nav.audit, path: '/audit', permission: 'AUDIT_READ', Icon: ClipboardList },
  { label: labels.nav.observability, path: '/observability', permission: 'DASHBOARD_READ', Icon: Activity },
];

export default function AppLayout() {
  const { user, logout, hasPermission } = useAuth();
  const { theme, setTheme } = useTheme();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const visibleItems = navItems.filter((item) => hasPermission(item.permission));

  const closeSidebar = useCallback(() => setSidebarOpen(false), []);

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">{labels.nav.skipToContent}</a>

      {sidebarOpen && <div className="sidebar-overlay" onClick={closeSidebar} />}

      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="brand-block">
          <div className="brand-mark">G</div>
          <div>
            <strong>{labels.nav.appBrand}</strong>
            <span>{labels.nav.appSub}</span>
          </div>
        </div>

        <nav className="main-nav" aria-label={labels.nav.mainNav}>
          {visibleItems.map((item) => (
            <NavLink key={item.path} to={item.path} className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'} onClick={closeSidebar}>
              <span className="nav-icon" aria-hidden="true"><item.Icon className="h-5 w-5" /></span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <ThemeToggle theme={theme} onChange={setTheme} />
          <div className="status-dot mt-3"><span /> {labels.nav.systemLocal}</div>
          <small>API: localhost:8080</small>
        </div>
      </aside>

      <main className="main-content" id="main-content">
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="hamburger-btn"
              onClick={() => setSidebarOpen((o) => !o)}
              aria-label="Menu"
              aria-expanded={sidebarOpen}
            >
              {sidebarOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
            <div>
              <span className="eyebrow">{labels.nav.operation}</span>
              <h1>{labels.nav.appTitle}</h1>
            </div>
          </div>
          <div className="user-menu">
            <RealtimeStatus />
            <div className="avatar">{user?.fullName?.slice(0, 1).toUpperCase() ?? 'U'}</div>
            <div className="user-copy">
              <strong>{user?.fullName}</strong>
              <span>{user?.role ? getRoleLabel(user.role) : '—'}</span>
            </div>
            <button className="ghost-button" onClick={logout} aria-label={labels.common.logout}>{labels.common.logout}</button>
          </div>
        </header>
        <div className="page-wrap"><Outlet /></div>
        <Footer />
      </main>
    </div>
  );
}
