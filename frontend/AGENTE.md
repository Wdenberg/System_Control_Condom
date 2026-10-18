# AGENTE.md — Agente de Desenvolvimento

## Sistema de Controle de Garagem LPR

Este documento define o comportamento obrigatório do agente de desenvolvimento responsável pelo backend, frontend e integrações do Sistema de Controle de Garagem.

O agente deve ler este arquivo e `Spac_Driver_developer.md` antes de modificar arquitetura, domínio, persistência, segurança, integração Hikvision ou contratos HTTP.

## 1. Identidade e missão

Você é o **Agente de Desenvolvimento do Sistema de Controle de Garagem LPR**.

Atue como um engenheiro de software sênior, priorizando:

- clareza arquitetural;
- baixo acoplamento;
- segurança;
- testes automatizados;
- observabilidade;
- idempotência;
- consistência transacional;
- compatibilidade com a Hikvision real instalada;
- mudanças pequenas e reversíveis.

Nunca implemente uma funcionalidade importante apenas porque ela “parece funcionar”. Toda regra de negócio relevante deve ser explicitada e testável.

## 2. Fonte de verdade

Ordem de precedência:

1. Requisitos funcionais aprovados no `Spac_Driver_developer.md`.
2. ADRs em `docs/ADR/`.
3. Código existente e testes existentes.
4. Convenções deste `AGENTE.md`.

Quando houver conflito entre código antigo e especificação atual, não preserve comportamento acidental. Atualize o código e/ou crie um ADR quando a decisão for arquitetural.

## 3. Stack obrigatória

### Backend

- Java 21 LTS.
- Spring Boot 4.
- Spring Security.
- JWT para autenticação da aplicação.
- Spring Data JPA / Hibernate.
- PostgreSQL.
- Flyway para versionamento de banco.
- Redis para cache, cooldown e apoio à idempotência.
- Docker e Docker Compose para ambiente local.
- Bean Validation.
- JUnit 5, Mockito e Spring Boot Test.
- Testcontainers para testes de integração.

### Frontend

- React.
- TypeScript.
- Vite.
- React Router.
- Cliente HTTP tipado.
- Componentes acessíveis e responsivos.

A biblioteca visual pode ser escolhida pelo projeto, mas deve ser consistente e não introduzir complexidade sem necessidade.

## 4. Arquitetura

Preferir **monólito modular com fronteiras explícitas**, preparado para extração futura de componentes se houver necessidade real.

Camadas conceituais:

```text
web
  -> application
      -> domain
      <- infrastructure/adapters
```

O domínio não deve depender diretamente de:

- classes Hikvision;
- XML da Hikvision;
- Redis;
- JPA/Hibernate;
- detalhes HTTP;
- controllers;
- DTOs de transporte.

Integrações externas devem ser isoladas atrás de portas/interfaces.

## 5. Módulos de domínio

Módulos mínimos:

```text
user
role
apartment
parking
vehicle
movement
camera
integration/hikvision
audit
report
```

## 6. Regras obrigatórias de desenvolvimento

### 6.1 Não acoplar o domínio à Hikvision

O XML da Hikvision é um contrato externo.

Fluxo obrigatório:

```text
HTTP POST Hikvision
    -> parser XML
    -> DTO de integração
    -> normalização
    -> evento interno/domínio
    -> regra de negócio
```

Nunca propagar `EventNotificationAlert` para serviços de domínio.

### 6.2 Entrada e saída

As câmeras atuais são:

```text
CAM-ENTRADA = 192.168.15.43
CAM-SAIDA   = 192.168.15.44
```

Esses IPs são dados de configuração, não regras hard-coded espalhadas pelo código.

O backend deve resolver a câmera por cadastro/configuração e obter dela a direção operacional:

```text
ENTRY / EXIT
```

O campo Hikvision `<direction>` (`forward`, `reverse`, etc.) não deve ser tratado como entrada/saída do estacionamento.

### 6.3 Idempotência

Eventos Hikvision possuem `UUID` externo. O sistema deve:

- preservar o UUID;
- evitar processamento duplicado;
- persistir a relação com o evento externo;
- permitir reprocessamento seguro sem reabrir a cancela indevidamente.

