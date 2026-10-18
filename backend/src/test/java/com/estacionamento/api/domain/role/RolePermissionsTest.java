package com.estacionamento.api.domain.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class RolePermissionsTest {

    @Test
    void everyRoleShouldHaveAPermissionSet() {
        for (RoleCode role : RoleCode.values()) {
            assertThat(RolePermissions.of(role)).isNotNull();
        }
    }

    @Test
    void superAdminShouldHaveAllPermissions() {
        assertThat(RolePermissions.of(RoleCode.SUPER_ADMIN))
                .containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void adminShouldHaveRoleManagementReadButNotRoleWrite() {
        Set<Permission> permissions = RolePermissions.of(RoleCode.ADMIN);

        assertThat(permissions)
                .contains(Permission.USER_READ, Permission.USER_WRITE, Permission.ROLE_READ)
                .doesNotContain(Permission.ROLE_WRITE);
    }

    @Test
    void attendantShouldNotHaveAdministrativePermissions() {
        Set<Permission> permissions = RolePermissions.of(RoleCode.ATTENDANT);

        assertThat(permissions)
                .contains(
                        Permission.DASHBOARD_READ,
                        Permission.MOVEMENT_MANUAL_WRITE,
                        Permission.CAMERA_READ)
                .doesNotContain(
                        Permission.USER_WRITE,
                        Permission.ROLE_WRITE,
                        Permission.MOVEMENT_OVERRIDE,
                        Permission.CAMERA_WRITE);
    }

    @Test
    void gateTestPermissionShouldNotBeAvailableToViewerOrAttendant() {
        assertThat(RolePermissions.of(RoleCode.VIEWER)).doesNotContain(Permission.GATE_TEST);
        assertThat(RolePermissions.of(RoleCode.ATTENDANT)).doesNotContain(Permission.GATE_TEST);
        assertThat(RolePermissions.of(RoleCode.OPERATOR)).doesNotContain(Permission.GATE_TEST);
        assertThat(RolePermissions.of(RoleCode.VIEWER)).doesNotContain(Permission.USER_READ);
        assertThat(RolePermissions.of(RoleCode.ADMIN)).contains(Permission.GATE_TEST);
    }

    @Test
    void nullRoleShouldReturnEmptyPermissions() {
        assertThat(RolePermissions.of(null)).isEmpty();
    }

    @Test
    void returnedPermissionSetShouldBeImmutable() {
        Set<Permission> permissions = RolePermissions.of(RoleCode.VIEWER);

        assertThatThrownBy(() -> permissions.add(Permission.USER_WRITE))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
