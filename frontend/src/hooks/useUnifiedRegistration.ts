import { useCallback, useRef, useState } from 'react';
import { api, getApiErrorMessage } from '@/services/api';
import { formatProperName, validateApartmentNumber, validateResidentName, validateParkingNumber, validateParkingTypeRequiresApartment, validatePlate } from '@/lib/validation';
import type { FieldErrors } from '@/lib/validation';
type Step = 1 | 2 | 3 | 4;

type ResidentEntry = { fullName: string; active: boolean };

type Form = {
  apartmentNumber: string;
  apartmentActive: boolean;
  residents: ResidentEntry[];
  parkingNumber: string;
  parkingType: string;
  parkingActive: boolean;
  plate: string;
  brand: string;
  model: string;
  color: string;
  vehicleActive: boolean;
};

const emptyForm: Form = {
  apartmentNumber: '',
  apartmentActive: true,
  residents: [],
  parkingNumber: '',
  parkingType: 'ASSIGNED',
  parkingActive: true,
  plate: '',
  brand: '',
  model: '',
  color: '',
  vehicleActive: true,
};

export function useUnifiedRegistration() {
  const lastRequest = useRef<{ signature: string; id: string } | null>(null);
  const inFlight = useRef(false);
  const [step, setStep] = useState<Step>(1);
  const [form, setForm] = useState<Form>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [globalError, setGlobalError] = useState('');
  const [residentNameInput, setResidentNameInput] = useState('');
  const nameInputRef = useRef<HTMLInputElement>(null);

  const setField = <K extends keyof Form>(key: K, value: Form[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setFieldErrors((prev) => {
      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  const clearFieldError = (key: string) => {
    setFieldErrors((prev) => {
      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  const validateStep1 = useCallback((): boolean => {
    const errors: FieldErrors = {};
    const numErr = validateApartmentNumber(form.apartmentNumber);
    if (numErr) errors.apartmentNumber = numErr;
    setFieldErrors(errors);
    if (errors.apartmentNumber) {
      document.querySelector<HTMLInputElement>('[name="apartmentNumber"]')?.focus();
    }
    return Object.keys(errors).length === 0;
  }, [form.apartmentNumber]);

  const validateStep2 = useCallback((): boolean => {
    if (form.residents.length === 0) {
      setFieldErrors({ residentName: 'Adicione pelo menos um morador.' });
      nameInputRef.current?.focus();
      return false;
    }
    setFieldErrors({});
    return true;
  }, [form.residents]);

  const validateStep3 = useCallback((): boolean => {
    if (!form.parkingNumber.trim()) {
      setFieldErrors({});
      return true;
    }
    const errors: FieldErrors = {};
    const numErr = validateParkingNumber(form.parkingNumber);
    if (numErr) errors.parkingNumber = numErr;
    const typeErr = validateParkingTypeRequiresApartment(form.parkingType, form.apartmentNumber);
    if (typeErr) errors.parkingType = typeErr;
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }, [form.parkingNumber, form.parkingType, form.apartmentNumber]);

  const validateStep4 = useCallback((): boolean => {
    if (!form.plate.trim()) {
      setFieldErrors({});
      return true;
    }
    const errors: FieldErrors = {};
    const plateErr = validatePlate(form.plate);
    if (plateErr) errors.plate = plateErr;
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }, [form.plate]);

  const validateCurrent = useCallback((): boolean => {
    switch (step) {
      case 1: return validateStep1();
      case 2: return validateStep2();
      case 3: return validateStep3();
      case 4: return validateStep4();
    }
  }, [step, validateStep1, validateStep2, validateStep3, validateStep4]);

  const goNext = () => {
    if (validateCurrent()) {
      setStep((s) => (s < 4 ? (s + 1) as Step : s));
    }
  };

  const goPrev = () => {
    setFieldErrors({});
    setStep((s) => (s > 1 ? (s - 1) as Step : s));
  };

  const addResident = () => {
    const err = validateResidentName(residentNameInput);
    if (err) {
      setFieldErrors({ residentName: err });
      nameInputRef.current?.focus();
      return;
    }
    setForm((prev) => ({ ...prev, residents: [...prev.residents, { fullName: formatProperName(residentNameInput), active: true }] }));
    setResidentNameInput('');
    setFieldErrors({});
    nameInputRef.current?.focus();
  };

  const removeResident = (index: number) => {
    setForm((prev) => ({ ...prev, residents: prev.residents.filter((_, i) => i !== index) }));
  };

  const handleSave = async () => {
    if (inFlight.current || !validateCurrent()) return;
    inFlight.current = true;
    setSaving(true);
    setGlobalError('');
    try {
      const payload = { ...form, apartmentNumber: form.apartmentNumber.trim(), plate: form.plate.toUpperCase().trim() };
      const signature = JSON.stringify(payload);
      if (lastRequest.current?.signature !== signature) lastRequest.current = { signature, id: crypto.randomUUID() };
      await api.registrations.create({ ...payload, requestId: lastRequest.current.id });
      setSuccess(true);
    } catch (err) {
      setGlobalError(getApiErrorMessage(err));
    } finally {
      inFlight.current = false;
      setSaving(false);
    }
  };

  return { step, setStep, form, fieldErrors, saving, success, globalError, residentNameInput, setResidentNameInput, nameInputRef, setField, clearFieldError, goNext, goPrev, addResident, removeResident, handleSave };
}
