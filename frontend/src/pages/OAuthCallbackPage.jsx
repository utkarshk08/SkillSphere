import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import AuthLayout from '../components/layout/AuthLayout';
import useAuth from '../hooks/useAuth';

const OAUTH_DESTINATION_KEY = 'skillsphere_oauth_destination';

export default function OAuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const [error, setError] = useState('');
  const { completeOAuthLogin } = useAuth();
  const navigate = useNavigate();
  const oauthError = searchParams.get('error');
  const oauthMessage = searchParams.get('message');
  const token = searchParams.get('accessToken') || searchParams.get('token');

  useEffect(() => {
    let isCurrent = true;

    if (oauthError) {
      setError(oauthMessage || 'Google sign-in was cancelled or could not be completed.');
      return () => {
        isCurrent = false;
      };
    }

    if (!token) {
      setError('Google sign-in did not return an access token.');
      return () => {
        isCurrent = false;
      };
    }

    async function finishLogin() {
      try {
        await completeOAuthLogin(token);
        if (isCurrent) {
          const destination = sessionStorage.getItem(OAUTH_DESTINATION_KEY) || '/dashboard';
          sessionStorage.removeItem(OAUTH_DESTINATION_KEY);
          navigate(destination, { replace: true });
        }
      } catch (callbackError) {
        if (isCurrent) {
          setError(callbackError.message);
        }
      }
    }

    finishLogin();

    return () => {
      isCurrent = false;
    };
  }, [completeOAuthLogin, navigate, oauthError, oauthMessage, token]);

  return (
    <AuthLayout>
      <div className="auth-card callback-card" aria-live="polite">
        {error ? (
          <>
            <p className="eyebrow">Google sign-in</p>
            <h2>We could not sign you in</h2>
            <p className="form-message form-message-error" role="alert">{error}</p>
            <button className="button button-primary button-full" onClick={() => navigate('/login')} type="button">
              Return to sign in
            </button>
          </>
        ) : (
          <>
            <span className="loading-spinner" aria-hidden="true" />
            <h2>Completing Google sign-in</h2>
            <p>Please wait while we securely finish signing you in.</p>
          </>
        )}
      </div>
    </AuthLayout>
  );
}
