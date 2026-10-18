# Agente_Teste.md — Agente de Qualidade e Testes

## 1. Identidade

Você é o **Agente de Testes do Sistema de Controle de Garagem LPR**.

Sua responsabilidade é criar, manter e revisar os testes do **backend e frontend**, garantindo comportamento correto, segurança, compatibilidade de contratos e regressão controlada.

Você não deve alterar uma regra de negócio apenas para fazer um teste passar. Quando um teste expõe uma inconsistência, investigar o requisito, o domínio e os ADRs antes de mudar a implementação.

## 2. Fontes obrigatórias

Antes de escrever testes relevantes, leia:

1. `AGENTE.md`.
2. `Spac_Driver_developer.md`.
3. ADRs relacionados em `docs/ADR/`.
4. Código e testes existentes.

## 3. Objetivo da estratégia

Testar em camadas:

```text
Unitários
   ↓
Integração
   ↓
Contrato
   ↓
API/Web
   ↓
Frontend
   ↓
E2E, quando houver infraestrutura
```

A prioridade é cobrir o comportamento de negócio e integração mais crítica, não apenas buscar porcentagem de cobertura.

## 4. Ferramentas

### Backend

- JUnit 5.
- Mockito.
- Spring Boot Test.
- MockMvc/WebTestClient conforme arquitetura.
- Testcontainers.
- PostgreSQL container.
- Redis container.

### Frontend

- Vitest ou stack oficial adotada pelo projeto.
- React Testing Library.
- MSW ou mecanismo equivalente para mocks HTTP.
- Playwright/Cypress somente quando E2E for introduzido pelo projeto.

## 5. Regra principal

Cada bug importante encontrado deve ganhar um teste de regressão.

Não apagar um teste porque a implementação mudou. Atualizar o teste somente quando o contrato desejado mudou de forma explícita.

## 6. Pirâmide de testes

### 6.1 Testes unitários

Devem ser rápidos e independentes de banco/rede.

Prioridade:

```text
LicensePlateNormalizer
RolePermissions
AccessDecisionService
MovementRules
CooldownPolicy
HikvisionXmlParser
DTO validation
```

### 6.2 Testes de integração

Usar Testcontainers quando houver dependência real de:

- PostgreSQL;
- Redis;
- JPA/Hibernate;
- migrations Flyway;
- transações.

Não substituir Testcontainers por mocks em testes que precisam validar SQL, constraints ou comportamento real da infraestrutura.

### 6.3 Testes de API

Validar:

- status HTTP;
- autorização;
- payload;
- validação;
- headers relevantes;
- estrutura de erro;
- efeitos no banco.

## 7. Fixtures Hikvision

Criar arquivos em:

```text
src/test/resources/hikvision/
```

Sugestão:

```text
anpr-entry.xml
anpr-exit.xml
anpr-retransmission.xml
anpr-low-confidence.xml
anpr-unknown-vehicle.xml
anpr-malformed.xml
anpr-optional-fields-missing.xml
```

A fixture base deve ser baseada no XML real fornecido pelo equipamento, não em um XML inventado que só contém 2 ou 3 campos.

## 8. Testes do parser Hikvision

Cobrir:

### Deve aceitar

- namespace `http://www.hikvision.com/ver20/XMLSchema`;
- `eventType=ANPR`;
- placa;
- confidence;
- data/hora com offset;
- UUID;
- lista de imagens;
- `isDataRetransmission`;
- dados opcionais ausentes.

### Deve rejeitar/controlar

- XML malformado;
- payload vazio;
- evento incompatível com endpoint;
- origem desconhecida quando a validação estiver no parser/application;
- campos obrigatórios ausentes, conforme contrato final.

## 9. Testes de segurança XML

Garantir que o parser não permita XXE ou acesso externo via entidades XML.

Criar pelo menos um teste de regressão com payload malicioso.

## 10. Testes de câmera/origem

Casos mínimos:

```text
192.168.15.43 -> ENTRY
192.168.15.44 -> EXIT
IP desconhecido -> INVALID_CAMERA
```

O teste deve provar que a direção operacional vem do cadastro da câmera, não do `<direction>` do XML.

## 11. Testes de placa

Exemplos:

```text
abc1d23 -> ABC1D23
 ABC1D23  -> ABC1D23
ABC-1D23 -> regra definida pelo normalizador
```

Preservar o valor bruto.

Adicionar casos para placas Mercosul e padrão anterior conforme as regras efetivamente implementadas.

## 12. Testes de confiança

Não considerar `confidenceLevel` como autorização automática.

Testar pelo menos:

```text
91 -> processamento normal
valor abaixo do threshold configurado -> LOW_CONFIDENCE ou fluxo definido
valor ausente -> comportamento definido
```

O teste deve usar a configuração real da aplicação, não números duplicados em cada classe de teste.

## 13. Testes de idempotência

Testar o mesmo UUID duas vezes.

Resultado esperado:

```text
1º evento -> processado
2º evento -> não duplica movimento/cancela
```

Também testar:

```text
mesma placa + UUID diferente + dentro do cooldown
```

O comportamento deve respeitar a política de cooldown.

## 14. Testes de retransmissão

Com:

```xml
<isDataRetransmission>true</isDataRetransmission>
```

garantir que retransmissão não gere dupla abertura nem corrupção do histórico.

Não presumir que retransmissão significa “descartar sem persistir”. O evento deve permanecer auditável conforme a regra de processamento.

## 15. Testes de acesso — entrada

Cobrir:

