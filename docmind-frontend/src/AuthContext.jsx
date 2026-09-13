import { createContext, useContext, useState, useCallback } from 'react';
import { api } from './api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const stored = localStorage.getItem('docmind.auth');
    return stored ? JSON.parse(stored) : null;
  });

  const persist = (value) => {
    setAuth(value);
    if (value) localStorage.setItem('docmind.auth', JSON.stringify(value));
    else localStorage.removeItem('docmind.auth');
  };

  const login = useCallback(async (username, password) => {
    const result = await api.login({ username, password });
    persist(result);
    return result;
  }, []);

  const register = useCallback(async (username, email, password) => {
    const result = await api.register({ username, email, password });
    persist(result);
    return result;
  }, []);

  const logout = useCallback(() => persist(null), []);

  return (
    <AuthContext.Provider value={{ auth, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
