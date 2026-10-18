# Runbook Operacional — Garage Control 1.0.0

## Inicialização

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

## Verificação

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
./deploy/scripts/healthcheck.sh
```

## Logs

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f backend
```

## Backup

```bash
./deploy/scripts/backup-postgres.sh
```

## Restore

Execute somente em procedimento controlado:

```bash
./deploy/scripts/restore-postgres.sh backups/arquivo.sql.gz RESTORE
```

## Câmeras

- ENTRY: `192.168.15.43`
- EXIT: `192.168.15.44`

Validar no painel se o último evento de cada câmera aparece e se a direção é consistente.

## Cancela

Até identificar o controlador real, manter:

```env
GATE_ENABLED=false
GATE_DRIVER=SIMULATED
```
