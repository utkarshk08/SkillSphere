const TOKEN_STORAGE_KEY = 'skillsphere_access_token';
const USER_STORAGE_KEY = 'skillsphere_user';

export function getStoredToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function getStoredUser() {
  const storedUser = localStorage.getItem(USER_STORAGE_KEY);

  if (!storedUser) {
    return null;
  }

  try {
    return JSON.parse(storedUser);
  } catch {
    localStorage.removeItem(USER_STORAGE_KEY);
    return null;
  }
}

export function saveSession(token, user) {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user || {}));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(USER_STORAGE_KEY);
}

function decodeBase64Url(value) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
  const paddedValue = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
  return decodeURIComponent(
    atob(paddedValue)
      .split('')
      .map((character) => `%${(`00${character.charCodeAt(0).toString(16)}`).slice(-2)}`)
      .join(''),
  );
}

/**
 * Reads claims only to display basic account information before /auth/me responds.
 * It does not validate the JWT; Spring Security validates it on every protected call.
 */
export function getUserFromToken(token) {
  if (!token) {
    return {};
  }

  try {
    const payload = token.split('.')[1];
    const claims = JSON.parse(decodeBase64Url(payload));

    return {
      username: claims.username || claims.sub,
      email: claims.email,
      role: claims.role,
    };
  } catch {
    return {};
  }
}

export function extractSession(responseData) {
  const token = responseData?.accessToken || responseData?.token;
  const responseUser = responseData?.user || responseData || {};

  if (!token) {
    return null;
  }

  return {
    token,
    user: {
      ...getUserFromToken(token),
      ...responseUser,
    },
  };
}
