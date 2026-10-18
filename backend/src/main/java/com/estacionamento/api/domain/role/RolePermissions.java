package com.estacionamento.api.domain.role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RolePermissions {

    private static final Map<RoleCode, Set<Permission>> MAP = buildMap();

    private RolePermissions() {
    }

    public static Set<Permission> of(RoleCode role) {
        if (role == null) {
            return Set.of();
        }
        return MAP.getOrDefault(role, Set.of());
    }

    private static Map<RoleCode, Set<Permission>> buildMap() {
        EnumMap<RoleCode, Set<Permission>> map = new EnumMap<>(RoleCode.class);

        map.put(RoleCode.SUPER_ADMIN,
                Collections.unmodifiableSet(EnumSet.allOf(Permission.class)));

        map.put(RoleCode.ADMIN, immutableSet(
                Permission.DASHBOARD_READ,
                Permission.APARTMENT_READ, Permission.APARTMENT_WRITE,
                Permission.PARKING_READ, Permission.PARKING_WRITE,
                Permission.VEHICLE_READ, Permission.VEHICLE_WRITE,
                Permission.MOVEMENT_READ, Permission.MOVEMENT_MANUAL_WRITE, Permission.MOVEMENT_OVERRIDE,
                Permission.CAMERA_READ, Permission.CAMERA_WRITE, Permission.CAMERA_TEST,
                Permission.GATE_READ, Permission.GATE_WRITE, Permission.GATE_TEST,
                Permission.USER_READ, Permission.USER_WRITE,
                Permission.ROLE_READ,
                Permission.REPORT_READ,
                Permission.AUDIT_READ
        ));

        map.put(RoleCode.OPERATOR, immutableSet(
                Permission.DASHBOARD_READ,
                Permission.APARTMENT_READ,
                Permission.PARKING_READ,
                Permission.VEHICLE_READ, Permission.VEHICLE_WRITE,
                Permission.MOVEMENT_READ, Permission.MOVEMENT_MANUAL_WRITE,
                Permission.CAMERA_READ,
                Permission.GATE_READ,
                Permission.REPORT_READ
        ));

        map.put(RoleCode.ATTENDANT, immutableSet(
                Permission.DASHBOARD_READ,
                Permission.APARTMENT_READ,
                Permission.PARKING_READ,
                Permission.VEHICLE_READ,
                Permission.MOVEMENT_READ, Permission.MOVEMENT_MANUAL_WRITE,
                Permission.CAMERA_READ,
                Permission.GATE_READ
        ));

        map.put(RoleCode.VIEWER, immutableSet(
                Permission.DASHBOARD_READ,
                Permission.APARTMENT_READ,
                Permission.PARKING_READ,
                Permission.VEHICLE_READ,
                Permission.MOVEMENT_READ,
                Permission.CAMERA_READ,
                Permission.GATE_READ,
                Permission.REPORT_READ
        ));

        return Collections.unmodifiableMap(map);
    }

    private static Set<Permission> immutableSet(Permission... permissions) {
        return Collections.unmodifiableSet(EnumSet.of(permissions[0], permissions));
    }
}
