import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/authApi';
import {
  clearSession,
  extractSession,
  getStoredToken,
  getStoredUser,
  getUserFromToken,
  saveSession,
} from '../utils/auth';

export const AuthContext = createContext(null);

function getErrorMessage(error, fallbackMessage) {
  return error.response?.data?.message || error.response?.data?.error || fallbackMessage;
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(getStoredToken);
  const [user, setUser] = useState(getStoredUser);
  const [isLoading, setIsLoading] = useState(true);

  const logout = useCallback(() => {
    clearSession();
    setToken(null);
    setUser(null);
  }, []);

  const setSession = useCallback((session) => {
    saveSession(session.token, session.user);
    setToken(session.token);
    setUser(session.user);
  }, []);

  const refreshCurrentUser = useCallback(async () => {
    if (!getStoredToken()) {
      return null;
    }

    try {
      const response = await authApi.getCurrentUser();
      const currentUser = response.data;
      const nextUser = {
        ...getUserFromToken(getStoredToken()),
        ...(currentUser?.user || currentUser || {}),
      };

      saveSession(getStoredToken(), nextUser);
      setUser(nextUser);
      return nextUser;
    } catch (error) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        logout();
      }
      throw error;
    }
  }, [logout]);

  useEffect(() => {
    let isActive = true;

    async function restoreSession() {
      if (!getStoredToken()) {
        if (isActive) {
          setIsLoading(false);
        }
        return;
      }

      try {
        await refreshCurrentUser();
      } catch {
        // A non-auth network error should not remove a valid locally stored session.
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    restoreSession();

    return () => {
      isActive = false;
    };
  }, [refreshCurrentUser]);

  // The Axios client emits this event when Spring rejects an expired or invalid JWT.
  // Keeping the logout here means every screen returns to a truthful signed-out state.
  useEffect(() => {
    const handleUnauthorized = () => logout();
    window.addEventListener('skillsphere:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('skillsphere:unauthorized', handleUnauthorized);
  }, [logout]);

  const login = useCallback(async (credentials) => {
    try {
      const response = await authApi.login(credentials);
      const session = extractSession(response.data);

      if (!session) {
        throw new Error('The server response did not include an access token.');
      }

      setSession(session);
      return session.user;
    } catch (error) {
      throw new Error(getErrorMessage(error, 'Unable to sign in. Please try again.'));
    }
  }, [setSession]);

  const register = useCallback(async (registrationData) => {
    try {
      const response = await authApi.register(registrationData);
      const session = extractSession(response.data);

      if (session) {
        setSession(session);
        return { user: session.user, signedIn: true };
      }

      return { signedIn: false };
    } catch (error) {
      throw new Error(getErrorMessage(error, 'Unable to create your account. Please try again.'));
    }
  }, [setSession]);

  const completeOAuthLogin = useCallback(async (oauthToken) => {
    const session = extractSession({ accessToken: oauthToken });

    if (!session) {
      throw new Error('Google sign-in did not return an access token.');
    }

    setSession(session);
    try {
      // JWT claims intentionally contain only small identity details. Fetching /auth/me
      // supplies the database user id needed by ownership and collaboration screens.
      return await refreshCurrentUser();
    } catch (error) {
      logout();
      throw new Error(getErrorMessage(error, 'Google sign-in could not load your account.'));
    }
  }, [logout, refreshCurrentUser, setSession]);

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token),
      isLoading,
      login,
      register,
      logout,
      refreshCurrentUser,
      completeOAuthLogin,
    }),
    [token, user, isLoading, login, register, logout, refreshCurrentUser, completeOAuthLogin],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
