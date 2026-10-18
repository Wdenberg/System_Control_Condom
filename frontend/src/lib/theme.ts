import { useCallback, useEffect, useState } from 'react';

type Theme = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'garage-control-theme';

function getSystemTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light';
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function resolveTheme(theme: Theme): 'light' | 'dark' {
  return theme === 'system' ? getSystemTheme() : theme;
}

function applyTheme(resolved: 'light' | 'dark') {
  document.documentElement.setAttribute('data-theme', resolved);
}

function loadStoredTheme(): Theme {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'system') return stored;
  } catch { /* ignore */ }
  return 'system';
}

export function useTheme() {
  const [theme, setThemeState] = useState<Theme>(loadStoredTheme);
  const [resolved, setResolved] = useState<'light' | 'dark'>(() => resolveTheme(loadStoredTheme()));

  const setTheme = useCallback((next: Theme) => {
    setThemeState(next);
    setResolved(resolveTheme(next));
    try { localStorage.setItem(STORAGE_KEY, next); } catch { /* ignore */ }
  }, []);

  useEffect(() => {
    applyTheme(resolved);
  }, [resolved]);

  useEffect(() => {
    if (theme !== 'system') return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = () => {
      const next = getSystemTheme();
      setResolved(next);
      applyTheme(next);
    };
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [theme]);

  useEffect(() => {
    applyTheme(resolveTheme(theme));
  }, []);

  return { theme, resolved, setTheme };
}
