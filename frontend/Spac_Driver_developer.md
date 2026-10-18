# SPAC_DRIVER_DEVELOPER.md — Especificação Técnica do Sistema

## 1. Objetivo

Construir um sistema local de controle de garagem baseado em reconhecimento automático de placas (LPR/ANPR), usando duas câmeras Hikvision e um backend Java/Spring Boot.

O sistema deverá controlar e auditar entradas e saídas, relacionar veículos a moradores/apartamentos/vagas, permitir operação manual conforme permissões e fornecer uma interface web para portaria e administração.

## 2. Ambiente físico conhecido

### Câmeras

| Identificação | IP | Função | Modelo |
|---|---|---|---|
| CAM-ENTRADA | 192.168.15.43 | Entrada | Hikvision iDS-2CD7A46G0/P-IZHS |
| CAM-SAIDA | 192.168.15.44 | Saída | Hikvision iDS-2CD7A46G0/P-IZHS |

As duas câmeras possuem autenticação configurada. A credencial é compartilhada entre elas no ambiente atual.

**Não armazenar a credencial neste arquivo nem no Git.** A aplicação deve recebê-la por variável de ambiente/secret quando precisar consultar a câmera.

## 3. Princípios

1. Hikvision é uma integração externa.
2. PostgreSQL é fonte da verdade.
3. Redis é cache/cooldown/idempotência auxiliar.
4. Regras de garagem pertencem ao backend.
5. Frontend não é autoridade de segurança.
6. Todo acesso automático ou manual deve ser auditável.
7. Eventos externos devem ser processados de forma idempotente.
8. Operações críticas devem ser testáveis sem depender da câmera física.

## 4. Stack

### Backend

- Java 21 LTS.
- Spring Boot 4.
- Spring Security.
- JWT.
- PostgreSQL 16 ou versão compatível aprovada pelo projeto.
- Flyway.
- Redis.
- JPA/Hibernate.
- Bean Validation.
- JUnit 5.
- Mockito.
- Spring Boot Test.
- Testcontainers.
- Docker.

### Frontend

- React.
- TypeScript.
- Vite.
- React Router.
- Cliente HTTP tipado.

## 5. Arquitetura alvo

```text
+---------------------+
| React + TypeScript   |
| localhost:5173       |
+----------+-----------+
           |
           | REST/JWT
           v
+-------------------------------+
| Spring Boot 4 / Java 21       |
|                               |
| Web / Application / Domain    |
| Infrastructure / Security     |
+------+------------------+-----+
       |                  |
       v                  v
+-------------+      +-----------+
| PostgreSQL  |      | Redis     |
| source truth|      | cache/etc |
+-------------+      +-----------+
       ^
       |
       | ISAPI/HTTP
       |
+------+----------------------------+
| Hikvision ANPR                    |
| .43 ENTRY         .44 EXIT        |
+-----------------------------------+
```

## 6. Fronteiras de módulo

### 6.1 Auth/User/Role

Responsável por:

- login;
- senha;
- JWT;
- usuários;
- roles;
- permissões;
- controle de acesso.

### 6.2 Apartment

Responsável pelo cadastro de apartamentos/unidades.

### 6.3 Parking

Responsável por vagas, vínculos e estados de ocupação lógica.

### 6.4 Vehicle

Responsável pelos veículos e relacionamento com moradores.

### 6.5 Camera

Responsável pelo cadastro e saúde/configuração lógica das câmeras.

### 6.6 Movement

Responsável por entradas, saídas, decisões e histórico.

### 6.7 Hikvision Integration

Responsável exclusivamente por:

- receber eventos;
- parsear XML;
- normalizar payload externo;
- resolver imagens;
- fazer chamadas ISAPI quando necessário.

### 6.8 Audit

Responsável por operações administrativas/manuais e alterações sensíveis.

## 7. Modelo de domínio inicial

### Apartment

```text
id
code/number
name/label (se necessário)
active
createdAt
updatedAt
```

### ParkingSpace

```text
id
number
apartmentId
kind
status
active
createdAt
updatedAt
```

`kind` pode iniciar com:

```text
FIXED
RENTED
VISITOR
SPECIAL
```

### Vehicle

```text
id
plate
normalizedPlate
brand
model
color
vehicleType
status
createdAt
updatedAt
```

### VehicleResident

```text
vehicleId
residentId/user-or-resident-entity-id
relationship
active
```

