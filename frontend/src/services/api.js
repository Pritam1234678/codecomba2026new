import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL ?? '/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (user && user.token) {
      config.headers['Authorization'] = 'Bearer ' + user.token;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to detect disabled accounts
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle 401 Unauthorized - token invalid/expired
    if (error.response?.status === 401) {
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }
    
    // Check if account is disabled (403)
    const isDisabled = error.response?.status === 403 && (
        error.response?.data?.message?.includes('ACCOUNT_DISABLED') ||
        error.response?.data?.message?.includes('disabled') ||
        (typeof error.response?.data === 'object' && 
         error.response?.data?.message?.includes('ACCOUNT_DISABLED'))
    );
    
    if (isDisabled) {
      
      // Logout user
      localStorage.removeItem('user');
      
      // Create and show blocking modal
      const existingModal = document.getElementById('account-blocked-modal');
      if (!existingModal) {
        const modalDiv = document.createElement('div');
        modalDiv.id = 'account-blocked-modal';
        modalDiv.innerHTML = `
          <div style="position: fixed; inset: 0; z-index: 9999; display: flex; align-items: center; justify-content: center; padding: 1rem; background: rgba(0, 0, 0, 0.8); backdrop-filter: blur(8px);">
            <div style="background: linear-gradient(to bottom right, rgba(127, 29, 29, 0.4), rgba(0, 0, 0, 0.6)); backdrop-filter: blur(24px); border: 2px solid rgba(239, 68, 68, 0.5); border-radius: 1.5rem; padding: 3rem; max-width: 28rem; width: 100%; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);">
              <div style="display: flex; align-items: center; justify-content: center; width: 5rem; height: 5rem; background: rgba(239, 68, 68, 0.2); border-radius: 9999px; margin: 0 auto 1.5rem; animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;">
                <svg style="width: 3rem; height: 3rem; color: rgb(248, 113, 113);" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                </svg>
              </div>
              <h2 style="font-size: 2rem; font-weight: bold; color: rgb(248, 113, 113); text-align: center; margin-bottom: 1rem;">Account Blocked</h2>
              <p style="color: rgb(229, 231, 235); text-align: center; margin-bottom: 0.75rem; font-size: 1.125rem;">Your account has been disabled by an administrator.</p>
              <p style="color: rgb(156, 163, 175); text-align: center; margin-bottom: 2rem;">If you believe this is a mistake, please contact our support team.</p>
              <div style="background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 0.75rem; padding: 1rem; margin-bottom: 1.5rem;">
                <p style="font-size: 0.875rem; color: rgb(156, 163, 175); text-align: center; margin-bottom: 0.5rem;">Contact Support:</p>
                <a href="mailto:support@codecombat.live" style="color: rgb(74, 222, 128); font-weight: 500; font-size: 1.125rem; display: block; text-align: center; text-decoration: none;">support@codecombat.live</a>
              </div>
              <p style="font-size: 0.75rem; color: rgb(107, 114, 128); text-align: center;">Redirecting to login page...</p>
            </div>
          </div>
        `;
        document.body.appendChild(modalDiv);
        
        // Redirect to login after 5 seconds
        setTimeout(() => {
          window.location.href = '/login';
        }, 5000);
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
