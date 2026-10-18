import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import UnifiedRegistrationPage from '@/pages/UnifiedRegistrationPage';
import { ApiError } from '@/services/api';

const mocks = vi.hoisted(() => ({
  createRegistration: vi.fn(),
  createApartment: vi.fn(),
  createResident: vi.fn(),
  createParking: vi.fn(),
  createVehicle: vi.fn(),
}));

vi.mock('@/services/api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/api')>();
  return {
    ...actual,
    api: {
      ...actual.api,
      registrations: { create: mocks.createRegistration },
      apartments: { ...actual.api.apartments, create: mocks.createApartment, createResident: mocks.createResident },
      parking: { ...actual.api.parking, create: mocks.createParking },
      vehicles: { ...actual.api.vehicles, create: mocks.createVehicle },
    },
  };
});

beforeEach(() => {
  vi.resetAllMocks();
  mocks.createRegistration.mockResolvedValue({ apartmentId: 1, residentIds: [2], parkingSpaceId: 3, vehicleId: 4 });
});

function renderPage() {
  return render(<MemoryRouter><UnifiedRegistrationPage /></MemoryRouter>);
}

async function fillRegistration(withParkingAndVehicle = true) {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText(/Número/), '101');
  await user.click(screen.getByRole('button', { name: 'Próximo' }));
  await user.type(screen.getByRole('textbox', { name: 'Nome do morador' }), 'Maria Silva');
  await user.click(screen.getByRole('button', { name: 'Adicionar morador' }));
  await user.click(screen.getByRole('button', { name: 'Próximo' }));
  if (withParkingAndVehicle) {
    await user.type(screen.getByLabelText('Número da vaga'), '127');
    await user.click(screen.getByRole('button', { name: 'Próximo' }));
    await user.type(screen.getByLabelText('Placa'), 'abc1d23');
    await user.type(screen.getByLabelText('Marca'), 'Fiat');
    await user.type(screen.getByLabelText('Modelo'), 'Uno');
    await user.type(screen.getByLabelText('Cor'), 'Branco');
  } else {
    await user.click(screen.getByRole('button', { name: 'Pular etapa' }));
  }
  return user;
}