A modelagem do residente deve ser separada da conta de usuário do sistema quando o domínio exigir isso.

### Camera

```text
id
name
ipAddress
port
model
vendor
location/direction
channelId
enabled
secret/reference
createdAt
updatedAt
```

Direção operacional:

```text
ENTRY
EXIT
```

### Movement

```text
id
vehicleId
cameraId
parkingSpaceId
plateSnapshot
direction
occurredAt
confidence
status
accessDecision
authorizationType
operatorId
createdAt
updatedAt
```

### HikvisionEvent

```text
id
cameraId
externalUuid
receivedAt
occurredAt
eventType
eventState
rawXml
isRetransmission
processingStatus
processingError
createdAt
```

Constraint obrigatória:

```text
UNIQUE(camera_id, external_uuid)
```

### AuditLog

```text
id
actorUserId
action
entityType
entityId
oldValue/newValue or metadata
justification
createdAt
sourceIp (quando apropriado)
```

## 8. RBAC

### Roles

```text
SUPER_ADMIN
ADMIN
OPERATOR
ATTENDANT
VIEWER
```

### Permissions

```text
DASHBOARD_READ
APARTMENT_READ
APARTMENT_WRITE
PARKING_READ
PARKING_WRITE
VEHICLE_READ
VEHICLE_WRITE
MOVEMENT_READ
MOVEMENT_MANUAL_WRITE
MOVEMENT_OVERRIDE
CAMERA_READ
CAMERA_WRITE
CAMERA_TEST
USER_READ
USER_WRITE
ROLE_READ
ROLE_WRITE
REPORT_READ
AUDIT_READ
```

### Matriz padrão

A matriz oficial da V1 é a seguinte e deve ser tratada como fonte de verdade para os testes e a implementação RBAC:

```java
MAP.put(RoleCode.SUPER_ADMIN, EnumSet.allOf(Permission.class));

MAP.put(RoleCode.ADMIN, EnumSet.of(
        Permission.DASHBOARD_READ,
        Permission.APARTMENT_READ, Permission.APARTMENT_WRITE,
        Permission.PARKING_READ, Permission.PARKING_WRITE,
        Permission.VEHICLE_READ, Permission.VEHICLE_WRITE,
        Permission.MOVEMENT_READ, Permission.MOVEMENT_MANUAL_WRITE, Permission.MOVEMENT_OVERRIDE,
        Permission.CAMERA_READ, Permission.CAMERA_WRITE, Permission.CAMERA_TEST,
        Permission.USER_READ, Permission.USER_WRITE,
        Permission.ROLE_READ,
        Permission.REPORT_READ,
        Permission.AUDIT_READ
);

MAP.put(RoleCode.OPERATOR, EnumSet.of(
        Permission.DASHBOARD_READ,
        Permission.APARTMENT_READ,
        Permission.PARKING_READ,
        Permission.VEHICLE_READ, Permission.VEHICLE_WRITE,
        Permission.MOVEMENT_READ, Permission.MOVEMENT_MANUAL_WRITE,
        Permission.CAMERA_READ,
        Permission.REPORT_READ
));

MAP.put(RoleCode.ATTENDANT, EnumSet.of(
        Permission.DASHBOARD_READ,
        Permission.APARTMENT_READ,
        Permission.PARKING_READ,
        Permission.VEHICLE_READ,
        Permission.MOVEMENT_READ, Permission.MOVEMENT_MANUAL_WRITE,
        Permission.CAMERA_READ
));

MAP.put(RoleCode.VIEWER, EnumSet.of(
        Permission.DASHBOARD_READ,
        Permission.APARTMENT_READ,
        Permission.PARKING_READ,
        Permission.VEHICLE_READ,
        Permission.MOVEMENT_READ,
        Permission.CAMERA_READ,
        Permission.REPORT_READ
));
```

A classe base aprovada é:

```java
RolePermissions.of(role)
```

Não duplicar a matriz em outro arquivo sem necessidade.

## 9. Autenticação

Endpoint inicial:

```http
POST /api/v1/auth/login
```

Resposta deve conter token e informações mínimas do usuário/role conforme contrato aprovado.

JWT deve ter expiração configurada.

Credenciais e secrets devem vir de ambiente.

## 10. Autorização

O backend deve aplicar autorização usando `Permission`/authority.

Exemplos conceituais:

