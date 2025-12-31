import api from './api';

const register = (username, email, password, fullName, rollNumber, branch, phoneNumber) => {
  return api.post('/auth/signup', {
    username,
    email,
    password,
    fullName,
    rollNumber,
    branch,
    phoneNumber
  });
};

const login = (username, password) => {
  return api.post('/auth/signin', {
    username,
    password,
  })
  .then((response) => {
    if (response.data.token) {
      localStorage.setItem('user', JSON.stringify(response.data));
      localStorage.setItem('token', response.data.token); // Store token separately
    }
    return response.data;
  });
};

const logout = () => {
  localStorage.removeItem('user');
  localStorage.removeItem('token');
};

const getCurrentUser = () => {
  return JSON.parse(localStorage.getItem('user'));
};

const AuthService = {
  register,
  login,
  logout,
  getCurrentUser,
};

export default AuthService;
