import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // so fetch('/api/...') in the app reaches the Spring Boot backend during dev
      '/api': 'http://localhost:8080'
    }
  }
});
