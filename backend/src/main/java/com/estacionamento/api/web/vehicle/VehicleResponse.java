package com.estacionamento.api.web.vehicle;

import com.estacionamento.api.domain.resident.Resident;
import com.estacionamento.api.domain.vehicle.Vehicle;

import java.util.Comparator;
import java.util.List;

public record VehicleResponse(
        Long id,
        String plate,
        String brand,
        String model,
        String color,
        Long parkingSpaceId,
        String parkingSpaceNumber,
        Long apartmentId,
        String apartmentNumber,
        List<ResidentSummary> residents,
        boolean active
) {
    public static VehicleResponse from(Vehicle vehicle) {
        Resident apartmentResident = vehicle.getResidents().stream().min(Comparator.comparing(Resident::getId)).orElseThrow();
        return new VehicleResponse(
                vehicle.getId(), vehicle.getPlate(), vehicle.getBrand(), vehicle.getModel(), vehicle.getColor(),
                vehicle.getParkingSpace() == null ? null : vehicle.getParkingSpace().getId(),
                vehicle.getParkingSpace() == null ? null : vehicle.getParkingSpace().getNumber(),
                apartmentResident.getApartment().getId(), apartmentResident.getApartment().getNumber(),
                vehicle.getResidents().stream().sorted(Comparator.comparing(Resident::getFullName)).map(ResidentSummary::from).toList(),
                vehicle.isActive()
        );
    }

    public record ResidentSummary(Long id, String fullName) {
        static ResidentSummary from(Resident resident) {
            return new ResidentSummary(resident.getId(), resident.getFullName());
        }
    }
}
