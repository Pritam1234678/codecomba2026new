import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../services/auth.service';

const useAccountStatusCheck = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const checkAccountStatus = async () => {
      const user = AuthService.getCurrentUser();
      
      // Only check if user is logged in
      if (!user) return;

      try {
        // Make a lightweight API call to check status
        const response = await fetch('/api/user/profile', {
          headers: {
            'Authorization': `Bearer ${user.token}`
          }
        });

        // If 403 or account disabled, the interceptor will handle it
        if (!response.ok && response.status === 403) {
          // Interceptor will show modal and logout
          return;
        }
      } catch (error) {
        // Interceptor handles the error
        console.log('Account status check:', error.message);
      }
    };

    checkAccountStatus();
  }, [navigate]);
};

export default useAccountStatusCheck;
