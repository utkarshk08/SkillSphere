import { NavLink } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';

const mainLinks = [
  ['Dashboard', '/dashboard'],
  ['My profile', '/profile'],
  ['Find students', '/search'],
  ['Skills', '/skills'],
  ['Projects', '/projects'],
  ['Communities', '/communities'],
  ['Roadmaps', '/roadmaps'],
  ['Collaboration requests', '/collaboration-requests'],
  ['Bookmarks', '/bookmarks'],
  ['Notifications', '/notifications'],
  ['Reports', '/reports'],
];

export default function SideNav() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN';

  return (
    <aside className="side-nav" aria-label="Main navigation">
      <nav className="side-nav-links">
        {mainLinks.map(([label, to]) => (
          <NavLink className={({ isActive }) => `side-nav-link${isActive ? ' active' : ''}`} key={to} to={to}>
            {label}
          </NavLink>
        ))}
      </nav>
      {isAdmin && (
        <div className="side-nav-admin">
          <p className="side-nav-label">Administration</p>
          <NavLink className={({ isActive }) => `side-nav-link${isActive ? ' active' : ''}`} to="/admin">
            Admin dashboard
          </NavLink>
        </div>
      )}
    </aside>
  );
}

