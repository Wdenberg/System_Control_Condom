package com.estacionamento.api.application.audit;

import com.estacionamento.api.domain.audit.AuditLog;
import com.estacionamento.api.domain.audit.AuditLogRepository;
import com.estacionamento.api.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditLog record(User user, String action, String entityType, String entityId,
                           String justification, String details) {
        AuditLog log = new AuditLog(user, action, entityType, entityId, justification, details);
        AuditRequestContext.Context context = AuditRequestContext.current();
        if (context != null) {
            log.enrichRequestContext(context.correlationId(), context.method(), context.path(),
                    context.ipAddress(), context.userAgent());
        }
        return repository.save(log);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public AuditLog recordAttempt(User user, String action, String resource, boolean success, String details) {
        AuditLog log = new AuditLog(user, action, "HTTP_OPERATION", resource, null, details);
        log.outcome(success);
        AuditRequestContext.Context context = AuditRequestContext.current();
        if (context != null) log.enrichRequestContext(context.correlationId(), context.method(), context.path(), context.ipAddress(), context.userAgent());
        return repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(AuditFilter filter, Pageable pageable) {
        Specification<AuditLog> specification = (root, query, cb) -> null;
        if (filter.from() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
        }
        if (filter.to() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), filter.to()));
        }
        if (hasText(filter.username())) {
            String value = likeValue(filter.username());
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("user").get("username")), value));
        }
        if (hasText(filter.action())) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("action")), filter.action().trim().toUpperCase(Locale.ROOT)));
        }
        if (hasText(filter.entityType())) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("entityType")), filter.entityType().trim().toUpperCase(Locale.ROOT)));
        }
        if (hasText(filter.entityId())) {
            String value = likeValue(filter.entityId());
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("entityId")), value));
        }
        if (hasText(filter.ipAddress())) {
            String value = likeValue(filter.ipAddress());
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("ipAddress")), value));
        }
        if (hasText(filter.correlationId())) {
            String value = likeValue(filter.correlationId());
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("correlationId")), value));
        }

        return repository.findAll(specification,
                pageable.getSort().isSorted() ? pageable : org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), org.springframework.data.domain.Sort.by("createdAt").descending()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String likeValue(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
