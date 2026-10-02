import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import path from 'node:path'

export default defineConfig({
  plugins: [react()],

  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },

  test: {
    environment: 'jsdom',
    globals: true,

    setupFiles: ['./tests/setup.js'],

    include: ['tests/**/*.{test,spec}.{js,jsx}'],
    exclude: ['node_modules', 'dist', '.idea', '.git', '.cache'],

    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: [
        'src/modules/auth/authApi.js',
        'src/context/AuthContext.jsx',
        'src/services/apiClient.js',
        'src/modules/auth/pages/LoginPage.jsx',
        'src/modules/auth/pages/SignupPage.jsx',
        'src/modules/auth/pages/AccountVerificationPage.jsx',
        'src/modules/auth/pages/ResetPasswordPage.jsx',
        'src/components/Input/Input.jsx',
      ],
      exclude: ['node_modules/', 'tests/', '**/*.config.js'],
      thresholds: {
        lines: 80,
        branches: 65,
        functions: 80,
        statements: 80,
      },
    },

    reporters: ['verbose'],
    testTimeout: 10000,
  },
})
