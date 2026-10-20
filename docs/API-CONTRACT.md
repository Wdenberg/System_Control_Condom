# Contrato Frontend ↔ Backend — Release 1.0.0

## Base

Local: `http://localhost:8080/api/v1`

Autenticação: `Authorization: Bearer <JWT>` para APIs de usuário.

Webhook Hikvision: `POST /integrations/hikvision/events`, protegido por segredo distinto de câmera; IP declarado no XML não autentica.

## Auth

| Método | Endpoint | Permission | Frontend |
|---|---|---|---|
| POST | `/auth/login` | público | LoginPage |
| GET | `/auth/me` | autenticado | sessão |

## Dashboard / realtime

| Método | Endpoint | Permission |
|---|---|---|
| GET | `/dashboard/summary` | DASHBOARD_READ |
| GET | `/dashboard/activity` | DASHBOARD_READ |
| GET | `/dashboard/occupancy` | DASHBOARD_READ |
| GET | `/dashboard/cameras` | DASHBOARD_READ |
| GET | `/dashboard/alerts` | DASHBOARD_READ |
| GET | `/realtime/events` | MOVEMENT_READ |

## Cadastros

### Apartments
- `GET /apartments`
- `GET /apartments/{id}`
- `POST /apartments`
- `PUT /apartments/{id}`
- `GET /apartments/{id}/residents`
- `POST /apartments/{id}/residents`
- `PUT /apartments/{id}/residents/{residentId}`

### Parking
- `GET /parking-spaces`
- `GET /parking-spaces/{id}`
- `GET /parking-spaces/apartment/{apartmentId}`
- `POST /parking-spaces`
- `PUT /parking-spaces/{id}`

### Vehicles
- `GET /vehicles`
- `GET /vehicles/{id}`
- `GET /vehicles/plate/{plate}`
- `POST /vehicles`
- `PUT /vehicles/{id}`

## Movements / gates / cameras

- `GET /movements`
- `GET /movements/{id}`
- `POST /movements/manual`
- `POST /movements/{id}/override`
- `GET /gates/status`
- `GET /gates/operations`
- `POST /gates/operations/{id}/retry`
- `POST /gates/test/{direction}`
- `GET /cameras`

## Reports / audit / users

- `GET /reports/access`
- `GET /reports/access/summary`
- `GET /reports/access.csv`
- `GET /reports/occupancy`
- `GET /reports/gates`
- `GET /audit`
- `GET /audit/csv`
- `GET /users`
- `GET /users/{id}`
- `POST /users`
- `PUT /users/{id}`
- `POST /users/{id}/password`
- `GET /roles`

## Regras de contrato

- Datas: ISO-8601 com timezone.
- IDs: numéricos no contrato atual.
- `401`: sessão/token inválido.
- `403`: permission insuficiente.
- `404`: recurso inexistente.
- `409`: conflito de regra/duplicidade.
- Respostas de erro não devem expor stack trace nem segredos.
- O frontend nunca define a autorização final; o backend é a autoridade.

## Ponto de atenção para evolução

A especificação é um snapshot da API atual da release. Ao alterar payloads, primeiro atualizar backend, tipos TypeScript, testes e este documento na mesma mudança.

## Remediação — contratos adicionais e alterações

- POST `/auth/revoke`: Bearer atual, sem corpo, 204; revoga todas as sessões da conta. JWT inclui session_version; tokens antigos exigem login após V9.
- POST `/movements/{id}/override`: requer veículo cadastrado, ausência de reservas concorrentes e ocupação compatível; conflitos retornam 409. O override vincula veículo/vaga, mas não confirma passagem.
- PUT `/parking-spaces/{id}`: alterações são bloqueadas com 422 enquanto houver passagem pendente. Vagas ocupadas ou vinculadas a veículos não podem sofrer alterações estruturais incompatíveis sem reconciliação/desvinculação.
- POST `/registrations`: requer APARTMENT_WRITE, PARKING_WRITE e VEHICLE_WRITE. DTO flat: requestId UUID, apartmentNumber/apartmentActive, residents[{fullName,active}], parkingNumber/parkingType/parkingActive opcionais, plate/brand/model/color/vehicleActive opcionais. Resultado 200: apartmentId, residentIds, parkingSpaceId e vehicleId. Uma transação; retry com mesmo requestId/dados retorna mesmo resultado. Mesmo ID/dados diferentes retorna 409. Não há retry automático do cliente após timeout.
- POST `/movements/{id}/passage-confirmation`: MOVEMENT_OVERRIDE, `{passed:boolean, controllerSettled?:boolean, justification:string(10..1000)}`. `controllerSettled` omitido ou nulo assume `false`; somente `true` declara a verificação do controlador. Apenas observação operacional; não indica sensor. Atualiza ocupação sob locks, audita e retorna MovementResult. PENDING/PROCESSING impede reconciliação.
- MovementResult acrescenta passageState. Autorização não altera mais ocupação confirmada. Estados: AUTHORIZED, GATE_REQUESTED, COMMAND_ACCEPTED, COMPLETED, DENIED, FAILED, UNKNOWN, RECONCILIATION_REQUIRED. OPERATION_PENDING é nova decisão de acesso para reserva já existente.
- GateOperation.status acrescenta PROCESSING e UNKNOWN. UNKNOWN não aceita retry; requer reconciliação. Teste direto de cancela só aceita driver SIMULATED.
- Hikvision retorna `{eventId,status,externalEventId,cameraName,cameraDirection,plate,confidence,occurredAt,pictureCount,movementId,movementStatus,accessDecision}`. Não há objeto movement aninhado.
- Campo active é respeitado em todos os POSTs de cadastro. Atividade e vínculos continuam sujeitos às regras do domínio.
- VIEWER não possui USER_READ e OPERATOR não possui GATE_TEST; matriz central documentada em RBAC_MATRIX.md.

Para operação UNKNOWN, controllerSettled=true é obrigatório após verificação operacional da ausência de comandos pendentes no controlador; a declaração é auditada e não simula verificação por sensor.