```java
@PreAuthorize("hasAuthority('VEHICLE_READ')")
```

ou um componente central de autorização.

O frontend pode ocultar menus/ações conforme permissões, mas a proteção final deve estar no backend.

## 11. Evento Hikvision — contrato observado

O equipamento atualmente envia eventos no formato `EventNotificationAlert` XML, namespace Hikvision `ver20`, com `eventType=ANPR`.

Exemplo resumido real:

```xml
<EventNotificationAlert version="2.0" xmlns="http://www.hikvision.com/ver20/XMLSchema">
  <ipAddress>192.168.15.43</ipAddress>
  <ipv6Address>::ffff:192.168.15.43</ipv6Address>
  <portNo>80</portNo>
  <protocol>HTTP</protocol>
  <macAddress>40:ac:bf:9d:36:8d</macAddress>
  <channelID>1</channelID>
  <dateTime>2026-09-11T14:34:11-03:00</dateTime>
  <activePostCount>1</activePostCount>
  <eventType>ANPR</eventType>
  <eventState>active</eventState>
  <eventDescription>ANPR</eventDescription>
  <channelName>Camera 01</channelName>
  <ANPR>
    <country>220</country>
    <licensePlate>SOO3H61</licensePlate>
    <line>1</line>
    <direction>forward</direction>
    <confidenceLevel>91</confidenceLevel>
    <plateType>unknown</plateType>
    <plateColor>unknown</plateColor>
    <licenseBright>130</licenseBright>
    <dangmark>no</dangmark>
    <twoWheelVehicle>no</twoWheelVehicle>
    <threeWheelVehicle>no</threeWheelVehicle>
    <plateCharBelieve>99,56,96,99,99,99,99</plateCharBelieve>
    <vehicleType>SUVMPV</vehicleType>
    <detectDir>8</detectDir>
    <detectType>0</detectType>
    <alarmDataType>0</alarmDataType>
    <vehicleInfo>
      <index>2018</index>
      <colorDepth>2</colorDepth>
      <color>white</color>
      <length>0</length>
      <vehicleLogoRecog>1123</vehicleLogoRecog>
      <vehileSubLogoRecog>0</vehileSubLogoRecog>
      <vehileModel>0</vehileModel>
    </vehicleInfo>
    <pictureInfoList>
      <pictureInfo>
        <fileName>licensePlatePicture.jpg</fileName>
        <type>licensePlatePicture</type>
        <dataType>0</dataType>
        <picRecogMode>0</picRecogMode>
        <absTime>20260911143411364</absTime>
        <pId>...</pId>
      </pictureInfo>
      <pictureInfo>
        <fileName>vehiclePicture.jpg</fileName>
        <type>vehiclePicture</type>
        <dataType>0</dataType>
        <picRecogMode>0</picRecogMode>
        <absTime>20260911143411364</absTime>
        <pId>...</pId>
      </pictureInfo>
      <pictureInfo>
        <fileName>detectionPicture.jpg</fileName>
        <type>detectionPicture</type>
        <dataType>0</dataType>
        <picRecogMode>0</picRecogMode>
        <absTime>20260911143411364</absTime>
        <pId>...</pId>
      </pictureInfo>
    </pictureInfoList>
    <originalLicensePlate>SOO3H61</originalLicensePlate>
    <CRIndex>220</CRIndex>
    <vehicleListName>blockList</vehicleListName>
    <plateSize>0</plateSize>
  </ANPR>
  <UUID>...</UUID>
  <picNum>3</picNum>
  <monitoringSiteID></monitoringSiteID>
  <isDataRetransmission>false</isDataRetransmission>
</EventNotificationAlert>
```

Observação: o exemplo acima foi reduzido em alguns identificadores longos. O parser real deve aceitar o XML completo recebido.

## 12. Endpoint de integração Hikvision

Endpoint inicial:

```http
POST /api/v1/integrations/hikvision/events
Content-Type: application/xml
```

O controller deve:

1. aceitar o XML;
2. validar limite de tamanho do payload;
3. identificar origem;
4. parsear XML de forma segura;
5. rejeitar XML malformado sem derrubar a aplicação;
6. persistir evento bruto;
7. responder rapidamente à câmera;
8. processar de forma idempotente;
9. aplicar regra de negócio fora do controller.

O controller não deve abrir a cancela diretamente.

## 13. Segurança do parser XML

