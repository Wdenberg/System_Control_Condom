import PageHeader from '@/components/PageHeader';
import { formatProperName, LIMITS } from '@/lib/validation';
import { labels } from '@/lib/presentation/labels';
import { useUnifiedRegistration } from '@/hooks/useUnifiedRegistration';
const l = labels.unifiedRegistration;
type Step = 1 | 2 | 3 | 4;
export default function UnifiedRegistrationPage() {
  const { step, setStep, form, fieldErrors, saving, success, globalError, residentNameInput, setResidentNameInput, nameInputRef, setField, clearFieldError, goNext, goPrev, addResident, removeResident, handleSave } = useUnifiedRegistration();
  if (success) {
    return (
      <section className="page-container">
        <PageHeader title={l.title} description={l.description} />
        <section className="panel">
          <div className="step-success">
            <div className="step-success-icon">✓</div>
            <h2>{l.success}</h2>
            <p>{l.successDesc}</p>
          </div>
        </section>
      </section>
    );
  }

  const canSkip = step === 3 || step === 4;

  return (
    <section className="page-container">
      <PageHeader title={l.title} description={l.description} />
      <section className="panel">
        <div className="step-bar">
          {([1, 2, 3, 4] as Step[]).map((s) => (
            <div key={s} className={`step-item ${step === s ? 'active' : ''} ${step > s ? 'done' : ''}`}>
              <span className="step-number">{step > s ? '✓' : s}</span>
              <span className="step-label">{s === 1 ? l.stepApartment : s === 2 ? l.stepResidents : s === 3 ? l.stepParking : l.stepVehicle}</span>
            </div>
          ))}
        </div>

        {globalError && (
          <div className="banner warning" role="alert">
            <strong>{l.errorTitle}:</strong> {globalError}
          </div>
        )}

        <div className="step-content">
          {step === 1 && (
            <div className="form-grid">
              <label>
                <span>{labels.apartments.numberLabel} *</span>
                <input
                  name="apartmentNumber"
                  type="text"
                  value={form.apartmentNumber}
                  onChange={(e) => setField('apartmentNumber', e.target.value)}
                  maxLength={LIMITS.apartmentNumber.max}
                  aria-invalid={Boolean(fieldErrors.apartmentNumber)}
                  aria-describedby={fieldErrors.apartmentNumber ? 'err-apartmentNumber' : undefined}
                  autoFocus
                />
                {fieldErrors.apartmentNumber && <small className="field-error" id="err-apartmentNumber" role="alert">{fieldErrors.apartmentNumber}</small>}
              </label>
              <label className="checkbox-field">
                <input
                  type="checkbox"
                  checked={form.apartmentActive}
                  onChange={(e) => setField('apartmentActive', e.target.checked)}
                />
                <span>{labels.apartments.activeLabel}</span>
              </label>
            </div>
          )}

          {step === 2 && (
            <div>
              <p className="step-desc">{l.stepResidentsDesc}</p>
              <div className="resident-form-row">
                <div style={{ flex: 1 }}>
                  <input
                    ref={nameInputRef}
                    aria-label={l.nameLabel}
                    value={residentNameInput}
                    onChange={(e) => { setResidentNameInput(e.target.value); clearFieldError('residentName'); }}
                    onBlur={() => { if (residentNameInput.trim()) setResidentNameInput(formatProperName(residentNameInput)); }}
                    onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addResident(); } }}
                    placeholder={l.namePlaceholder}
                    maxLength={LIMITS.residentName.max}
                    aria-invalid={Boolean(fieldErrors.residentName)}
                    aria-describedby={fieldErrors.residentName ? 'err-residentName' : undefined}
                  />
                  {fieldErrors.residentName && <small className="field-error" id="err-residentName" role="alert">{fieldErrors.residentName}</small>}
                </div>
                <button type="button" className="primary-button" onClick={addResident}>{l.addButton}</button>
              </div>
              {form.residents.length > 0 ? (
                <div className="resident-list">
                  <strong>{l.residentsTitle} ({form.residents.length})</strong>
                  <ul className="resident-items">
                    {form.residents.map((r, i) => (
                      <li key={i} className="resident-item">
                        <span>{r.fullName}</span>
                        <button type="button" className="secondary-button small" onClick={() => removeResident(i)}>Remover</button>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : (
                <p className="step-desc">{l.residentsEmpty}</p>
              )}
            </div>
          )}

          {step === 3 && (
            <div>
              <p className="step-desc">{l.stepParkingDesc}</p>
              <div className="form-grid">
                <label>
                  <span>{l.numberLabel}</span>
                  <input
                    name="parkingNumber"
                    type="text"
                    value={form.parkingNumber}
                    onChange={(e) => setField('parkingNumber', e.target.value)}
                    placeholder={l.numberPlaceholder}
                    maxLength={LIMITS.parkingNumber.max}
                    aria-invalid={Boolean(fieldErrors.parkingNumber)}
                    aria-describedby={fieldErrors.parkingNumber ? 'err-parkingNumber' : undefined}
                  />
                  {fieldErrors.parkingNumber && <small className="field-error" id="err-parkingNumber" role="alert">{fieldErrors.parkingNumber}</small>}
                </label>
                <label>
                  <span>{l.typeLabel}</span>
                  <select
                    value={form.parkingType}
                    onChange={(e) => setField('parkingType', e.target.value)}
                  >
                    <option value="ASSIGNED">Própria</option>
                    <option value="RENTED">Alugada</option>
                    <option value="EXTRA">Extra</option>
                    <option value="VISITOR">Visitante</option>
                  </select>
                </label>
                <label className="checkbox-field">
                  <input
                    type="checkbox"
                    checked={form.parkingActive}
                    onChange={(e) => setField('parkingActive', e.target.checked)}
                  />
                  <span>{labels.parking.activeLabel}</span>
                </label>
              </div>
            </div>
          )}

          {step === 4 && (
            <div>
              <p className="step-desc">{l.stepVehicleDesc}</p>
              <div className="form-grid">
                <label>
                  <span id="registration-plate-label">{l.plateLabel}</span>
                  <input
                    name="plate"
                    type="text"
                    aria-labelledby="registration-plate-label"
                    value={form.plate}
                    onChange={(e) => setField('plate', e.target.value.toUpperCase())}
                    placeholder={l.platePlaceholder}
                    maxLength={LIMITS.plate.exact}
                    aria-invalid={Boolean(fieldErrors.plate)}
                    aria-describedby={fieldErrors.plate ? 'err-plate' : undefined}
                  />
                  {fieldErrors.plate && <small className="field-error" id="err-plate" role="alert">{fieldErrors.plate}</small>}
                </label>
                <label>
                  <span>{l.brandLabel}</span>
                  <input type="text" value={form.brand} onChange={(e) => setField('brand', e.target.value)} maxLength={LIMITS.brand.max} />
                </label>
                <label>
                  <span>{l.modelLabel}</span>
                  <input type="text" value={form.model} onChange={(e) => setField('model', e.target.value)} maxLength={LIMITS.model.max} />
                </label>
                <label>
                  <span>{l.colorLabel}</span>
                  <input type="text" value={form.color} onChange={(e) => setField('color', e.target.value)} maxLength={LIMITS.color.max} />
                </label>
                <label className="checkbox-field">
                  <input
                    type="checkbox"
                    checked={form.vehicleActive}
                    onChange={(e) => setField('vehicleActive', e.target.checked)}
                  />
                  <span>{l.activeLabel}</span>
                </label>
              </div>
            </div>
          )}
        </div>

        <div className="modal-actions step-actions">
          <div>
            {step > 1 && (
              <button type="button" className="secondary-button" onClick={goPrev}>{l.previousStep}</button>
            )}
          </div>
          <div className="step-actions-right">
            {canSkip && (
              <button type="button" className="secondary-button" onClick={() => setStep((s) => (s < 4 ? (s + 1) as Step : s))}>
                {l.skipStep}
              </button>
            )}
            {step < 4 ? (
              <button type="button" className="primary-button" onClick={goNext}>{l.nextStep}</button>
            ) : (
              <button type="button" className="primary-button" onClick={handleSave} disabled={saving}>
                {saving ? l.creating : l.saveAll}
              </button>
            )}
          </div>
        </div>
      </section>
    </section>
  );
}
