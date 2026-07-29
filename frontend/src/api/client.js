import axios from 'axios';
import { API_BASE_URL } from '../config/environment';

export { API_BASE_URL };
export const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '');

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('skillsphere_access_token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // AuthContext owns storage and UI state, while this small browser event lets the
    // shared Axios client report an expired or rejected token without importing React.
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      window.dispatchEvent(new Event('skillsphere:unauthorized'));
    }

    return Promise.reject(error);
  },
);

export default apiClient;
