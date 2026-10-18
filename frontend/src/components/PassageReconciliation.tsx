import { useState } from 'react';
import { useAuth } from '@/auth/auth-context';
import { api, getApiErrorMessage } from '@/services/api';
import type { MovementResult } from '@/types/api';
import { passageStateLabels } from '@/lib/presentation/movement';
import Modal from './Modal';
export default function PassageReconciliation({ movement, onUpdate }: { movement: MovementResult; onUpdate: (result: MovementResult) => void }) {
  const { hasPermission } = useAuth();
  const [open, setOpen] = useState(false);
  const [justification, setJustification] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [controllerSettled, setControllerSettled] = useState(false);
  const state = movement.passageState;
  const canReconcile = hasPermission('MOVEMENT_OVERRIDE') && movement.status !== 'DENIED' && state !== 'COMPLETED' && state !== 'FAILED' && state !== 'DENIED';
  const confirm = async (passed: boolean) => {
    if (busy) return;
    if (justification.trim().length < 10) { setError('Descreva a observação com pelo menos 10 caracteres.'); return; }
    if (state === 'UNKNOWN' && !controllerSettled) { setError('Verifique o controlador e confirme a ausência de comandos pendentes.'); return; }
    setBusy(true); setError('');
    try { onUpdate(await (controllerSettled ? api.movements.confirmPassage(movement.movementId, passed, justification.trim(), true) : api.movements.confirmPassage(movement.movementId, passed, justification.trim()))); setOpen(false); }
    catch (failure) { setError(getApiErrorMessage(failure)); }
    finally { setBusy(false); }
  };
  return <div><span>{state ? passageStateLabels[state] : 'Histórico sem confirmação física'}</span>
    {canReconcile && <button className="secondary-button" onClick={() => setOpen(true)}>Reconciliar passagem</button>}
    {open && <Modal title="Confirmar observação da passagem" onClose={() => { if (!busy) setOpen(false); }}>
      <p>Registre somente a passagem observada. A autorização e a aceitação do comando não confirmam a entrada ou saída.</p>
      <label>Justificativa<textarea value={justification} onChange={(event) => setJustification(event.target.value)} minLength={10} maxLength={1000} /></label>
      {state === 'UNKNOWN' && <label><input type="checkbox" checked={controllerSettled} onChange={(event) => setControllerSettled(event.target.checked)} />Verifiquei que o controlador não tem comandos pendentes.</label>}
      {error && <p role="alert">{error}</p>}
      <button disabled={busy} onClick={() => void confirm(true)}>Confirmar passagem observada</button>
      <button disabled={busy} onClick={() => void confirm(false)}>Registrar que não passou</button>
    </Modal>}
  </div>;
}
