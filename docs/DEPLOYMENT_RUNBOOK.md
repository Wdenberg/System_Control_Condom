# Implantação e operação

Suporte: UMA instância do backend. container_name fixo e broadcaster local não oferecem escalabilidade horizontal. Claims no PostgreSQL protegem contra workers concorrentes, mas isso não transforma SSE em barramento distribuído.

1. Rever RELEASE_READINESS.md; não promover com validações críticas pendentes.
2. Provisionar .env.prod a partir do exemplo, secrets reais e distintos, PostgreSQL, CORS e certificados. Não versionar secrets.
3. Manter GATE_ENABLED=false, GATE_HARDWARE_APPROVED=false e GATE_DRIVER=SIMULATED até comissionamento autorizado. Falta de aprovação impede startup de driver físico habilitado.
4. Verificar se 172.30.0.0/24 e 172.31.0.0/24 não conflitam com a rede do host. Se alterar sub-redes, alterar também IP confiável no Nginx e SECURITY_TRUSTED_PROXIES. Docker não publica portas de PostgreSQL, Redis ou management.
5. Nginx roda sem root nas portas internas 8080/8443. Dar ao UID/GID 101 acesso somente leitura à chave TLS e certificados montados; nunca usar chmod 777. Garantir renovação de TLS no processo operacional.
6. Executar `docker compose --env-file .env.prod -f docker-compose.prod.yml config --quiet` e depois `docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build` em ambiente de homologação.
7. Verificar health interno com `docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T backend wget -qO- http://127.0.0.1:8081/actuator/health/readiness` e o scrape /actuator/prometheus na rede management. A porta da API nega Actuator.
8. Habilitar perfil observability quando necessário. Dashboard Grafana é montado em /var/lib/grafana/dashboards. Acesso administrativo exige túnel/VPN ou endpoint aprovado separado. VITE_PROMETHEUS_URL e VITE_GRAFANA_URL são argumentos de build opcionais; nunca adicionar localhost no navegador de produção.
9. Confirmar IP de origem e correlation ID em auditoria com uma operação de homologação; cabeçalhos enviados pelo cliente são substituídos pelo edge. Requisição direta de peer não confiável deve ignorar X-Forwarded-For.
10. Executar testes integrados, E2E, inventário/reconciliação de ocupação e restauração isolada antes de aprovação.

Gradle Docker usa o wrapper, Java 21. Frontend usa npm ci e o lock existente. Imagens possuem tags de versão; pin por digest, SBOM e resultados de scanner deverão ser registrados pela cadeia de release antes de produção. CI valida build, testes, tipagem/lint, cobertura, npm audit e estrutura. O provedor de cobertura ainda é instalado temporariamente em versão exata no job, pois o registry estava indisponível para atualizar o lock nesta entrega.

Rollback de aplicação exige avaliar migrations aditivas V9-V12. Não executar down -v, nem reverter schema sem backup/restore validado. A migration V9 invalida tokens antigos sem session_version, exigindo novo login. Não remover volumes durante homologação.

Antes de V12, verificar duplicidades de (camera_id,external_event_id) em cópia do banco. A migration falha explicitamente se houver colisões legadas, preservando todos os registros. Resolver esses casos por procedimento auditado, nunca apagar eventos para forçar implantação.
