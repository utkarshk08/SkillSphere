import DashboardHeader from './DashboardHeader';
import SideNav from './SideNav';

/** Consistent dashboard layout, with a simplified public variant for shared profiles. */
export default function AppShell({ children, publicView = false }) {
  return (
    <div className="app-shell">
      <DashboardHeader publicView={publicView} />
      <div className={`app-body${publicView ? ' app-body-public' : ''}`}>
        {!publicView && <SideNav />}
        <main className="app-main">{children}</main>
      </div>
    </div>
  );
}
