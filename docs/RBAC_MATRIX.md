# Matriz RBAC

Gerada de `RolePermissions.java`; não editar como segunda fonte de regras.

| Permissão | SUPER_ADMIN | ADMIN | OPERATOR | ATTENDANT | VIEWER |
|---|---|---|---|---|---|
| DASHBOARD_READ | Sim | Sim | Sim | Sim | Sim |
| APARTMENT_READ | Sim | Sim | Sim | Sim | Sim |
| APARTMENT_WRITE | Sim | Sim | — | — | — |
| PARKING_READ | Sim | Sim | Sim | Sim | Sim |
| PARKING_WRITE | Sim | Sim | — | — | — |
| VEHICLE_READ | Sim | Sim | Sim | Sim | Sim |
| VEHICLE_WRITE | Sim | Sim | Sim | — | — |
| MOVEMENT_READ | Sim | Sim | Sim | Sim | Sim |
| MOVEMENT_MANUAL_WRITE | Sim | Sim | Sim | Sim | — |
| MOVEMENT_OVERRIDE | Sim | Sim | — | — | — |
| CAMERA_READ | Sim | Sim | Sim | Sim | Sim |
| CAMERA_WRITE | Sim | Sim | — | — | — |
| CAMERA_TEST | Sim | Sim | — | — | — |
| GATE_READ | Sim | Sim | Sim | Sim | Sim |
| GATE_WRITE | Sim | Sim | — | — | — |
| GATE_TEST | Sim | Sim | — | — | — |
| USER_READ | Sim | Sim | — | — | — |
| USER_WRITE | Sim | Sim | — | — | — |
| ROLE_READ | Sim | Sim | — | — | — |
| ROLE_WRITE | Sim | — | — | — | — |
| REPORT_READ | Sim | Sim | Sim | — | Sim |
| AUDIT_READ | Sim | Sim | — | — | — |

Decisão de menor privilégio: VIEWER não lê contas administrativas; OPERATOR não testa cancela. Leitura do estado da cancela preservada para a operação existente. SUPER_ADMIN continua protegido por verificações adicionais de alvo, conta própria e último administrador ativo.
