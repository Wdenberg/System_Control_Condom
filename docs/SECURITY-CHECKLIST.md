# Checklist de Segurança — Release 1.0.0

- [ ] `.env` real fora do Git.
- [ ] `JWT_SECRET_BASE64` gerado aleatoriamente.
- [ ] Senha PostgreSQL não é valor de exemplo.
- [ ] Senha do admin inicial não é valor de exemplo.
- [ ] Credenciais Hikvision configuradas somente no ambiente.
- [ ] `HIKVISION_WEBHOOK_TOKEN` definido quando suportado.
- [ ] `HIKVISION_ENFORCE_REMOTE_IP=true` na rede definitiva, quando não houver proxy/NAT que altere a origem.
- [ ] CORS restringido ao host real do frontend.
- [ ] Actuator não exposto publicamente além do necessário.
- [ ] HTTPS ativo fora do localhost.
- [ ] Prometheus/Grafana protegidos e sem publicação desnecessária.
- [ ] Containers sem privilégios desnecessários.
- [ ] Backup criptografado/armazenado com controle de acesso conforme política local.
