/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// In development the page is on Vite's port (5173) and the API on Spring Boot's (8080). The proxy makes
// both look like one origin to the browser: /api/... goes to Spring Boot, everything else to Vite.
// Same origin means no CORS, and cookies (Day 29) just work. Production does the same with Caddy (Day 39).
const apiUrl = process.env.ORUDESK_API_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': apiUrl,
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts'],
  },
});
