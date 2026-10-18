package com.estacionamento.api.application.registration;
import com.estacionamento.api.application.apartment.ApartmentService;
import com.estacionamento.api.application.parking.ParkingSpaceService;
import com.estacionamento.api.application.vehicle.VehicleService;
import com.estacionamento.api.application.audit.AuditService;
import com.estacionamento.api.application.shared.DuplicateResourceException;
import com.estacionamento.api.domain.apartment.Apartment;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.infrastructure.hikvision.Sha256;
import com.estacionamento.api.web.registration.UnifiedRegistrationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
@Service
public class UnifiedRegistrationService {
    public record Result(Long apartmentId, List<Long> residentIds, Long parkingSpaceId, Long vehicleId) {}
    private final ApartmentService apartments;
    private final ParkingSpaceService spaces;
    private final VehicleService vehicles;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AuditService audit;
    public UnifiedRegistrationService(ApartmentService apartments, ParkingSpaceService spaces, VehicleService vehicles, JdbcTemplate jdbc, ObjectMapper mapper, AuditService audit) {
        this.apartments=apartments;this.spaces=spaces;this.vehicles=vehicles;this.jdbc=jdbc;this.mapper=mapper;this.audit=audit;
    }
    @Transactional
    public Result create(UnifiedRegistrationRequest request, User actor) {
        String key=actor.getId()+":"+request.requestId();
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",(rs,n)->1,key);
        String hash=Sha256.hex(mapper.writeValueAsString(request));
        var previous=jdbc.queryForList("SELECT payload_hash,response FROM registration_requests WHERE actor_id=? AND request_id=?",actor.getId(),request.requestId());
        if(!previous.isEmpty()) {
            if(!hash.equals(previous.getFirst().get("payload_hash"))) throw new DuplicateResourceException("Request identifier reused with different registration data");
            return mapper.readValue((String)previous.getFirst().get("response"),Result.class);
        }
        if(request.vehicleActive() && hasText(request.plate()) && (!request.apartmentActive() || request.residents().stream().noneMatch(UnifiedRegistrationRequest.ResidentInput::active)))
            throw new IllegalArgumentException("An active vehicle requires an active apartment and resident");
        // Build relationships while parent is active; publish its requested final state before commit.
        Apartment apartment=apartments.create(request.apartmentNumber(),true);
        Set<Long> residentIds=new LinkedHashSet<>();
        for(var resident:request.residents()) residentIds.add(apartments.createResident(apartment.getId(),resident.fullName(),resident.active()).getId());
        Long spaceId=hasText(request.parkingNumber()) ? spaces.create(request.parkingNumber(),apartment.getId(),request.parkingType(),request.parkingActive()).getId() : null;
        Long vehicleId=hasText(request.plate()) ? vehicles.create(request.plate(),request.brand(),request.model(),request.color(),residentIds,spaceId,request.vehicleActive()).getId() : null;
        apartment.setActive(request.apartmentActive());
        Result result=new Result(apartment.getId(),List.copyOf(residentIds),spaceId,vehicleId);
        jdbc.update("INSERT INTO registration_requests(actor_id,request_id,payload_hash,response) VALUES (?,?,?,?)",actor.getId(),request.requestId(),hash,mapper.writeValueAsString(result));
        audit.record(actor,"UNIFIED_REGISTRATION","APARTMENT",apartment.getId().toString(),null,"Atomic registration completed");
        return result;
    }
    private static boolean hasText(String value) { return value!=null && !value.isBlank(); }
}
