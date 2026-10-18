import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import Modal from '@/components/Modal';

describe('Modal', () => {
  it('renders title', () => {
    render(<Modal title="Editar vaga" onClose={vi.fn()}>conteudo</Modal>);
    expect(screen.getByText('Editar vaga')).toBeInTheDocument();
  });

  it('renders children', () => {
    render(<Modal title="Teste" onClose={vi.fn()}>conteudo</Modal>);
    expect(screen.getByText('conteudo')).toBeInTheDocument();
  });

  it('renders description when provided', () => {
    render(<Modal title="Teste" description="Descricao" onClose={vi.fn()}>x</Modal>);
    expect(screen.getByText('Descricao')).toBeInTheDocument();
  });

  it('calls onClose when close button is clicked', () => {
    const onClose = vi.fn();
    render(<Modal title="Teste" onClose={onClose}>x</Modal>);
    fireEvent.click(screen.getByLabelText('Fechar'));
    expect(onClose).toHaveBeenCalled();
  });

  it('calls onClose on Escape key', () => {
    const onClose = vi.fn();
    render(<Modal title="Teste" onClose={onClose}>x</Modal>);
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });

  it('keeps field focus after a callback change and restores it only when unmounted', () => {
    const trigger = document.createElement('button');
    document.body.append(trigger);
    trigger.focus();
    try {
      const { rerender, unmount } = render(<Modal title="Observação" onClose={vi.fn()}><textarea aria-label="Justificativa" /></Modal>);
      const field = screen.getByRole('textbox', { name: 'Justificativa' });
      field.focus();
      rerender(<Modal title="Observação" onClose={vi.fn()}><textarea aria-label="Justificativa" /></Modal>);
      expect(field).toHaveFocus();
      unmount();
      expect(trigger).toHaveFocus();
    } finally { trigger.remove(); }
  });

  it('uses the current close callback after a rerender', () => {
    const previous = vi.fn();
    const current = vi.fn();
    const { rerender } = render(<Modal title="Observação" onClose={previous}>conteúdo</Modal>);
    rerender(<Modal title="Observação" onClose={current}>conteúdo</Modal>);
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(current).toHaveBeenCalledTimes(1);
    expect(previous).not.toHaveBeenCalled();
  });

  it('has dialog role and aria-label', () => {
    render(<Modal title="Editar vaga" onClose={vi.fn()}>x</Modal>);
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-label', 'Editar vaga');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
  });
});