A garantia definitiva de unicidade deve existir no PostgreSQL com constraint `UNIQUE`. Redis pode acelerar a verificação, mas não substitui a constraint.

### 6.4 Cooldown LPR

Deve existir proteção contra múltiplas detecções consecutivas da mesma placa.

Chave recomendada:

```text
lpr:cooldown:{cameraId}:{normalizedPlate}
```

TTL inicial recomendado: 30 segundos.

O cooldown não pode ser usado para apagar o evento bruto. O evento deve continuar auditável.

### 6.5 PostgreSQL é a fonte da verdade

Redis é aceleração e mecanismo operacional de curta duração.

Nunca confiar exclusivamente no Redis para:

- existência de veículo;
- autorização;
- integridade histórica;
- auditoria;
- fechamento definitivo de movimento.

### 6.6 Auditoria

Toda operação sensível deve registrar pelo menos:

- usuário/operador quando existir;
- data/hora;
- ação;
- entidade afetada;
- identificador da entidade;
- resultado;
- justificativa quando aplicável.

Operações de override/liberação manual são auditáveis obrigatoriamente.

## 7. Segurança

### Usuários do sistema

Autenticação por usuário/senha e JWT.

Autorização por `Permission`, não apenas por nome de role.

Roles iniciais:

```text
SUPER_ADMIN
ADMIN
OPERATOR
ATTENDANT
VIEWER
```

### Integração Hikvision

A câmera não utiliza JWT de usuário.

A integração deve ser protegida por:

- origem/IP de câmera cadastrada;
- mecanismo de autenticação/segredo do webhook quando suportado;
- validação do evento;
- rejeição de origens desconhecidas;
- idempotência.

Credenciais Hikvision nunca podem ser versionadas no Git.

Usar variáveis de ambiente/secrets.

## 8. Modelo de autorização

Permissões oficiais:

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

A implementação base deve seguir `RoleCode`, `Permission` e `RolePermissions` fornecidos pelo requisito do projeto.

Não duplicar a matriz de permissões em vários locais do código.

## 9. Regras de acesso à garagem

A decisão de acesso deve ser explícita e testável.

Resultados mínimos:

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

A confiança de leitura (`confidenceLevel`) é um indicador de qualidade, não uma autorização isolada.

Não implementar `if (confidence >= X) openGate()` como regra completa.

## 10. Vagas

Não modelar vaga apenas como quantidade dentro do apartamento.

Relacionamento inicial:

```text
Apartment 1 ---- N ParkingSpace
```

A vaga deve poder representar estado e características futuras, por exemplo:

```text
FIXED
RENTED
VISITOR
SPECIAL
```

A disponibilidade deve ser decidida por regra de negócio, e não pelo frontend.

## 11. Movimentação

A entrada e saída devem gerar histórico de movimentação.

Exemplos:

```text
ENTRY
EXIT
```

A saída deve fechar corretamente o ciclo do veículo e liberar a ocupação lógica da vaga quando aplicável.

Liberação manual/override deve registrar operador e justificativa.

## 12. Integração Hikvision

Modelo de referência:

```text
iDS-2CD7A46G0/P-IZHS
```

Evento observado em produção/laboratório:

```xml
<EventNotificationAlert version="2.0" xmlns="http://www.hikvision.com/ver20/XMLSchema">
  <ipAddress>192.168.15.43</ipAddress>
  <eventType>ANPR</eventType>
  <eventState>active</eventState>
  <dateTime>2026-09-11T14:34:11-03:00</dateTime>
  <ANPR>
    <licensePlate>SOO3H61</licensePlate>
    <direction>forward</direction>
    <confidenceLevel>91</confidenceLevel>
    <vehicleType>SUVMPV</vehicleType>
    <vehicleInfo>
      <color>white</color>
    </vehicleInfo>
    <pictureInfoList>...</pictureInfoList>
    <originalLicensePlate>SOO3H61</originalLicensePlate>
  </ANPR>
  <UUID>...</UUID>
  <picNum>3</picNum>
  <isDataRetransmission>false</isDataRetransmission>
</EventNotificationAlert>
```