O parser deve ser configurado para impedir XXE e resolução de entidades externas.

Não permitir acesso arbitrário a recursos externos durante o parsing.

Validar:

- tamanho máximo do payload;
- namespace esperado quando apropriado;
- `eventType=ANPR` quando for endpoint ANPR;
- câmera conhecida;
- UUID presente, quando exigido pelo fluxo;
- placa válida após normalização, quando exigida.

## 14. Normalização de placa

A aplicação deve manter:

```text
plateRaw
plateNormalized
```

Normalização inicial sugerida:

- trim;
- uppercase;
- remover espaços e caracteres de separação indevidos;
- validar formato de placa conforme a regra adotada pelo domínio;
- nunca alterar o valor bruto do evento.

O formato brasileiro deve ser suportado para Mercosul e padrão anterior, sem transformar a normalização em validação rígida que impeça placas reais retornadas pelo equipamento.

## 15. Fluxo de entrada

```text
CAM-ENTRADA .43
        |
        v
Evento ANPR
        |
        v
Resolver câmera = ENTRY
        |
        v
Normalizar placa
        |
        v
Idempotência + cooldown
        |
        v
Persistir evento
        |
        v
Buscar veículo
        |
        +---- não existe -> UNKNOWN_VEHICLE
        |
        v
Validar vínculo/estado
        |
        +---- bloqueado -> VEHICLE_BLOCKED
        |
        v
Resolver apartamento/vaga
        |
        +---- indisponível -> NO_PARKING_AVAILABLE
        |
        v
AUTHORIZED
        |
        v
GateController.open()
        |
        v
Registrar Movement + auditoria
```

## 16. Fluxo de saída

```text
CAM-SAIDA .44
        |
        v
Evento ANPR
        |
        v
Resolver câmera = EXIT
        |
        v
Normalizar placa
        |
        v
Idempotência + cooldown
        |
        v
Localizar movimento aberto/veículo dentro
        |
        v
Fechar movimento
        |
        v
Liberar ocupação lógica
        |
        v
GateController.open()
        |
        v
Auditar
```

Casos anômalos de saída devem ser registrados sem destruir o histórico.

## 17. Decisão de acesso

Implementar serviço dedicado, por exemplo:

```java
AccessDecisionService
```

Entrada:

```java
LicensePlateEvent
```

Saída:

```java
AccessDecision
```

Enum inicial:

```text
AUTHORIZED
UNKNOWN_VEHICLE
VEHICLE_BLOCKED
RESIDENT_INACTIVE
NO_PARKING_AVAILABLE
DUPLICATE_EVENT
LOW_CONFIDENCE
INVALID_CAMERA
```

## 18. Confiança

O campo:

```xml
<confidenceLevel>91</confidenceLevel>
```

deve ser armazenado.

Também preservar `plateCharBelieve` quando disponível.

Os thresholds devem ser configuráveis e testáveis. Não assumir que um único valor global de confiança é uma regra suficiente para liberar acesso.

## 19. Imagens

O evento informa referências como:

```text
licensePlatePicture.jpg
vehiclePicture.jpg
detectionPicture.jpg
```

com `pId`.

A implementação deve encapsular a estratégia de obtenção das imagens em:

```text
HikvisionImageHandler
```

Não assumir automaticamente que todos os eventos chegam com multipart/binary. Primeiro confirmar o `Content-Type` e o comportamento da câmera no ambiente real.

## 20. Câmera como entidade configurável

Não hard-code IPs nos serviços.

A associação é feita por cadastro:

```text
CAM-ENTRADA -> 192.168.15.43 -> ENTRY
CAM-SAIDA   -> 192.168.15.44 -> EXIT
```

Isso permite mudança de IP sem recompilar o domínio.

## 21. Gate Controller

Criar abstração:

```java
public interface GateController {
    GateOperationResult open(GateCommand command);
}
```

O domínio chama uma porta, não um fabricante específico.

Implementação Hikvision/relay/ControlID/ESP32 poderá ser trocada posteriormente sem mudar as regras de acesso.

Toda operação de cancela deve ser registrada.

## 22. Redis

Usos iniciais:

### Cooldown

```text
lpr:cooldown:{cameraId}:{normalizedPlate}
```

TTL inicial: 30 segundos.

### Idempotência auxiliar

```text
lpr:event:{uuid}
```

TTL conforme a estratégia adotada.

