package com.estacionamento.api.application.apartment;

import com.estacionamento.api.application.shared.DuplicateResourceException;
import com.estacionamento.api.application.shared.ResourceNotFoundException;
import com.estacionamento.api.domain.apartment.Apartment;
import com.estacionamento.api.domain.apartment.ApartmentRepository;
import com.estacionamento.api.domain.resident.Resident;
import com.estacionamento.api.domain.resident.ResidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final ResidentRepository residentRepository;

    public ApartmentService(ApartmentRepository apartmentRepository, ResidentRepository residentRepository) {
        this.apartmentRepository = apartmentRepository;
        this.residentRepository = residentRepository;
    }

    @Transactional(readOnly = true)
    public List<Apartment> list() {
        return apartmentRepository.findAllByOrderByNumberAsc();
    }

    @Transactional(readOnly = true)
    public Apartment get(Long id) {
        return apartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + id));
    }

    public Apartment create(String number) { return create(number, true); }

    public Apartment create(String number, boolean active) {
        String normalized = number == null ? null : number.trim().toUpperCase();
        if (normalized != null && apartmentRepository.existsByNumber(normalized)) {
            throw new DuplicateResourceException("Apartment number already exists: " + normalized);
        }
        Apartment apartment = new Apartment(number);
        apartment.setActive(active);
        return apartmentRepository.save(apartment);
    }

    public Apartment update(Long id, String number, boolean active) {
        Apartment apartment = get(id);
        String normalized = number == null ? null : number.trim().toUpperCase();
        if (normalized != null && apartmentRepository.existsByNumberAndIdNot(normalized, id)) {
            throw new DuplicateResourceException("Apartment number already exists: " + normalized);
        }
        apartment.rename(number);
        apartment.setActive(active);
        return apartmentRepository.save(apartment);
    }

    @Transactional(readOnly = true)
    public List<Resident> listResidents(Long apartmentId) {
        get(apartmentId);
        return residentRepository.findAllByApartmentIdOrderByFullNameAsc(apartmentId);
    }

    public Resident createResident(Long apartmentId, String fullName) { return createResident(apartmentId, fullName, true); }

    public Resident createResident(Long apartmentId, String fullName, boolean active) {
        Apartment apartment = get(apartmentId);
        if (!apartment.isActive()) {
            throw new IllegalStateException("Cannot add resident to an inactive apartment");
        }
        Resident resident = new Resident(fullName, apartment);
        resident.setActive(active);
        Resident saved = residentRepository.save(resident);
        return residentRepository.findWithApartmentById(saved.getId()).orElse(saved);
    }

    public Resident updateResident(Long apartmentId, Long residentId, String fullName, boolean active) {
        Apartment apartment = get(apartmentId);
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found: " + residentId));
        if (!resident.getApartment().getId().equals(apartment.getId())) {
            throw new ResourceNotFoundException("Resident does not belong to apartment: " + apartment.getNumber());
        }
        resident.rename(fullName);
        resident.setActive(active);
        residentRepository.save(resident);
        return residentRepository.findWithApartmentById(resident.getId()).orElse(resident);
    }
}
