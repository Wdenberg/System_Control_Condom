# ADR-0008 — Modelagem de apartamentos, moradores, vagas e veículos

## Status

Accepted

## Contexto

A leitura ANPR da Hikvision fornece uma placa. A aplicação precisa transformá-la em uma identidade operacional: veículo, morador, apartamento e, quando aplicável, uma vaga.

## Decisão

O domínio será separado em:

- `Apartment`: unidade residencial.
- `Resident`: pessoa vinculada a um apartamento.
- `ParkingSpace`: vaga física, opcionalmente vinculada a um apartamento.
- `Vehicle`: placa e dados do veículo.
- `vehicle_residents`: relação entre veículo e moradores.
- `Vehicle.parkingSpace`: eventual vaga primária do veículo.

Um veículo pode ter vários moradores, mas todos devem pertencer ao mesmo apartamento na V1. Uma vaga `ASSIGNED` obrigatoriamente pertence a um apartamento. Vagas `RENTED`, `EXTRA` e `VISITOR` permitem modelagem de cenários futuros.

## Regras

1. A placa é normalizada para letras/números em maiúsculo e deve possuir 7 caracteres.
2. A placa é única no PostgreSQL.
3. Um veículo ativo pode ocupar no máximo uma vaga.
4. Uma vaga pode estar vinculada a no máximo um veículo ativo.
5. Uma vaga atribuída permanentemente a veículo deve pertencer ao mesmo apartamento dos moradores do veículo. Vagas `VISITOR` não podem ser atribuídas permanentemente a veículos.
6. Moradores inativos não podem ser associados a novos veículos.
7. Estado `OCCUPIED` será atualizado pelo domínio de movimentações na Fase 5; Fase 4 apenas cadastra a vaga.
8. Exclusão física não será usada na V1 para apartamentos, moradores, vagas ou veículos; usamos `active`.

## Autorização

Os endpoints usam as permissões existentes:

- `APARTMENT_READ` / `APARTMENT_WRITE`
- `PARKING_READ` / `PARKING_WRITE`
- `VEHICLE_READ` / `VEHICLE_WRITE`

## Consequências

A modelagem permite que uma placa seja resolvida até um apartamento sem acoplar o domínio à Hikvision. A Fase 5 poderá aplicar as regras de acesso e ocupação sem alterar o modelo básico.
