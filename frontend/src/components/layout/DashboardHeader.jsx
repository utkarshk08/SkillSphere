import { Link } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';

export default function DashboardHeader({ publicView = false }) {
  const { user, logout } = useAuth();
  const displayName = user?.firstName || user?.username || user?.email || 'Student';

  return (
    <header className="dashboard-header">
      <div className="header-inner">
        <Link className="brand" to={publicView ? '/' : '/dashboard'}>
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>SkillSphere</span>
        </Link>
        <div className="header-account">
          {publicView ? (
            <Link className="button button-secondary button-small" to="/login">Sign in</Link>
          ) : (
            <>
              <span className="account-name">{displayName}</span>
              <button className="button button-secondary button-small" type="button" onClick={logout}>
                Sign out
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
