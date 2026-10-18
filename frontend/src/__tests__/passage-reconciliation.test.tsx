import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PassageReconciliation from '@/components/PassageReconciliation';
import type { MovementResult } from '@/types/api';

const mocks = vi.hoisted(() => ({ hasPermission: vi.fn(), confirmPassage: vi.fn() }));
vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ hasPermission: mocks.hasPermission }) }));
vi.mock('@/services/api', () => ({
  api: { movements: { confirmPassage: mocks.confirmPassage } },
  getApiErrorMessage: (failure: unknown) => failure instanceof Error ? failure.message : 'Não foi possível confirmar.',
}));

const movement: MovementResult = {
  movementId: 7, plate: 'ABC1D23', direction: 'ENTRY', status: 'AUTHORIZED', decision: 'AUTHORIZED',
  vehicleId: 1, vehicleOwner: 'Maria Silva', apartmentNumber: '101', parkingSpaceId: 3, parkingSpaceNumber: '127',
  eventTime: '2026-10-18T10:00:00-03:00', passageState: 'RECONCILIATION_REQUIRED',
};

beforeEach(() => {
  vi.resetAllMocks();
  mocks.hasPermission.mockImplementation((permission: string) => permission === 'MOVEMENT_OVERRIDE');
  mocks.confirmPassage.mockResolvedValue({ ...movement, passageState: 'COMPLETED' });
});

describe('PassageReconciliation', () => {
  it('hides physical reconciliation without MOVEMENT_OVERRIDE', () => {
    mocks.hasPermission.mockReturnValue(false);
    render(<PassageReconciliation movement={movement} onUpdate={vi.fn()} />);
    expect(screen.queryByRole('button', { name: 'Reconciliar passagem' })).not.toBeInTheDocument();
  });

  it.each(['COMPLETED', 'FAILED', 'DENIED'] as const)('does not offer reconciliation for %s', (state) => {
    render(<PassageReconciliation movement={{ ...movement, passageState: state }} onUpdate={vi.fn()} />);
    expect(screen.queryByRole('button', { name: 'Reconciliar passagem' })).not.toBeInTheDocument();
  });

  it('does not offer reconciliation for denied movements', () => {
    render(<PassageReconciliation movement={{ ...movement, status: 'DENIED', decision: 'UNKNOWN_VEHICLE' }} onUpdate={vi.fn()} />);
    expect(screen.queryByRole('button', { name: 'Reconciliar passagem' })).not.toBeInTheDocument();
  });

  it('requires a meaningful justification before registering a physical observation', async () => {
    const user = userEvent.setup();
    render(<PassageReconciliation movement={movement} onUpdate={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: 'Reconciliar passagem' }));
    await user.type(screen.getByRole('textbox', { name: 'Justificativa' }), 'curta');
    await user.click(screen.getByRole('button', { name: 'Confirmar passagem observada' }));
    expect(screen.getByRole('alert')).toHaveTextContent('pelo menos 10 caracteres');
    expect(mocks.confirmPassage).not.toHaveBeenCalled();
  });

  it.each([
    [true, 'Confirmar passagem observada'],
    [false, 'Registrar que não passou'],
  ] as const)('records passed=%s with the observed justification and updates the movement', async (passed, button) => {
    const user = userEvent.setup();
    const update = vi.fn();
    const result = { ...movement, passageState: passed ? 'COMPLETED' : 'FAILED' };
    mocks.confirmPassage.mockResolvedValue(result);
    render(<PassageReconciliation movement={movement} onUpdate={update} />);
    await user.click(screen.getByRole('button', { name: 'Reconciliar passagem' }));
    await user.type(screen.getByRole('textbox', { name: 'Justificativa' }), '  Observado na portaria.  ');
    await user.click(screen.getByRole('button', { name: button }));
    await waitFor(() => expect(update).toHaveBeenCalledWith(result));
    expect(mocks.confirmPassage).toHaveBeenCalledWith(7, passed, 'Observado na portaria.');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('keeps the observation available to retry after an API failure', async () => {
    const user = userEvent.setup();
    mocks.confirmPassage.mockRejectedValueOnce(new Error('Sessão sem autorização para reconciliar.'));
    const update = vi.fn();
    render(<PassageReconciliation movement={movement} onUpdate={update} />);
    await user.click(screen.getByRole('button', { name: 'Reconciliar passagem' }));
    await user.type(screen.getByRole('textbox', { name: 'Justificativa' }), 'Passagem observada na portaria.');
    await user.click(screen.getByRole('button', { name: 'Confirmar passagem observada' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Sessão sem autorização');
    expect(screen.getByRole('textbox', { name: 'Justificativa' })).toHaveValue('Passagem observada na portaria.');
    expect(update).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Confirmar passagem observada' }));
    await waitFor(() => expect(update).toHaveBeenCalledTimes(1));
  });

  it('requires controller verification for an unknown physical outcome', async () => {
    const user = userEvent.setup();
    render(<PassageReconciliation movement={{ ...movement, passageState: 'UNKNOWN' }} onUpdate={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: 'Reconciliar passagem' }));
    await user.type(screen.getByRole('textbox', { name: 'Justificativa' }), 'Controlador verificado na portaria.');
    await user.click(screen.getByRole('button', { name: 'Registrar que não passou' }));
    expect(screen.getByRole('alert')).toHaveTextContent('ausência de comandos pendentes');
    expect(mocks.confirmPassage).not.toHaveBeenCalled();

    await user.click(screen.getByRole('checkbox', { name: 'Verifiquei que o controlador não tem comandos pendentes.' }));
    await user.click(screen.getByRole('button', { name: 'Registrar que não passou' }));
    await waitFor(() => expect(mocks.confirmPassage).toHaveBeenCalledWith(7, false, 'Controlador verificado na portaria.', true));
  });

  it('disables both physical outcomes until the pending request finishes', async () => {
    const user = userEvent.setup();
    let finish: ((value: MovementResult) => void) | undefined;
    mocks.confirmPassage.mockReturnValue(new Promise<MovementResult>((resolve) => { finish = resolve; }));
    render(<PassageReconciliation movement={movement} onUpdate={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: 'Reconciliar passagem' }));
    await user.type(screen.getByRole('textbox', { name: 'Justificativa' }), 'Passagem observada na portaria.');
    await user.click(screen.getByRole('button', { name: 'Confirmar passagem observada' }));
    expect(screen.getByRole('button', { name: 'Confirmar passagem observada' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Registrar que não passou' })).toBeDisabled();
    await act(async () => { finish?.({ ...movement, passageState: 'COMPLETED' }); });
    expect(mocks.confirmPassage).toHaveBeenCalledTimes(1);
  });
});
