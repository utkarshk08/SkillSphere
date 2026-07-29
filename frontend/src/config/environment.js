const localApiBaseUrl = 'http://localhost:8080/api';
const localOAuthLoginUrl = 'http://localhost:8080/oauth2/authorization/google';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || localApiBaseUrl;
export const OAUTH_LOGIN_URL = import.meta.env.VITE_OAUTH_LOGIN_URL?.trim() || localOAuthLoginUrl;
