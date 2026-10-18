# ADR-0007 — Integração ANPR Hikvision por HTTP Notification + ISAPI

## Status

Accepted

## Contexto

O sistema recebe eventos ANPR diretamente das câmeras Hikvision iDS-2CD7A46G0/P-IZHS.

No ambiente definido:

- `192.168.15.43` = câmera de entrada
- `192.168.15.44` = câmera de saída
- evento principal = `ANPR`
- payload recebido = `EventNotificationAlert` XML
- `UUID` é usado como identificador externo do evento
- quando `dataType=0`, as imagens podem chegar como partes multipart junto do XML

A documentação oficial da Hikvision define o `EventNotificationAlert` como evento ANPR e documenta HTTP hosts para envio de alarmes. A documentação de integração de estacionamento também descreve recebimento de evento ANPR com imagem e características do veículo.  

## Decisão

O backend terá um webhook único:

```text
POST /api/v1/integrations/hikvision/events
```

O endpoint aceitará:

- `application/xml`
- `text/xml`
- `multipart/form-data`

A câmera é resolvida pelo `ipAddress` informado no evento e cadastrada no PostgreSQL.

O domínio nunca dependerá do XML Hikvision diretamente. O parser converte o XML para um modelo de integração e então para entidades persistidas.

## Idempotência

A identidade principal é:

```text
(source_ip_address, external_event_id)
```

onde `external_event_id` é o `UUID` Hikvision. Se o UUID não vier, é usado SHA-256 de um fingerprint determinístico.

O PostgreSQL mantém uma constraint `UNIQUE` como garantia de consistência.

Redis mantém um cooldown de 30 segundos por câmera e placa:

```text
lpr:cooldown:<camera-ip>:<plate>
```

O Redis é otimização; o PostgreSQL continua sendo a garantia de idempotência.

## Imagens

Quando as imagens chegam no multipart, o XML determina os nomes/tipos e o backend armazena os bytes em filesystem. O PostgreSQL mantém apenas os metadados e o caminho relativo.

A raiz de armazenamento é configurável por `HIKVISION_IMAGE_STORAGE_PATH`.

## Segurança

O endpoint não usa JWT porque é chamado pelo dispositivo.

A proteção do webhook é feita por cadastro da câmera, origem de rede e, opcionalmente, um token configurável. A autenticação Digest de saída para chamadas do backend à câmera será tratada em uma etapa posterior de comunicação ativa com o dispositivo.

## Consequências

### Positivas

- Hikvision fica isolada em `infrastructure/integration`.
- O XML bruto é preservado para auditoria e diagnóstico.
- Eventos retransmitidos e leituras duplicadas não geram processamento repetido.
- Imagens não são armazenadas dentro do PostgreSQL.

### Negativas

- O backend precisa de armazenamento persistente para as imagens.
- A configuração multipart precisa respeitar o limite de tamanho da câmera.
- O contrato exato pode evoluir conforme firmware e configuração do equipamento; eventos reais devem continuar como fixtures de teste.
