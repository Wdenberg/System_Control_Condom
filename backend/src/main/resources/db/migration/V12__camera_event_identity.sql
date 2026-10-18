-- Preserve data and fail explicitly if legacy camera identities need reconciliation.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM hikvision_events WHERE camera_id IS NOT NULL GROUP BY camera_id, external_event_id HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Legacy duplicate camera event identifiers require audited reconciliation before V12';
    END IF;
END $$;
CREATE UNIQUE INDEX uk_hikvision_camera_external ON hikvision_events(camera_id, external_event_id) WHERE camera_id IS NOT NULL;
