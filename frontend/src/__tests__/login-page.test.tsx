import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router';
import LoginPage from '@/pages/LoginPage';

const login = vi.fn();
const authState = { isAuthenticated: false, login, navigate: vi.fn() };

vi.mock('@/auth/auth-context', () => ({
  useAuth: () => ({ isAuthenticated: authState.isAuthenticated, login }),
}));

const navigate = authState.navigate;
vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return { ...actual, useNavigate: () => navigate };
});

beforeEach(() => {
  login.mockReset();
  navigate.mockReset();
});

describe('LoginPage', () => {
  it('submits credentials and navigates to dashboard', async () => {
    login.mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(<MemoryRouter><LoginPage /></MemoryRouter>);

    await user.type(screen.getByLabelText('Usuário'), 'admin');
    await user.type(screen.getByLabelText('Senha'), 'secret');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(login).toHaveBeenCalledWith('admin', 'secret'));
    expect(navigate).toHaveBeenCalledWith('/dashboard', { replace: true });
  });

  it('shows an error for invalid credentials', async () => {
    login.mockRejectedValue(new Error('UNAUTHORIZED'));
    const user = userEvent.setup();
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await user.type(screen.getByLabelText('Usuário'), 'admin');
    await user.type(screen.getByLabelText('Senha'), 'wrong');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Usuário ou senha inválidos.');
  });
});