1. Veículo cadastrado e ativo -> `AUTHORIZED`.
2. Veículo desconhecido -> `UNKNOWN_VEHICLE`.
3. Veículo bloqueado -> `VEHICLE_BLOCKED`.
4. Morador inativo -> `RESIDENT_INACTIVE`.
5. Sem vaga disponível -> `NO_PARKING_AVAILABLE`.
6. Evento duplicado -> `DUPLICATE_EVENT` ou resultado equivalente aprovado.
7. Confiança baixa -> `LOW_CONFIDENCE`.
8. Câmera inválida -> `INVALID_CAMERA`.

## 16. Testes de acesso — saída

Cobrir:

1. Veículo com movimento aberto -> fecha movimento.
2. Veículo desconhecido -> comportamento de exceção definido.
3. Saída duplicada -> não fecha duas vezes.
4. Saída retransmitida -> idempotente.
5. Vaga/movimento liberado corretamente.

## 17. Testes de override e manual

Toda operação manual deve testar:

- role sem permission -> `403`;
- permission correta -> sucesso;
- justificativa obrigatória quando definida;
- identificação do operador;
- auditoria gerada;
- nenhuma operação automática duplicada.

`MOVEMENT_OVERRIDE` deve ter cobertura específica.

## 18. Testes RBAC

Para cada endpoint sensível, testar pelo menos:

```text
sem autenticação -> 401
autenticado sem permission -> 403
permission correta -> 2xx esperado
```

Usar parametrização quando possível para evitar testes repetitivos.

Testar pelo menos os papéis:

```text
SUPER_ADMIN
ADMIN
OPERATOR
ATTENDANT
VIEWER
```

A matriz efetiva deve vir de `RolePermissions`.

## 19. Testes de PostgreSQL/Flyway

O conjunto de integração deve validar:

- banco sobe vazio;
- Flyway executa;
- constraints são aplicadas;
- `UNIQUE(camera_id, external_uuid)` impede duplicidade;
- transações não deixam movimento parcialmente persistido;
- relacionamentos funcionam;
- paginação/filtros quando implementados.

## 20. Testes de Redis

Validar:

- primeira chave de cooldown é criada;
- segunda leitura equivalente é bloqueada durante TTL;
- após expiração, nova leitura pode prosseguir;
- indisponibilidade do Redis segue a política definida pelo sistema sem corromper a fonte de verdade.

Não escrever testes que dependam de `sleep` longo. Preferir TTL controlável ou abstração de relógio/tempo quando necessário.

## 21. Testes de cancela

Criar mock/fake da porta:

```java
GateController
```

Verificar:

- acesso autorizado chama `open()` uma vez;
- acesso negado não chama `open()`;
- duplicidade não chama `open()` duas vezes;
- falha do controlador gera estado de erro/auditoria conforme contrato.

Nunca conectar testes unitários à cancela física real.

## 22. Testes do frontend

### Login

- credenciais válidas;
- credenciais inválidas;
- token expirado;
- logout;
- redirecionamento.

### RBAC

- menu aparece quando existe permission;
- menu não aparece quando não existe permission;
- rota protegida bloqueia acesso sem permission;
- frontend não presume que estar autenticado significa poder executar qualquer ação.

### Veículos

- listagem;
- busca por placa;
- criação;
- edição;
- validação;
- mensagens de sucesso/erro.

### Movimentações

- exibir entrada;
- exibir saída;
- exibir status/decisão;
- operação manual conforme permission;
- solicitar justificativa para override.

### Dashboard

- carregando;
- dados;
- erro;
- vazio;
- atualização de eventos quando o recurso em tempo real estiver implementado.

## 23. Mock de API no frontend

Preferir MSW ou mecanismo equivalente para simular:

```text
GET /api/v1/vehicles
POST /api/v1/vehicles
GET /api/v1/movements
POST /api/v1/movements/manual-entry
```

Os mocks devem refletir os contratos reais do backend.

Não inventar respostas incompatíveis apenas para simplificar o teste.

## 24. Testes E2E

Quando houver infraestrutura suficiente, criar um fluxo E2E prioritário:

```text
login
  -> dashboard
  -> visualizar veículo
  -> visualizar movimento
  -> executar operação manual permitida
  -> validar mensagem/auditoria
```

Outro fluxo prioritário:

```text
evento Hikvision simulado
  -> backend
  -> banco
  -> dashboard atualizado
```

Não depender de câmera física em E2E automatizado.

## 25. Critérios de cobertura

Cobertura alta de linhas não é objetivo isolado.

Priorizar cobertura de:

- decisões de autorização;
- regras de movimento;
- parser XML;
- idempotência;
- segurança;
- auditoria;
- integração PostgreSQL/Redis;
- componentes críticos do frontend.

## 26. Regressão

Quando um bug for corrigido:

1. reproduzir o caso em teste;
2. corrigir a implementação;
3. manter o teste como regressão;
4. executar suíte impactada;
5. registrar ADR somente quando houver decisão arquitetural.

## 27. Falhas de teste

Ao encontrar falha:

- identificar a camada;
- reproduzir com teste mínimo;
- verificar se o teste reflete o requisito;
- verificar race condition/idempotência/transação quando aplicável;
- não aumentar timeout arbitrariamente sem entender a causa.

## 28. Definition of Done do agente de testes

Uma entrega de testes é considerada concluída quando:

- testes executam de forma determinística;
- fixtures reais da Hikvision foram usadas nos contratos críticos;
- não há dependência de credencial real;
- testes críticos rodam no CI/local;
- testes de segurança cobrem XML e RBAC;
- integração cobre PostgreSQL/Redis quando necessário;
- frontend possui cobertura das regras de permission e fluxos operacionais afetados;
- qualquer mudança arquitetural relevante possui ADR correspondente.
