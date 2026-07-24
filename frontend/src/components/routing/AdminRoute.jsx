import { Navigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';

/** Adds the role check on top of the protected route used for all signed-in pages. */
export default function AdminRoute({ children }) {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN';

  return isAdmin ? children : <Navigate replace to="/dashboard" />;
}