describe('UnifiedRegistrationPage', () => {
  it('renders title and description', () => {
    render(<MemoryRouter><UnifiedRegistrationPage /></MemoryRouter>);
    expect(screen.getByText('Cadastro Completo')).toBeInTheDocument();
    expect(screen.getByText(/Cadastre apartamento/)).toBeInTheDocument();
  });

  it('renders step bar with 4 steps', () => {
    render(<MemoryRouter><UnifiedRegistrationPage /></MemoryRouter>);
    expect(screen.getByText('Apartamento')).toBeInTheDocument();
    expect(screen.getByText('Moradores')).toBeInTheDocument();
    expect(screen.getByText('Vaga')).toBeInTheDocument();
    expect(screen.getByText('Veículo')).toBeInTheDocument();
  });

  it('shows apartment form first', () => {
    render(<MemoryRouter><UnifiedRegistrationPage /></MemoryRouter>);
    expect(screen.getByLabelText(/Número/)).toBeInTheDocument();
  });

  it('shows next button', () => {
    render(<MemoryRouter><UnifiedRegistrationPage /></MemoryRouter>);
    expect(screen.getByText('Próximo')).toBeInTheDocument();
  });

  it('shows skip button for optional steps', () => {
    render(<MemoryRouter><UnifiedRegistrationPage /></MemoryRouter>);
    expect(screen.queryByText('Pular etapa')).not.toBeInTheDocument();
  });

  it('blocks an empty apartment number and focuses the invalid field', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Próximo' }));
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByLabelText(/Número/)).toHaveFocus();
    expect(mocks.createRegistration).not.toHaveBeenCalled();
  });

  it('requires a resident before advancing', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.type(screen.getByLabelText(/Número/), '101');
    await user.click(screen.getByRole('button', { name: 'Próximo' }));
    await user.click(screen.getByRole('button', { name: 'Próximo' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Adicione pelo menos um morador.');
    expect(screen.getByRole('textbox', { name: 'Nome do morador' })).toHaveFocus();
    expect(mocks.createRegistration).not.toHaveBeenCalled();
  });

  it('saves the complete registration with one idempotent request and preserves inactive vehicle', async () => {
    renderPage();
    const user = await fillRegistration();
    await user.click(screen.getByRole('checkbox', { name: 'Ativo' }));
    await user.click(screen.getByRole('button', { name: 'Salvar Tudo' }));

    expect(await screen.findByRole('heading', { name: 'Cadastro realizado com sucesso!' })).toBeInTheDocument();
    expect(mocks.createRegistration).toHaveBeenCalledTimes(1);
    expect(mocks.createRegistration.mock.calls[0][0]).toEqual(expect.objectContaining({
      requestId: expect.stringMatching(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i),
      apartmentNumber: '101', apartmentActive: true,
      residents: [{ fullName: 'Maria Silva', active: true }],
      parkingNumber: '127', parkingType: 'ASSIGNED', parkingActive: true,
      plate: 'ABC1D23', brand: 'Fiat', model: 'Uno', color: 'Branco', vehicleActive: false,
    }));
    expect(mocks.createApartment).not.toHaveBeenCalled();
    expect(mocks.createResident).not.toHaveBeenCalled();
    expect(mocks.createParking).not.toHaveBeenCalled();
    expect(mocks.createVehicle).not.toHaveBeenCalled();
  });

  it('allows registration without optional parking and vehicle', async () => {
    renderPage();
    const user = await fillRegistration(false);
    await user.click(screen.getByRole('button', { name: 'Salvar Tudo' }));
    expect(await screen.findByRole('heading', { name: 'Cadastro realizado com sucesso!' })).toBeInTheDocument();
    expect(mocks.createRegistration.mock.calls[0][0]).toEqual(expect.objectContaining({
      apartmentNumber: '101', residents: [{ fullName: 'Maria Silva', active: true }],
    }));
    expect(mocks.createVehicle).not.toHaveBeenCalled();
  });

  it('rejects an invalid plate without sending a request', async () => {
    renderPage();
    const user = await fillRegistration();
    await user.clear(screen.getByLabelText('Placa'));
    await user.type(screen.getByLabelText('Placa'), '123');
    await user.click(screen.getByRole('button', { name: 'Salvar Tudo' }));
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByLabelText('Placa')).toHaveAttribute('aria-invalid', 'true');
    expect(mocks.createRegistration).not.toHaveBeenCalled();
  });

  it.each([
    ['conflict', new ApiError('Apartamento já cadastrado.', 409, { message: 'Apartamento já cadastrado.' }), 'Apartamento já cadastrado.'],
    ['validation', new ApiError('Morador inválido.', 400, { message: 'Morador inválido.' }), 'Morador inválido.'],
    ['API failure', new Error('Serviço temporariamente indisponível.'), 'Serviço temporariamente indisponível.'],
    ['timeout', new Error('Tempo limite excedido. Tente novamente.'), 'Tempo limite excedido.'],
  ])('keeps the form and the request identifier after %s, allowing a safe retry', async (_scenario, error, message) => {
    mocks.createRegistration.mockRejectedValueOnce(error);
    renderPage();
    const user = await fillRegistration();
    await user.click(screen.getByRole('button', { name: 'Salvar Tudo' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(screen.getByLabelText('Placa')).toHaveValue('ABC1D23');
    expect(screen.getByRole('button', { name: 'Salvar Tudo' })).toBeEnabled();
    const firstPayload = mocks.createRegistration.mock.calls[0][0];
    await user.click(screen.getByRole('button', { name: 'Salvar Tudo' }));

    expect(await screen.findByRole('heading', { name: 'Cadastro realizado com sucesso!' })).toBeInTheDocument();
    expect(mocks.createRegistration.mock.calls[1][0]).toEqual(firstPayload);
  });

  it('prevents repeated submission while the registration is pending', async () => {
    let finish: (() => void) | undefined;
    mocks.createRegistration.mockReturnValue(new Promise<void>((resolve) => { finish = resolve; }));
    renderPage();
    const user = await fillRegistration();
    await user.click(screen.getByRole('button', { name: 'Salvar Tudo' }));
    const button = screen.getByRole('button', { name: 'Salvando...' });
    expect(button).toBeDisabled();
    await user.click(button);
    expect(mocks.createRegistration).toHaveBeenCalledTimes(1);
    finish?.();
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Cadastro realizado com sucesso!' })).toBeInTheDocument());
  });

  it('uses a new request identifier after the user changes rejected registration data', async () => {
    mocks.createRegistration.mockRejectedValueOnce(new ApiError('Placa já cadastrada.', 409, { message: 'Placa já cadastrada.' }));
    renderPage();
    const user = await fillRegistration();
    await user.click(screen.getByRole('button', { name: 'Salvar Tudo' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Placa já cadastrada.');
    const firstRequest = mocks.createRegistration.mock.calls[0][0];
    await user.clear(screen.getByLabelText('Placa'));
    await user.type(screen.getByLabelText('Placa'), 'DEF4G56');
    await user.click(screen.getByRole('button', { name: 'Salvar Tudo' }));
    expect(await screen.findByRole('heading', { name: 'Cadastro realizado com sucesso!' })).toBeInTheDocument();
    const secondRequest = mocks.createRegistration.mock.calls[1][0];
    expect(secondRequest.plate).toBe('DEF4G56');
    expect(secondRequest.requestId).not.toBe(firstRequest.requestId);
  });
});
