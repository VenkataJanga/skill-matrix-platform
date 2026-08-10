-- ============================================================
-- V05 — Complete Application Portfolios
-- Skill Matrix Platform
-- Adds missing portfolios: B12-HOST, B20-WEB, B20-HOST
-- Safe to re-run: NOT EXISTS guards prevent duplicate inserts.
-- DO NOT MODIFY this file after deployment.
-- Create a new Vxx migration for any changes.
-- ============================================================

-- B12-HOST
INSERT INTO application_portfolios
    (public_id, account_id, application_type_id, bundle_id,
     portfolio_code, portfolio_name, description,
     status_lookup_id, is_active, created_by, updated_by)
SELECT UUID(), a.id, at2.id, b.id,
       'B12-HOST', 'Bundle 12 Host Portfolio',
       'Host applications in Bundle 12 for NTT DATA Germany',
       (SELECT lv.id FROM lookup_value lv
        JOIN lookup_type lt ON lt.id = lv.lookup_type_id
        WHERE lt.type_code = 'PORTFOLIO_STATUS' AND lv.value_code = 'ACTIVE'),
       1, 'system', 'system'
FROM accounts a
JOIN application_types at2 ON at2.type_code = 'HOST'
JOIN bundles b              ON b.bundle_code  = 'B12'
WHERE a.account_code = 'NTT_DATA'
  AND NOT EXISTS (
      SELECT 1 FROM application_portfolios ap
      WHERE ap.portfolio_code = 'B12-HOST'
  );

-- B20-WEB
INSERT INTO application_portfolios
    (public_id, account_id, application_type_id, bundle_id,
     portfolio_code, portfolio_name, description,
     status_lookup_id, is_active, created_by, updated_by)
SELECT UUID(), a.id, at2.id, b.id,
       'B20-WEB', 'Bundle 20 Web Portfolio',
       'Web applications in Bundle 20 for NTT DATA Germany',
       (SELECT lv.id FROM lookup_value lv
        JOIN lookup_type lt ON lt.id = lv.lookup_type_id
        WHERE lt.type_code = 'PORTFOLIO_STATUS' AND lv.value_code = 'ACTIVE'),
       1, 'system', 'system'
FROM accounts a
JOIN application_types at2 ON at2.type_code = 'WEB'
JOIN bundles b              ON b.bundle_code  = 'B20'
WHERE a.account_code = 'NTT_DATA'
  AND NOT EXISTS (
      SELECT 1 FROM application_portfolios ap
      WHERE ap.portfolio_code = 'B20-WEB'
  );

-- B20-HOST
INSERT INTO application_portfolios
    (public_id, account_id, application_type_id, bundle_id,
     portfolio_code, portfolio_name, description,
     status_lookup_id, is_active, created_by, updated_by)
SELECT UUID(), a.id, at2.id, b.id,
       'B20-HOST', 'Bundle 20 Host Portfolio',
       'Host applications in Bundle 20 for NTT DATA Germany',
       (SELECT lv.id FROM lookup_value lv
        JOIN lookup_type lt ON lt.id = lv.lookup_type_id
        WHERE lt.type_code = 'PORTFOLIO_STATUS' AND lv.value_code = 'ACTIVE'),
       1, 'system', 'system'
FROM accounts a
JOIN application_types at2 ON at2.type_code = 'HOST'
JOIN bundles b              ON b.bundle_code  = 'B20'
WHERE a.account_code = 'NTT_DATA'
  AND NOT EXISTS (
      SELECT 1 FROM application_portfolios ap
      WHERE ap.portfolio_code = 'B20-HOST'
  );

-- ============================================================
-- V05 complete
-- Summary:
--   3 portfolios added (if not already present):
--     B12-HOST — Bundle 12 Host Portfolio
--     B20-WEB  — Bundle 20 Web Portfolio
--     B20-HOST — Bundle 20 Host Portfolio
-- ============================================================
