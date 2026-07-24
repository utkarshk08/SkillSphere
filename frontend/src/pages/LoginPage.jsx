import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../components/layout/AuthLayout';
import useAuth from '../hooks/useAuth';

const initialCredentials = { email: '', password: '' };
const OAUTH_DESTINATION_KEY = 'skillsphere_oauth_destination';

export default function LoginPage() {
  const [credentials, setCredentials] = useState(initialCredentials);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const destination = location.state?.from?.pathname || '/dashboard';

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  function handleChange(event) {
    const { name, value } = event.target;
    setCredentials((current) => ({ ...current, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      await login(credentials);
      navigate(destination, { replace: true });
    } catch (submissionError) {
      setError(submissionError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  function beginGoogleLogin() {
    // A browser redirect to Google loses React route state, so retain the internal
    // destination briefly and let the callback restore it after the JWT is stored.
    if (destination !== '/dashboard') {
      sessionStorage.setItem(OAUTH_DESTINATION_KEY, destination);
    } else {
      sessionStorage.removeItem(OAUTH_DESTINATION_KEY);
    }
    window.location.assign(
      import.meta.env.VITE_OAUTH_LOGIN_URL || 'http://localhost:8080/oauth2/authorization/google',
    );
  }

  return (
    <AuthLayout>
      <div className="auth-card">
        <div className="auth-card-heading">
          <p className="eyebrow">Welcome back</p>
          <h2>Sign in to SkillSphere</h2>
          <p>Use your email and password to continue.</p>
        </div>

        {error && <p className="form-message form-message-error" role="alert">{error}</p>}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <label className="field">
            <span>Email</span>
            <input
              autoComplete="email"
              name="email"
              onChange={handleChange}
              required
              type="email"
              value={credentials.email}
            />
          </label>
          <label className="field">
            <span>Password</span>
            <input
              autoComplete="current-password"
              name="password"
              onChange={handleChange}
              required
              type="password"
              value={credentials.password}
            />
          </label>
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="auth-divider"><span>or</span></div>
        <button className="button button-google button-full" onClick={beginGoogleLogin} type="button">
          <span className="google-letter" aria-hidden="true">G</span>
          Continue with Google
        </button>

        <p className="auth-switch">
          New to SkillSphere? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </AuthLayout>
  );
}