### Cache de leitura

Somente para dados com política clara de invalidação.

Não armazenar JWT ou segredo de câmera em chave de cache sem necessidade.

## 23. Flyway

Migrations devem ser incrementais.

Exemplo:

```text
V1__create_roles.sql
V2__create_users.sql
V3__create_apartments.sql
V4__create_parking_spaces.sql
V5__create_vehicles.sql
V6__create_cameras.sql
V7__create_movements.sql
V8__create_hikvision_events.sql
V9__create_audit_logs.sql
```

A ordem pode ser ajustada conforme a evolução real do modelo.

## 24. API REST — contrato inicial

### Auth

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh   (quando refresh token for adotado)
```

### Apartments

```text
GET    /api/v1/apartments
GET    /api/v1/apartments/{id}
POST   /api/v1/apartments
PUT    /api/v1/apartments/{id}
PATCH  /api/v1/apartments/{id}/status
```

### Parking

```text
GET    /api/v1/parking/spaces
GET    /api/v1/parking/spaces/{id}
POST   /api/v1/parking/spaces
PUT    /api/v1/parking/spaces/{id}
```

### Vehicles

```text
GET    /api/v1/vehicles
GET    /api/v1/vehicles/{id}
GET    /api/v1/vehicles/by-plate/{plate}
POST   /api/v1/vehicles
PUT    /api/v1/vehicles/{id}
PATCH  /api/v1/vehicles/{id}/status
```

### Movements

```text
GET  /api/v1/movements
GET  /api/v1/movements/{id}
POST /api/v1/movements/manual-entry
POST /api/v1/movements/manual-exit
POST /api/v1/movements/{id}/override
```

### Cameras

```text
GET  /api/v1/cameras
GET  /api/v1/cameras/{id}
POST /api/v1/cameras
PUT  /api/v1/cameras/{id}
POST /api/v1/cameras/{id}/test
```

### Reports/Audit

```text
GET /api/v1/reports/accesses
GET /api/v1/reports/occupancy
GET /api/v1/audit
```

Os endpoints são contratos iniciais; filtros, paginação e respostas devem ser especificados antes de estabilização pública.

## 25. Testes obrigatórios

### Backend unitário

- `RolePermissionsTest`;
- `LicensePlateNormalizerTest`;
- `HikvisionXmlParserTest`;
- `AccessDecisionServiceTest`;
- `MovementServiceTest`;
- `CooldownServiceTest`;
- casos de duplicidade;
- casos de câmera inválida;
- casos de confidence baixa;
- regras de entrada e saída.

### Backend integração

Usar Testcontainers para PostgreSQL e Redis.

### Frontend

Testar:

- login;
- guards de rota;
- renderização por permission;
- formulários;
- integração com API via mocks;
- dashboard;
- operação de entrada/saída;
- mensagens de erro;
- estados loading/empty.

## 26. Docker Compose local

```text
frontend  -> 5173
backend   -> 8080
postgres  -> 5432
redis     -> 6379
```

A infraestrutura deve permitir que as câmeras façam HTTP para o backend local. Se o backend estiver dentro de container, o caminho de rede usado pela câmera deve ser comprovado no ambiente, não presumido.

## 27. Observabilidade

Logar no mínimo:

```text
correlationId
cameraId/cameraIp
externalUuid
plate (com política de privacidade adequada)
eventType
processingStatus
accessDecision
latência
```

Não logar:

- senha;
- JWT completo;
- segredo de webhook;
- binários de imagem.

## 28. Critérios de aceite da V1

A V1 estará funcional quando:

1. A câmera `.43` conseguir enviar evento ANPR para o backend.
2. A câmera `.44` conseguir enviar evento ANPR para o backend.
3. O backend distinguir corretamente ENTRY e EXIT por câmera cadastrada.
4. O XML real for parseado sem dependência do hardware em testes.
5. UUID impedir duplicidade lógica.
6. Cooldown impedir abertura repetitiva para leituras consecutivas.
7. Veículo cadastrado puder ser resolvido por placa normalizada.
8. Entrada autorizada gerar movimento e comando de cancela.
9. Saída autorizada fechar o movimento.
10. Operação manual exigir permission adequada e auditoria.
11. Frontend permitir operação conforme a role/permission.
12. Migrations forem executáveis do zero.
13. Testes automatizados cobrirem os fluxos críticos.
