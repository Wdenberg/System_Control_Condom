import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import ParkingStatusBadge from '@/components/ui/ParkingStatusBadge';
import ParkingTypeBadge from '@/components/ui/ParkingTypeBadge';
import MovementDirectionBadge from '@/components/ui/MovementDirectionBadge';
import MovementDecisionBadge from '@/components/ui/MovementDecisionBadge';
import ActiveStatusBadge from '@/components/ui/ActiveStatusBadge';
import RoleBadge from '@/components/ui/RoleBadge';

describe('ParkingStatusBadge', () => {
  it('renders AVAILABLE as Disponível', () => {
    render(<ParkingStatusBadge status="AVAILABLE" />);
    expect(screen.getByText('Disponível')).toBeInTheDocument();
  });

  it('renders OCCUPIED as Ocupada', () => {
    render(<ParkingStatusBadge status="OCCUPIED" />);
    expect(screen.getByText('Ocupada')).toBeInTheDocument();
  });

  it('renders BLOCKED as Bloqueada', () => {
    render(<ParkingStatusBadge status="BLOCKED" />);
    expect(screen.getByText('Bloqueada')).toBeInTheDocument();
  });

  it('applies success variant class for AVAILABLE', () => {
    render(<ParkingStatusBadge status="AVAILABLE" />);
    expect(screen.getByText('Disponível').className).toContain('success');
  });

  it('applies danger variant class for OCCUPIED', () => {
    render(<ParkingStatusBadge status="OCCUPIED" />);
    expect(screen.getByText('Ocupada').className).toContain('danger');
  });

  it('applies warning variant class for BLOCKED', () => {
    render(<ParkingStatusBadge status="BLOCKED" />);
    expect(screen.getByText('Bloqueada').className).toContain('warning');
  });
});

describe('ParkingTypeBadge', () => {
  it('renders ASSIGNED as Própria', () => {
    render(<ParkingTypeBadge type="ASSIGNED" />);
    expect(screen.getByText('Própria')).toBeInTheDocument();
  });

  it('renders RENTED as Alugada', () => {
    render(<ParkingTypeBadge type="RENTED" />);
    expect(screen.getByText('Alugada')).toBeInTheDocument();
  });

  it('renders EXTRA as Extra', () => {
    render(<ParkingTypeBadge type="EXTRA" />);
    expect(screen.getByText('Extra')).toBeInTheDocument();
  });

  it('renders VISITOR as Visitante', () => {
    render(<ParkingTypeBadge type="VISITOR" />);
    expect(screen.getByText('Visitante')).toBeInTheDocument();
  });
});

describe('MovementDirectionBadge', () => {
  it('renders ENTRY as Entrada', () => {
    render(<MovementDirectionBadge direction="ENTRY" />);
    expect(screen.getByText('Entrada')).toBeInTheDocument();
  });

  it('renders EXIT as Saída', () => {
    render(<MovementDirectionBadge direction="EXIT" />);
    expect(screen.getByText('Saída')).toBeInTheDocument();
  });

  it('applies entry variant class for ENTRY', () => {
    render(<MovementDirectionBadge direction="ENTRY" />);
    expect(screen.getByText('Entrada').className).toContain('entry');
  });

  it('applies exit variant class for EXIT', () => {
    render(<MovementDirectionBadge direction="EXIT" />);
    expect(screen.getByText('Saída').className).toContain('exit');
  });
});

describe('MovementDecisionBadge', () => {
  it('renders AUTHORIZED decision as Autorizado', () => {
    render(<MovementDecisionBadge decision="AUTHORIZED" />);
    expect(screen.getByText('Autorizado')).toBeInTheDocument();
  });

  it('renders DENIED decision as Negado', () => {
    render(<MovementDecisionBadge decision="DENIED" />);
    expect(screen.getByText('Negado')).toBeInTheDocument();
  });

  it('renders UNKNOWN_VEHICLE decision with translated label', () => {
    render(<MovementDecisionBadge decision="UNKNOWN_VEHICLE" />);
    expect(screen.getByText('Veículo não cadastrado')).toBeInTheDocument();
  });

  it('applies success variant class for AUTHORIZED', () => {
    render(<MovementDecisionBadge decision="AUTHORIZED" />);
    expect(screen.getByText('Autorizado').className).toContain('success');
  });

  it('applies danger variant class for DENIED', () => {
    render(<MovementDecisionBadge decision="DENIED" />);
    expect(screen.getByText('Negado').className).toContain('danger');
  });
});

describe('ActiveStatusBadge', () => {
  it('renders active as Ativo', () => {
    render(<ActiveStatusBadge active={true} />);
    expect(screen.getByText('Ativo')).toBeInTheDocument();
  });

  it('renders inactive as Inativo', () => {
    render(<ActiveStatusBadge active={false} />);
    expect(screen.getByText('Inativo')).toBeInTheDocument();
  });

  it('applies success variant class for active', () => {
    render(<ActiveStatusBadge active={true} />);
    expect(screen.getByText('Ativo').className).toContain('success');
  });

  it('applies danger variant class for inactive', () => {
    render(<ActiveStatusBadge active={false} />);
    expect(screen.getByText('Inativo').className).toContain('danger');
  });
});

describe('RoleBadge', () => {
  it('renders ADMIN as Administrador', () => {
    render(<RoleBadge role="ADMIN" />);
    expect(screen.getByText('Administrador')).toBeInTheDocument();
  });

  it('renders OPERATOR as Operador', () => {
    render(<RoleBadge role="OPERATOR" />);
    expect(screen.getByText('Operador')).toBeInTheDocument();
  });

  it('renders VIEWER as Visualizador', () => {
    render(<RoleBadge role="VIEWER" />);
    expect(screen.getByText('Visualizador')).toBeInTheDocument();
  });

  it('renders ATTENDANT as Atendente', () => {
    render(<RoleBadge role="ATTENDANT" />);
    expect(screen.getByText('Atendente')).toBeInTheDocument();
  });

  it('renders SUPER_ADMIN as Super Administrador', () => {
    render(<RoleBadge role="SUPER_ADMIN" />);
    expect(screen.getByText('Super Administrador')).toBeInTheDocument();
  });

  it('applies info variant class', () => {
    render(<RoleBadge role="ADMIN" />);
    expect(screen.getByText('Administrador').className).toContain('accent-soft');
  });
});
