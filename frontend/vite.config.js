import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), '');

  if (mode === 'production') {
    const requiredVariables = ['VITE_API_BASE_URL', 'VITE_OAUTH_LOGIN_URL'];
    const missingVariables = requiredVariables.filter((name) => !environment[name]?.trim());
    if (missingVariables.length > 0) {
      throw new Error(`Missing production environment variables: ${missingVariables.join(', ')}`);
    }
  }

  return {
    plugins: [react()],
  };
});
