import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';
import { labels } from '@/lib/presentation/labels';

type ModalProps = {
  title: string;
  description?: string;
  children: ReactNode;
  onClose: () => void;
  width?: 'small' | 'medium' | 'large';
};

export default function Modal({ title, description, children, onClose, width = 'medium' }: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    previousFocusRef.current = document.activeElement as HTMLElement;
    dialogRef.current?.focus();
    return () => { previousFocusRef.current?.focus(); };
  }, []);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.stopPropagation();
        onClose();
        return;
      }
      if (event.key !== 'Tab' || !dialogRef.current) return;
      const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey) {
        if (document.activeElement === first) { event.preventDefault(); last.focus(); }
      } else {
        if (document.activeElement === last) { event.preventDefault(); first.focus(); }
      }
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose]);

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section ref={dialogRef} className={`modal-card ${width}`} role="dialog" aria-modal="true" aria-label={title} tabIndex={-1}>
        <header className="modal-header">
          <div>
            <span className="eyebrow">{labels.modal.registrationEyebrow}</span>
            <h2>{title}</h2>
            {description && <p>{description}</p>}
          </div>
          <button className="icon-button" onClick={onClose} aria-label={labels.modal.closeLabel}><X className="h-4 w-4" /></button>
        </header>
        <div className="modal-body">{children}</div>
      </section>
    </div>
  );
}
