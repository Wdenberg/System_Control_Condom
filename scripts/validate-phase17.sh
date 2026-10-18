#!/usr/bin/env bash
set -euo pipefail

echo '== Phase 17 static validation =='
grep -q 'micrometer-registry-prometheus' backend/build.gradle
grep -q 'include: health,prometheus,metrics' backend/src/main/resources/application.yml
grep -q 'garage_hikvision_events_received_total' backend/src/main/java/com/estacionamento/api/observability/GarageMetrics.java
grep -q 'MDC.put("correlationId"' backend/src/main/java/com/estacionamento/api/web/filter/CorrelationIdFilter.java
grep -q 'path="observability"' frontend/src/app/App.tsx
grep -q 'ObservabilityPage' frontend/src/pages/ObservabilityPage.tsx
echo 'Static validation OK'
