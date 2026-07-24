import apiClient from './client';

export const authApi = {
  register: (registrationData) => apiClient.post('/auth/register', registrationData),
  login: (credentials) => apiClient.post('/auth/login', credentials),
  getCurrentUser: () => apiClient.get('/auth/me'),
};