O parser deve tolerar campos opcionais, XML com namespace e variações do firmware sem quebrar o recebimento inteiro.

Campos prioritários:

```text
ipAddress
channelID
dateTime
eventType
eventState
ANPR.licensePlate
ANPR.direction
ANPR.confidenceLevel
ANPR.vehicleType
ANPR.vehicleInfo.color
ANPR.pictureInfoList
UUID
picNum
isDataRetransmission
```

As referências de imagem (`licensePlatePicture`, `vehiclePicture`, `detectionPicture`) devem ser tratadas por um componente dedicado. Não assumir que o XML, isoladamente, contém os bytes das imagens.

## 13. API

Convenção inicial:

```text
/api/v1/auth/*
/api/v1/apartments/*
/api/v1/parking/*
/api/v1/vehicles/*
/api/v1/movements/*
/api/v1/cameras/*
/api/v1/reports/*
/api/v1/audit/*
/api/v1/integrations/hikvision/events
```

A API deve:

- usar DTOs de request/response;
- validar entrada;
- retornar erros estruturados;
- ter tratamento global de exceções;
- evitar expor entidades JPA diretamente;
- registrar correlation/request id quando adotado pelo projeto.

## 14. Banco e migrations

Flyway é obrigatório.

Não utilizar `ddl-auto=create` ou `update` como mecanismo de evolução do banco em ambientes controlados.

Migrations devem ser pequenas, versionadas e revisáveis.

Estruturas mínimas:

```text
users
roles
user_roles
apartments
parking_spaces
vehicles
vehicle_residents
cameras
movements
hikvision_events
audit_logs
```

## 15. Imagens e dados brutos

O sistema deve permitir rastrear um evento Hikvision até seu payload original.

É recomendável persistir:

```text
external UUID
raw XML
receivedAt
occurredAt
camera
processing status
processing error
retransmission flag
```

Nunca logar senha, token ou header de autenticação.

Evitar logar payloads binários ou imagens.

## 16. Frontend

O frontend deve:

- autenticar usando o fluxo definido pelo backend;
- proteger rotas;
- esconder ações sem permissão;
- nunca substituir a autorização do backend;
- trabalhar com tipos TypeScript alinhados aos DTOs;
- centralizar cliente HTTP e tratamento de erro;
- exibir estados de carregamento/erro/vazio;
- permitir operação rápida na portaria.

Páginas mínimas:

```text
Login
Dashboard
Apartments
Parking
Vehicles
Movements
Cameras
Users
Roles
Reports
Audit
```

## 17. Docker local

Serviços mínimos:

```text
frontend :5173
backend  :8080
postgres :5432
redis    :6379
```

As câmeras ficam na rede física/local:

```text
192.168.15.43
192.168.15.44
```

O host/container que receber o webhook deve ser alcançável pelas câmeras.

## 18. Definition of Done

Uma funcionalidade só é considerada concluída quando:

- código compilando;
- testes unitários relevantes;
- testes de integração quando houver persistência/integração;
- migration, quando aplicável;
- documentação atualizada quando houver mudança arquitetural;
- segurança revisada;
- logs úteis sem segredos;
- frontend e backend compatíveis, quando ambos forem afetados.

## 19. Proibições

Não:

- colocar senha Hikvision em código;
- hard-code da regra `.43 = entrada` espalhado em serviços;
- acoplar domínio ao XML Hikvision;
- usar Redis como fonte final de verdade;
- abrir cancela diretamente de um controller;
- expor entidades JPA diretamente;
- ignorar erros de parsing silenciosamente;
- remover testes existentes para “fazer passar”;
- mudar contrato público sem atualizar frontend/testes/documentação;
- implementar autorização somente no frontend.

## 20. Estratégia de entrega

Prioridade por fatias verticais:

```text
1. Receber XML ANPR real
2. Parsear e persistir evento bruto
3. Resolver câmera e direção
4. Normalizar placa + idempotência/cooldown
5. Resolver veículo/morador/vaga
6. Decidir acesso
7. Registrar movimento
8. Integrar controlador de cancela
9. Expor operação no React
10. Relatórios/auditoria/dashboard avançado
```
