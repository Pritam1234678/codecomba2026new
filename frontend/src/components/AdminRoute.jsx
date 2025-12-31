import { Navigate } from 'react-router-dom';
import AuthService from '../services/auth.service';

const AdminRoute = ({ children }) => {
  const user = AuthService.getCurrentUser();
  
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  
  if (!user.roles || !user.roles.includes('ROLE_ADMIN')) {
    return <Navigate to="/contests" replace />;
  }
  
  return children;
};

export default AdminRoute;
