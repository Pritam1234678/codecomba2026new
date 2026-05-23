import api from './api';
import cache from './cache';

const register = (username, email, password, fullName) => {
  return api.post('/auth/signup', {
    username,
    email,
    password,
    fullName
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
      localStorage.setItem('token', response.data.token);
    }
    return response.data;
  });
};

const logout = () => {
  localStorage.removeItem('user');
  localStorage.removeItem('token');
  cache.clear(); // clear all cached data on logout
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
