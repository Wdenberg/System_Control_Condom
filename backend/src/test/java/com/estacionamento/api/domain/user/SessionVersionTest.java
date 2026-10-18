package com.estacionamento.api.domain.user;
import com.estacionamento.api.domain.role.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class SessionVersionTest {
    private User user() { return new User("operator","hash","Operator",new Role(RoleCode.OPERATOR)); }
    @Test void passwordResetInvalidatesAllOlderSessionVersions() { var user=user();user.changePassword("different-hash");assertThat(user.getSessionVersion()).isEqualTo(1); }
    @Test void disablingThenReactivatingCannotRestoreAnOldSession() {var user=user();user.setActive(false);user.setActive(true);assertThat(user.getSessionVersion()).isEqualTo(2);}
    @Test void changedRoleInvalidatesExistingSessions() {var user=user();user.changeRole(new Role(RoleCode.VIEWER));assertThat(user.getSessionVersion()).isEqualTo(1);}
    @Test void unchangedRoleAndStatusDoNotRevokeSessions() {var user=user();user.changeRole(new Role(RoleCode.OPERATOR));user.setActive(true);assertThat(user.getSessionVersion()).isZero();}
}
