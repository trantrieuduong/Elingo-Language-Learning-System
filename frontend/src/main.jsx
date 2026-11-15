import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './i18n';
import './index.css';
import App from './App.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import { ThemeProvider } from './context/ThemeContext.jsx';
import { SocketErrorProvider } from './context/SocketErrorContext.jsx';
import { XpProvider } from './modules/gamification/context/XpContext.jsx';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider>
      <AuthProvider>
        <SocketErrorProvider>
          <XpProvider>
            <App />
          </XpProvider>
        </SocketErrorProvider>
      </AuthProvider>
    </ThemeProvider>
  </StrictMode>
);
