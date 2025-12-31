import { Navigate, useLocation } from 'react-router-dom';
import AuthService from '../services/auth.service';

const UserRoute = ({ children }) => {
  const user = AuthService.getCurrentUser();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Allow both USER and ADMIN to access profile edit page
  if (location.pathname === '/profile/edit') {
    return children;
  }

  // Redirect admin to admin dashboard for other user routes
  if (user.roles && user.roles.includes('ROLE_ADMIN')) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  return children;
};

export default UserRoute;
