-- ============================================================
-- V13 — Assessment Cycle Header Support (M4)
-- Skill Matrix Platform
--
-- assessment_cycles and technician_assessments already exist (V01)
-- and already model:
--   AssessmentCycle (cycle_name, dates, status, application, requirement_version)
--   AssessmentItem  (technician_assessments — one row per technician+cycle+skill)
--
-- technician_assessments is ITEM-grained only; there is no existing
-- header row representing "one technician's assessment for one cycle"
-- as a whole (with its own status/submitted_at/concurrency token).
--
-- This migration ADDS a new header table and links existing item
-- rows to it. No existing migration (V01-V12) is modified.
-- ============================================================

-- ------------------------------------------------------------
-- 1. New header table — authoritative status/submitted_at/version
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS technician_assessment_headers (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    public_id        VARCHAR(36)  NOT NULL,
    cycle_id         BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    status_lookup_id BIGINT,
    submitted_at     DATETIME,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT pk_technician_assessment_headers        PRIMARY KEY (id),
    CONSTRAINT uq_technician_assessment_headers_pubid   UNIQUE (public_id),
    CONSTRAINT uq_tah_cycle_user                        UNIQUE (cycle_id, user_id),
    CONSTRAINT fk_tah_cycle                             FOREIGN KEY (cycle_id) REFERENCES assessment_cycles (id),
    CONSTRAINT fk_tah_user                              FOREIGN KEY (user_id)  REFERENCES users (id),
    CONSTRAINT fk_tah_status_lookup                     FOREIGN KEY (status_lookup_id) REFERENCES lookup_value (id),
    INDEX idx_tah_user_cycle (user_id, cycle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 2. Link existing item rows (technician_assessments) to a header
--    Added nullable first so we can backfill safely.
-- ------------------------------------------------------------
ALTER TABLE technician_assessments
    ADD COLUMN assessment_header_id BIGINT NULL AFTER cycle_id;

-- ------------------------------------------------------------
-- 3. Backfill: synthesize one header per distinct (cycle_id, user_id)
--    found in existing technician_assessments rows (covers any
--    dev-seeded demo data). Header's initial status/submitted_at
--    is taken from the most recently submitted item row, if any.
-- ------------------------------------------------------------
INSERT INTO technician_assessment_headers (public_id, cycle_id, user_id, status_lookup_id, submitted_at, created_by)
SELECT UUID(), ta.cycle_id, ta.user_id,
       (SELECT t2.status_lookup_id FROM technician_assessments t2
         WHERE t2.cycle_id = ta.cycle_id AND t2.user_id = ta.user_id
         ORDER BY t2.submitted_at DESC LIMIT 1),
       MAX(ta.submitted_at),
       'system'
FROM technician_assessments ta
GROUP BY ta.cycle_id, ta.user_id;

UPDATE technician_assessments ta
JOIN technician_assessment_headers h
  ON h.cycle_id = ta.cycle_id AND h.user_id = ta.user_id
SET ta.assessment_header_id = h.id;

-- ------------------------------------------------------------
-- 4. Enforce NOT NULL + FK now that backfill guarantees no orphans
-- ------------------------------------------------------------
ALTER TABLE technician_assessments
    MODIFY COLUMN assessment_header_id BIGINT NOT NULL;

ALTER TABLE technician_assessments
    ADD CONSTRAINT fk_ta_header FOREIGN KEY (assessment_header_id) REFERENCES technician_assessment_headers (id);

CREATE INDEX idx_ta_header ON technician_assessments (assessment_header_id);

-- V13 complete
