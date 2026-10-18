# Checklist de Instalação / Homologação

## Host
- [ ] Docker funcional.
- [ ] Disco com espaço suficiente para PostgreSQL, Redis e imagens ANPR.
- [ ] Clock/timezone corretos.
- [ ] Backup externo configurado.

## Rede
- [ ] Backend alcança `192.168.15.43`.
- [ ] Backend alcança `192.168.15.44`.
- [ ] Câmera ENTRY aponta webhook para backend.
- [ ] Câmera EXIT aponta webhook para backend.
- [ ] Firewall limita serviços internos.

## Hikvision
- [ ] ANPR habilitado.
- [ ] Evento `ANPR` habilitado.
- [ ] HTTP Notification configurado.
- [ ] Imagens habilitadas conforme necessidade.
- [ ] Credenciais válidas testadas.
- [ ] UUID registrado no backend.

## Aplicação
- [ ] Login funciona.
- [ ] RBAC validado.
- [ ] Apartamento cadastrado.
- [ ] Morador cadastrado.
- [ ] Vaga cadastrada.
- [ ] Veículo cadastrado.
- [ ] Entrada automática testada.
- [ ] Saída automática testada.
- [ ] Rejeição de placa desconhecida testada.
- [ ] Override auditado.
- [ ] Relatórios consultados.
- [ ] Auditoria consultada.
- [ ] Dashboard e SSE funcionando.

## Cancela
- [ ] Controlador físico identificado.
- [ ] Driver escolhido.
- [ ] Teste em bancada.
- [ ] Teste de abertura de entrada.
- [ ] Teste de abertura de saída.
- [ ] Falha/timeout testado.
