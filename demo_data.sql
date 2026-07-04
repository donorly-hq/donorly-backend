-- =============================================================================
-- Donorly — Demo Data (Plain SQL — compatible with Railway web console)
-- =============================================================================
-- Run AFTER the application has started at least once so DataSeeder has
-- created the org (jamia-masjid-chicago) and demo user accounts.
-- Run each section separately in Railway's SQL editor if needed.
-- =============================================================================

-- ── Helpers (subqueries used inline) ─────────────────────────────────────────
-- org  : (SELECT id FROM organizations WHERE slug = 'jamia-masjid-chicago')
-- owner: (SELECT id FROM users WHERE lower(email) = 'owner@demo.donorly.org')
-- admin: (SELECT id FROM users WHERE lower(email) = 'admin@demo.donorly.org')
-- cm   : (SELECT id FROM users WHERE lower(email) = 'campaign@demo.donorly.org')
-- fin  : (SELECT id FROM users WHERE lower(email) = 'finance@demo.donorly.org')
-- amb  : (SELECT id FROM users WHERE lower(email) = 'ambassador@demo.donorly.org')
-- vol  : (SELECT id FROM users WHERE lower(email) = 'volunteer@demo.donorly.org')

-- =============================================================================
-- 1. Organisation Settings
-- =============================================================================

INSERT INTO organization_settings (organization_id, receipt_prefix, default_currency, ai_enabled, payment_enabled)
VALUES (
    (SELECT id FROM organizations WHERE slug = 'jamia-masjid-chicago'),
    'JMC', 'USD', true, true
)
ON CONFLICT (organization_id) DO UPDATE
    SET receipt_prefix = 'JMC', ai_enabled = true, payment_enabled = true;

-- =============================================================================
-- 2. Donor Tags
-- =============================================================================

INSERT INTO donor_tags (id, organization_id, name, color, created_by, modified_by)
VALUES
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Major Donor',   '#0a4f3f', (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Recurring',     '#2563eb', (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'New Donor',     '#d97706', (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'VIP',           '#7c3aed', (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Corporate',     '#475569', (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'))
ON CONFLICT (organization_id, name) DO NOTHING;

-- =============================================================================
-- 3. Campaigns
-- =============================================================================

INSERT INTO campaigns (id, organization_id, name, slug, campaign_type, goal_amount, start_date, end_date, status, created_by, modified_by)
VALUES
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Ramadan 2025 Campaign',   'ramadan-2025',   'annual_fund', 150000.00, '2025-03-01', '2025-04-15', 'active',    (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Masjid Expansion Fund',   'masjid-expansion','capital',    500000.00, '2025-01-01', '2026-12-31', 'active',    (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),    (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Youth Education Program', 'youth-edu-2024', 'program',      25000.00, '2024-09-01', '2024-12-31', 'completed', (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'))
ON CONFLICT (organization_id, slug) DO NOTHING;

-- Campaign Milestones
INSERT INTO campaign_milestones (id, organization_id, campaign_id, title, target_amount, target_date, status, created_by, modified_by)
VALUES
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), (SELECT id FROM campaigns WHERE slug='ramadan-2025'    AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')), 'First 10 Nights Goal',   50000.00, '2025-03-12', 'achieved', (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), (SELECT id FROM campaigns WHERE slug='ramadan-2025'    AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')), 'Last 10 Nights Push',   100000.00, '2025-04-05', 'pending',  (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), (SELECT id FROM campaigns WHERE slug='masjid-expansion' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')), 'Foundation Phase',      150000.00, '2025-06-30', 'achieved', (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),    (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), (SELECT id FROM campaigns WHERE slug='masjid-expansion' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')), 'Construction Phase 1',  300000.00, '2026-06-30', 'pending',  (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),    (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'));

-- =============================================================================
-- 4. Donors
-- =============================================================================

INSERT INTO donors (id, organization_id, full_name, email, phone, city, donor_type, status, lifetime_giving, created_by, modified_by)
VALUES
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Ahmed Hassan',        'ahmed.hassan@gmail.com',        '(312) 555-0101', 'Chicago',    'individual', 'active', 18500.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Fatima & Omar Khan',  'fatima.khan@icloud.com',         '(773) 555-0202', 'Naperville', 'family',     'active', 12000.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Yusuf Patel',         'yusuf.patel@business.com',       '(312) 555-0303', 'Schaumburg', 'individual', 'active', 31000.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Aisha Malik',         'aisha.malik@yahoo.com',          '(847) 555-0404', 'Oak Park',   'individual', 'active',  4500.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Halal Foods Inc.',    'giving@halalfoodsinc.com',        '(312) 555-0505', 'Chicago',    'business',   'active', 50000.00, (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'),  (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Zainab Abdullah',    'zainab.a@hotmail.com',            '(630) 555-0606', 'Aurora',     'individual', 'active',  1200.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Dr. Tariq Rahman',   'dr.tariq@rahmanmedical.com',      '(847) 555-0707', 'Evanston',   'individual', 'active', 22000.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Johnson Family',     'mark.johnson@johnsongroup.net',   '(312) 555-0808', 'Chicago',    'family',     'active',  8750.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Layla Qureshi',      'layla.q@gmail.com',               '(773) 555-0909', 'Chicago',    'individual', 'active',  3000.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Ibrahim Siddiqui',   'ibrahim.s@siddiquilaw.com',       '(312) 555-1010', 'Oakbrook',   'individual', 'active', 15500.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Maryam Farooq',      'maryam.farooq@northwestern.edu',  '(847) 555-1111', 'Evanston',   'individual', 'active',   750.00, (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Syed Tech Solutions', 'giving@syedtech.io',             '(312) 555-1212', 'Chicago',    'business',   'active', 28000.00, (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'),  (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'))
ON CONFLICT DO NOTHING;

-- Donor Profiles
INSERT INTO donor_profiles (donor_id, organization_id, occupation, employer, preferred_language, preferred_channel, notes_private, created_by, modified_by)
SELECT d.id, d.organization_id, p.occupation, p.employer, p.lang, p.channel, p.notes,
       (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',       'Software Engineer', 'Google LLC',                'English', 'email', 'Prefers contact after Fajr.'),
    ('fatima.khan@icloud.com',       'Homemaker',         'N/A',                       'Urdu',    'phone', 'Husband Omar handles finances.'),
    ('yusuf.patel@business.com',     'Business Owner',    'Patel Enterprises',         'Gujarati','email', 'Major donor — steward quarterly.'),
    ('aisha.malik@yahoo.com',        'Teacher',           'Chicago Public Schools',    'English', 'email', null),
    ('giving@halalfoodsinc.com',     'CEO',               'Halal Foods Inc.',          'Arabic',  'email', 'Corporate matching program available.'),
    ('dr.tariq@rahmanmedical.com',   'Physician',         'Rahman Medical Group',      'English', 'email', 'Interested in health outreach programs.'),
    ('ibrahim.s@siddiquilaw.com',    'Attorney',          'Siddiqui Law Firm',         'English', 'email', 'Pro-bono legal support offered.'),
    ('giving@syedtech.io',           'CTO',               'Syed Tech Solutions',       'English', 'email', 'Tech sponsorship for events.')
) AS p(email, occupation, employer, lang, channel, notes)
JOIN donors d ON lower(d.email) = lower(p.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
ON CONFLICT (donor_id) DO NOTHING;

-- Donor Tag Assignments
INSERT INTO donor_tag_assignments (donor_id, tag_id, created_by, modified_by)
SELECT d.id, t.id,
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',    'Major Donor'),
    ('ahmed.hassan@gmail.com',    'Recurring'),
    ('yusuf.patel@business.com',  'Major Donor'),
    ('yusuf.patel@business.com',  'VIP'),
    ('giving@halalfoodsinc.com',  'Corporate'),
    ('giving@halalfoodsinc.com',  'Major Donor'),
    ('dr.tariq@rahmanmedical.com','Major Donor'),
    ('dr.tariq@rahmanmedical.com','VIP'),
    ('ibrahim.s@siddiquilaw.com', 'VIP'),
    ('giving@syedtech.io',        'Corporate'),
    ('zainab.a@hotmail.com',      'New Donor'),
    ('maryam.farooq@northwestern.edu','New Donor'),
    ('fatima.khan@icloud.com',    'Recurring'),
    ('mark.johnson@johnsongroup.net','Recurring')
) AS x(email, tag_name)
JOIN donors d ON lower(d.email) = lower(x.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
JOIN donor_tags t ON t.name = x.tag_name
   AND t.organization_id = d.organization_id
ON CONFLICT DO NOTHING;

-- Donor Notes
INSERT INTO donor_notes (id, organization_id, donor_id, note_text, note_type, visibility, created_by, modified_by)
SELECT gen_random_uuid(), d.organization_id, d.id, n.note_text, n.note_type, n.visibility,
       (SELECT id FROM users WHERE lower(email)=n.author),
       (SELECT id FROM users WHERE lower(email)=n.author)
FROM (VALUES
    ('ahmed.hassan@gmail.com',      'Spoke to Ahmed at Friday prayers — excited about Ramadan campaign. May increase pledge this year.', 'call',    'team',    'ambassador@demo.donorly.org'),
    ('ahmed.hassan@gmail.com',      'Sent thank-you letter after last year''s $10,000 contribution.', 'note', 'team', 'admin@demo.donorly.org'),
    ('yusuf.patel@business.com',    'Yusuf confirmed matching up to $5,000 for expansion fund.', 'meeting', 'team', 'campaign@demo.donorly.org'),
    ('giving@halalfoodsinc.com',    'Corporate sponsorship proposal sent to CFO. Awaiting board approval.', 'email', 'team', 'campaign@demo.donorly.org'),
    ('dr.tariq@rahmanmedical.com',  'Dr. Tariq interested in establishing a named scholarship for youth program.', 'meeting', 'team', 'ambassador@demo.donorly.org'),
    ('fatima.khan@icloud.com',      'Called Fatima — family pledge of $1,000/month confirmed for Ramadan.', 'call', 'team', 'ambassador@demo.donorly.org'),
    ('layla.q@gmail.com',           'Layla is new this year. Met at community iftar dinner.', 'note', 'private', 'ambassador@demo.donorly.org')
) AS n(email, note_text, note_type, visibility, author)
JOIN donors d ON lower(d.email) = lower(n.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago');

-- Donor Assignments
INSERT INTO donor_assignments (id, organization_id, donor_id, ambassador_user_id, campaign_id, status, created_by, modified_by)
SELECT gen_random_uuid(),
       (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
       d.id,
       (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'),
       c.id,
       'active',
       (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',         'ramadan-2025'),
    ('fatima.khan@icloud.com',         'ramadan-2025'),
    ('yusuf.patel@business.com',       'masjid-expansion'),
    ('aisha.malik@yahoo.com',          'ramadan-2025'),
    ('zainab.a@hotmail.com',           'ramadan-2025'),
    ('dr.tariq@rahmanmedical.com',     'masjid-expansion'),
    ('layla.q@gmail.com',              'ramadan-2025'),
    ('maryam.farooq@northwestern.edu', 'ramadan-2025')
) AS x(email, campaign_slug)
JOIN donors d ON lower(d.email) = lower(x.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
JOIN campaigns c ON c.slug = x.campaign_slug
   AND c.organization_id = d.organization_id
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 5. Pledges
-- =============================================================================

INSERT INTO pledges (id, organization_id, campaign_id, donor_id, amount, collected_amount,
                     frequency, payment_method, start_date, end_date, status, source, notes, created_by, modified_by)
SELECT gen_random_uuid(), d.organization_id, c.id, d.id,
       p.amount, p.collected, p.frequency, p.method,
       p.start_date::date, p.end_date::date, p.status, p.source, p.notes,
       (SELECT id FROM users WHERE lower(email)=p.creator),
       (SELECT id FROM users WHERE lower(email)=p.creator)
FROM (VALUES
    ('ahmed.hassan@gmail.com',      'ramadan-2025',    10000.00,  7500.00, 'one_time',  'check',        '2025-03-05', null,         'active',    'in_person', 'Annual Ramadan pledge.',            'ambassador@demo.donorly.org'),
    ('fatima.khan@icloud.com',      'ramadan-2025',    12000.00,  6000.00, 'monthly',   'bank_transfer','2025-03-01', '2025-04-15', 'active',    'phone',     '$1,000/month x 12.',               'ambassador@demo.donorly.org'),
    ('yusuf.patel@business.com',    'masjid-expansion',25000.00, 10000.00, 'quarterly', 'check',        '2025-01-15', '2026-01-15', 'active',    'in_person', 'Expansion fund — quarterly.',       'ambassador@demo.donorly.org'),
    ('aisha.malik@yahoo.com',       'ramadan-2025',     2500.00,  2500.00, 'one_time',  'cash',         '2025-03-10', null,         'fulfilled', 'in_person', null,                                'ambassador@demo.donorly.org'),
    ('giving@halalfoodsinc.com',    'masjid-expansion',50000.00, 25000.00, 'quarterly', 'ach',          '2025-02-01', '2026-02-01', 'active',    'email',     'Corporate pledge — 4 payments.',    'campaign@demo.donorly.org'),
    ('zainab.a@hotmail.com',        'ramadan-2025',     1000.00,     0.00, 'one_time',  null,           '2025-03-20', null,         'pending',   'in_person', 'New donor — first pledge.',         'ambassador@demo.donorly.org'),
    ('dr.tariq@rahmanmedical.com',  'masjid-expansion',15000.00,  5000.00, 'one_time',  'check',        '2025-04-01', null,         'active',    'meeting',   null,                                'ambassador@demo.donorly.org'),
    ('mark.johnson@johnsongroup.net','youth-edu-2024',   5000.00,  5000.00, 'one_time',  'credit_card',  '2024-09-15', null,         'fulfilled', 'online',    'Youth program sponsor.',            'campaign@demo.donorly.org')
) AS p(email, campaign_slug, amount, collected, frequency, method, start_date, end_date, status, source, notes, creator)
JOIN donors d ON lower(d.email) = lower(p.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
JOIN campaigns c ON c.slug = p.campaign_slug
   AND c.organization_id = d.organization_id
ON CONFLICT DO NOTHING;

-- Pledge Schedules
INSERT INTO pledge_schedules (id, organization_id, pledge_id, due_date, amount_due, status, created_by, modified_by)
SELECT gen_random_uuid(), pl.organization_id, pl.id, s.due_date::date, s.amount_due, s.status,
       (SELECT id FROM users WHERE lower(email)='finance@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='finance@demo.donorly.org')
FROM (VALUES
    ('fatima.khan@icloud.com',   'ramadan-2025',    '2025-03-01',  1000.00, 'paid'),
    ('fatima.khan@icloud.com',   'ramadan-2025',    '2025-03-15',  1000.00, 'paid'),
    ('fatima.khan@icloud.com',   'ramadan-2025',    '2025-04-01',  1000.00, 'paid'),
    ('fatima.khan@icloud.com',   'ramadan-2025',    '2025-04-15',  1000.00, 'pending'),
    ('yusuf.patel@business.com', 'masjid-expansion','2025-01-15',  6250.00, 'paid'),
    ('yusuf.patel@business.com', 'masjid-expansion','2025-04-15',  6250.00, 'paid'),
    ('yusuf.patel@business.com', 'masjid-expansion','2025-07-15',  6250.00, 'pending'),
    ('yusuf.patel@business.com', 'masjid-expansion','2025-10-15',  6250.00, 'pending'),
    ('giving@halalfoodsinc.com', 'masjid-expansion','2025-02-01', 12500.00, 'paid'),
    ('giving@halalfoodsinc.com', 'masjid-expansion','2025-05-01', 12500.00, 'paid'),
    ('giving@halalfoodsinc.com', 'masjid-expansion','2025-08-01', 12500.00, 'pending'),
    ('giving@halalfoodsinc.com', 'masjid-expansion','2025-11-01', 12500.00, 'pending')
) AS s(email, campaign_slug, due_date, amount_due, status)
JOIN donors d ON lower(d.email) = lower(s.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
JOIN campaigns c ON c.slug = s.campaign_slug AND c.organization_id = d.organization_id
JOIN pledges pl ON pl.donor_id = d.id AND pl.campaign_id = c.id
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 6. Payments & Receipts
-- =============================================================================

INSERT INTO payments (id, organization_id, pledge_id, donor_id, amount, payment_method, payment_date, reference, notes, recorded_by, created_by, modified_by)
SELECT gen_random_uuid(), d.organization_id, pl.id, d.id,
       p.amount, p.method, p.pay_date::date, p.ref, p.notes,
       (SELECT id FROM users WHERE lower(email)='finance@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='finance@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='finance@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',   'ramadan-2025',     5000.00, 'check',        '2025-03-10', 'CHK-10042', null),
    ('ahmed.hassan@gmail.com',   'ramadan-2025',     2500.00, 'check',        '2025-03-25', 'CHK-10051', null),
    ('fatima.khan@icloud.com',   'ramadan-2025',     3000.00, 'bank_transfer','2025-03-05', 'ACH-88201', '3 payments bundled.'),
    ('yusuf.patel@business.com', 'masjid-expansion',10000.00, 'check',        '2025-02-01', 'CHK-10018', null),
    ('aisha.malik@yahoo.com',    'ramadan-2025',     2500.00, 'cash',         '2025-03-10', null,        'Paid in full at event.')
) AS p(email, campaign_slug, amount, method, pay_date, ref, notes)
JOIN donors d ON lower(d.email) = lower(p.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
JOIN campaigns c ON c.slug = p.campaign_slug AND c.organization_id = d.organization_id
JOIN pledges pl ON pl.donor_id = d.id AND pl.campaign_id = c.id
ON CONFLICT DO NOTHING;

-- Receipts
INSERT INTO receipts (id, organization_id, payment_id, receipt_number, issued_to, amount, issued_at, issued_by, created_by, modified_by)
SELECT gen_random_uuid(), py.organization_id, py.id,
       r.receipt_number, r.issued_to, py.amount,
       now() - (r.days_ago || ' days')::interval,
       (SELECT id FROM users WHERE lower(email)='finance@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='finance@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='finance@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',   'ramadan-2025',     5000.00, 'JMC-2025-0001', 'Ahmed Hassan',       15),
    ('ahmed.hassan@gmail.com',   'ramadan-2025',     2500.00, 'JMC-2025-0002', 'Ahmed Hassan',        0),
    ('fatima.khan@icloud.com',   'ramadan-2025',     3000.00, 'JMC-2025-0003', 'Fatima & Omar Khan', 25),
    ('yusuf.patel@business.com', 'masjid-expansion',10000.00, 'JMC-2025-0004', 'Yusuf Patel',        50),
    ('aisha.malik@yahoo.com',    'ramadan-2025',     2500.00, 'JMC-2025-0005', 'Aisha Malik',        15)
) AS r(email, campaign_slug, amount, receipt_number, issued_to, days_ago)
JOIN donors d ON lower(d.email) = lower(r.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
JOIN campaigns c ON c.slug = r.campaign_slug AND c.organization_id = d.organization_id
JOIN pledges pl ON pl.donor_id = d.id AND pl.campaign_id = c.id
JOIN payments py ON py.pledge_id = pl.id AND py.amount = r.amount
ON CONFLICT (organization_id, receipt_number) DO NOTHING;

-- =============================================================================
-- 7. Follow-ups
-- =============================================================================

INSERT INTO follow_ups (id, organization_id, donor_id, campaign_id, assigned_to_user_id, due_at, status, outcome, notes, created_by, modified_by)
SELECT gen_random_uuid(), d.organization_id, d.id, c.id,
       (SELECT id FROM users WHERE lower(email)=f.assignee),
       now() + (f.due_offset || ' days')::interval,
       f.status, f.outcome, f.notes,
       (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',         'ramadan-2025',    'ambassador@demo.donorly.org',  3,   'open',      null,      'Remind Ahmed about second pledge installment.'),
    ('fatima.khan@icloud.com',         'ramadan-2025',    'ambassador@demo.donorly.org',  1,   'open',      null,      'Confirm bank transfer scheduled for April.'),
    ('yusuf.patel@business.com',       'masjid-expansion','ambassador@demo.donorly.org',  7,   'open',      null,      'Q3 payment reminder for expansion fund.'),
    ('giving@halalfoodsinc.com',       'masjid-expansion','campaign@demo.donorly.org',   14,   'open',      null,      'Follow up on board approval for Q3 payment.'),
    ('zainab.a@hotmail.com',           'ramadan-2025',    'ambassador@demo.donorly.org', -2,   'overdue',   null,      'Layla has not confirmed pledge method yet.'),
    ('dr.tariq@rahmanmedical.com',     'masjid-expansion','ambassador@demo.donorly.org',  5,   'open',      null,      'Discuss named scholarship opportunity.'),
    ('layla.q@gmail.com',              'ramadan-2025',    'ambassador@demo.donorly.org', -1,   'open',      null,      'New donor — welcome call and pledge confirmation.'),
    ('aisha.malik@yahoo.com',          'ramadan-2025',    'ambassador@demo.donorly.org',-10,   'completed', 'pledged', 'Pledge fulfilled — receipt sent.'),
    ('mark.johnson@johnsongroup.net',  'youth-edu-2024',  'campaign@demo.donorly.org',  -30,  'completed', 'paid',    'Youth program sponsor confirmed and paid.')
) AS f(email, campaign_slug, assignee, due_offset, status, outcome, notes)
JOIN donors d ON lower(d.email) = lower(f.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
JOIN campaigns c ON c.slug = f.campaign_slug AND c.organization_id = d.organization_id
ON CONFLICT DO NOTHING;

-- Pledge Cards
INSERT INTO pledge_cards (id, organization_id, campaign_id, donor_id, amount, payment_method, notes, verification_status, created_by, modified_by)
SELECT gen_random_uuid(), d.organization_id, c.id, d.id,
       p.amount, p.method, p.notes, p.vstatus,
       (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',    'ramadan-2025',    10000.00, 'check',         'Scanned at pledge banquet.', 'approved'),
    ('fatima.khan@icloud.com',    'ramadan-2025',    12000.00, 'bank_transfer',  null,                        'approved'),
    ('zainab.a@hotmail.com',      'ramadan-2025',     1000.00, null,            'Needs payment method.',      'pending'),
    ('dr.tariq@rahmanmedical.com','masjid-expansion', 15000.00, 'check',         null,                        'reviewed')
) AS p(email, campaign_slug, amount, method, notes, vstatus)
JOIN donors d ON lower(d.email) = lower(p.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
JOIN campaigns c ON c.slug = p.campaign_slug AND c.organization_id = d.organization_id
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 8. Events
-- =============================================================================

INSERT INTO events (id, organization_id, campaign_id, name, description, event_type, location, starts_at, ends_at, capacity, status, created_by, modified_by)
VALUES
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM campaigns WHERE slug='ramadan-2025' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')),
     'Ramadan Pledge Banquet 2025', 'Annual fundraising dinner. Dinner, keynote, and pledge cards.',
     'fundraiser', 'Renaissance Chicago Downtown Hotel, 1 W Wacker Dr',
     '2025-03-28 18:30:00+00', '2025-03-28 22:00:00+00', 300, 'completed',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM campaigns WHERE slug='masjid-expansion' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')),
     'Masjid Expansion Ground-Breaking', 'Ceremony to break ground on the new wing. Community invited.',
     'community', '1 N Wabash Ave, Chicago, IL 60602',
     now() + interval '45 days', now() + interval '45 days' + interval '3 hours', 500, 'published',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     null,
     'Summer Community BBQ', 'Annual community gathering and family picnic. Free admission.',
     'community', 'Millennium Park, Chicago',
     now() + interval '60 days', now() + interval '60 days' + interval '5 hours', 1000, 'published',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'))
ON CONFLICT DO NOTHING;

-- Event Registrations
INSERT INTO event_registrations (id, organization_id, event_id, donor_id, guest_name, guest_email, guest_phone, party_size, status, check_in_code, checked_in_at, notes, created_by, modified_by)
SELECT gen_random_uuid(), d.organization_id,
       (SELECT id FROM events WHERE name=r.event_name AND organization_id=d.organization_id),
       d.id, r.guest_name, lower(d.email), d.phone, r.party_size, r.status, r.code, r.checked_in, r.notes,
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',       'Ramadan Pledge Banquet 2025', 'Ahmed Hassan',       2, 'checked_in', 'AH2025001', '2025-03-28 18:45:00+00'::timestamptz, null),
    ('fatima.khan@icloud.com',       'Ramadan Pledge Banquet 2025', 'Fatima & Omar Khan', 4, 'checked_in', 'FK2025002', '2025-03-28 18:55:00+00'::timestamptz, 'Table 3'),
    ('yusuf.patel@business.com',     'Ramadan Pledge Banquet 2025', 'Yusuf Patel',        2, 'checked_in', 'YP2025003', '2025-03-28 19:02:00+00'::timestamptz, null),
    ('dr.tariq@rahmanmedical.com',   'Ramadan Pledge Banquet 2025', 'Dr. Tariq Rahman',   2, 'checked_in', 'TR2025004', '2025-03-28 18:50:00+00'::timestamptz, 'Head table'),
    ('giving@halalfoodsinc.com',     'Ramadan Pledge Banquet 2025', 'Halal Foods Inc.',   6, 'checked_in', 'HF2025005', '2025-03-28 18:40:00+00'::timestamptz, 'Corporate table'),
    ('aisha.malik@yahoo.com',        'Ramadan Pledge Banquet 2025', 'Aisha Malik',        1, 'no_show',    'AM2025006', null,                                  null),
    ('yusuf.patel@business.com',     'Masjid Expansion Ground-Breaking', 'Yusuf Patel',   2, 'registered', 'YP2025010', null,                                  null),
    ('ahmed.hassan@gmail.com',       'Masjid Expansion Ground-Breaking', 'Ahmed Hassan',  3, 'registered', 'AH2025011', null,                                  null),
    ('layla.q@gmail.com',            'Summer Community BBQ',        'Layla Qureshi',      4, 'registered', 'LQ2025020', null,                                  null),
    ('maryam.farooq@northwestern.edu','Summer Community BBQ',       'Maryam Farooq',      2, 'registered', 'MF2025021', null,                                  null)
) AS r(email, event_name, guest_name, party_size, status, code, checked_in, notes)
JOIN donors d ON lower(d.email) = lower(r.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 9. Volunteer Shifts & Assignments
-- =============================================================================

INSERT INTO volunteer_shifts (id, organization_id, event_id, title, description, starts_at, ends_at, slots, status, created_by, modified_by)
VALUES
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM events WHERE name='Ramadan Pledge Banquet 2025' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')),
     'Registration Desk', 'Welcome guests and hand out pledge cards.',
     '2025-03-28 17:30:00+00', '2025-03-28 19:30:00+00', 4, 'closed',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM events WHERE name='Masjid Expansion Ground-Breaking' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')),
     'Crowd Management', 'Direct attendees and manage parking area.',
     now() + interval '45 days', now() + interval '45 days' + interval '4 hours', 6, 'open',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM events WHERE name='Summer Community BBQ' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')),
     'Food & Beverages', 'Set up, serve, and clean up the food stations.',
     now() + interval '60 days', now() + interval '60 days' + interval '6 hours', 8, 'open',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'))
ON CONFLICT DO NOTHING;

INSERT INTO volunteer_assignments (id, organization_id, shift_id, user_id, status, checked_in_at, created_by, modified_by)
VALUES
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM volunteer_shifts WHERE title='Registration Desk' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')),
     (SELECT id FROM users WHERE lower(email)='volunteer@demo.donorly.org'),
     'completed', '2025-03-28 17:35:00+00',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM volunteer_shifts WHERE title='Crowd Management' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')),
     (SELECT id FROM users WHERE lower(email)='volunteer@demo.donorly.org'),
     'assigned', null,
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM volunteer_shifts WHERE title='Food & Beverages' AND organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')),
     (SELECT id FROM users WHERE lower(email)='volunteer@demo.donorly.org'),
     'assigned', null,
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'))
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 10. Townhalls
-- =============================================================================

INSERT INTO townhalls (id, organization_id, person_name, address, event_date, event_time, duration_minutes, notes, created_by, modified_by)
VALUES
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Ahmed Hassan',     '4521 N Kedzie Ave, Chicago, IL',      CURRENT_DATE+5,  '19:00', 90, 'Expansion fund discussion — expected 12 attendees.', (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Dr. Tariq Rahman', '820 Davis St, Evanston, IL',           CURRENT_DATE+10, '18:30', 60, 'Youth scholarship program briefing.',                (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Yusuf Patel',      '1225 Corporate Blvd, Schaumburg, IL',  CURRENT_DATE+14, '12:00', 60, 'Business community lunch — bring pledge packets.',   (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'),  (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Fatima Khan',      '209 S Oak Park Ave, Oak Park, IL',     CURRENT_DATE+21, '20:00', 45, null,                                                 (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Ibrahim Siddiqui', '333 W Wacker Dr, Chicago, IL',         CURRENT_DATE-5,  '12:30', 60, 'Completed — raised $3,000 in commitments.',          (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'), 'Layla Qureshi',    '2150 N Clark St, Chicago, IL',         CURRENT_DATE-12, '19:00', 45, 'First townhall with new donor. Very positive.',      (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'), (SELECT id FROM users WHERE lower(email)='ambassador@demo.donorly.org'))
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 11. Message Templates
-- =============================================================================

INSERT INTO message_templates (id, organization_id, name, channel, subject, body, is_system, created_by, modified_by)
VALUES
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     'Pledge Confirmation Email', 'email',
     'Thank You for Your Pledge — Jamia Masjid Chicago',
     E'Dear {{donor_name}},\n\nJazakAllahu Khayran for your generous pledge of {{amount}} to the {{campaign_name}}.\n\nYour commitment is a testament to your dedication to our community. May Allah (SWT) bless you and your family abundantly.\n\nWarm regards,\nJamia Masjid Chicago',
     false,
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     'Ramadan Appeal SMS', 'sms', null,
     E'Assalamu Alaikum {{donor_name}}! Ramadan Mubarak! Please consider fulfilling your pledge. Every dollar brings us closer to our goal. Reply STOP to opt out.',
     false,
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     'Payment Receipt Email', 'email',
     'Payment Receipt #{{receipt_number}} — Jamia Masjid Chicago',
     E'Dear {{donor_name}},\n\nThis confirms your payment of {{amount}} on {{payment_date}}.\n\nReceipt: {{receipt_number}}\nCampaign: {{campaign_name}}\n\nPlease retain for tax purposes.\n\nJamia Masjid Chicago',
     false,
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     'Follow-Up Reminder', 'email',
     'A Gentle Reminder — {{campaign_name}}',
     E'Dear {{donor_name}},\n\nThis is a friendly reminder regarding your pledge commitment. Our ambassador will be in touch shortly.\n\nThank you for your continued support.\n\nJamia Masjid Chicago',
     false,
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(), (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     'Event Invitation', 'email',
     'You''re Invited: {{event_name}}',
     E'Dear {{donor_name}},\n\nWe are delighted to invite you to {{event_name}} on {{event_date}} at {{event_location}}.\n\nPlease RSVP by replying to this email.\n\nJamia Masjid Chicago',
     false,
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'))
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 12. Communication Messages (sent history)
-- =============================================================================

INSERT INTO communication_messages (id, organization_id, channel, recipient, donor_id, template_id, subject, body, status, sent_by, sent_at, created_by, modified_by)
SELECT gen_random_uuid(), d.organization_id, m.channel, lower(d.email), d.id,
       (SELECT id FROM message_templates WHERE name=m.template_name AND organization_id=d.organization_id),
       m.subject, m.body_snippet, 'sent',
       (SELECT id FROM users WHERE lower(email)=m.sender),
       now() - (m.days_ago || ' days')::interval,
       (SELECT id FROM users WHERE lower(email)=m.sender),
       (SELECT id FROM users WHERE lower(email)=m.sender)
FROM (VALUES
    ('ahmed.hassan@gmail.com',     'Pledge Confirmation Email', 'email', 'Thank You for Your Pledge — JMC', 'Dear Ahmed Hassan, JazakAllahu Khayran for your pledge of $10,000...', 'admin@demo.donorly.org',   20),
    ('fatima.khan@icloud.com',     'Pledge Confirmation Email', 'email', 'Thank You for Your Pledge — JMC', 'Dear Fatima & Omar Khan, JazakAllahu Khayran for your pledge of $12,000...', 'admin@demo.donorly.org', 18),
    ('ahmed.hassan@gmail.com',     'Payment Receipt Email',     'email', 'Payment Receipt #JMC-2025-0001',  'Dear Ahmed Hassan, Confirming payment of $5,000...', 'finance@demo.donorly.org', 15),
    ('zainab.a@hotmail.com',       'Follow-Up Reminder',        'email', 'A Gentle Reminder — Ramadan 2025', 'Dear Zainab Abdullah, Friendly reminder about your pledge...', 'ambassador@demo.donorly.org', 3),
    ('dr.tariq@rahmanmedical.com', 'Pledge Confirmation Email', 'email', 'Thank You for Your Pledge — JMC', 'Dear Dr. Tariq Rahman, JazakAllahu Khayran for your pledge of $15,000...', 'admin@demo.donorly.org', 10)
) AS m(email, template_name, channel, subject, body_snippet, sender, days_ago)
JOIN donors d ON lower(d.email) = lower(m.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 13. AI Insights
-- =============================================================================

INSERT INTO ai_insights (id, organization_id, entity_type, entity_id, insight, model, generated_by, created_by, modified_by)
SELECT gen_random_uuid(), d.organization_id, 'donor', d.id,
       i.insight, 'gpt-4o-mini',
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')
FROM (VALUES
    ('ahmed.hassan@gmail.com',    'Ahmed Hassan shows a strong giving trajectory with consistent annual pledges increasing ~15% year-over-year. He is most responsive to in-person asks during Friday prayers. Recommend scheduling a personal meeting before the Ramadan campaign closes to discuss a legacy giving opportunity.'),
    ('yusuf.patel@business.com',  'Yusuf Patel is a high-capacity donor with significant untapped potential. His business revenue suggests he could sustain a $50,000+ annual gift. He responded positively to the capital campaign narrative. Consider presenting a naming opportunity for the new masjid wing.')
) AS i(email, insight)
JOIN donors d ON lower(d.email) = lower(i.email)
   AND d.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
ON CONFLICT DO NOTHING;

INSERT INTO ai_insights (id, organization_id, entity_type, entity_id, insight, model, generated_by, created_by, modified_by)
SELECT gen_random_uuid(), c.organization_id, 'campaign', c.id,
       i.insight, 'gpt-4o-mini',
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
       (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')
FROM (VALUES
    ('ramadan-2025',    'The Ramadan 2025 Campaign is at 56% of its $150,000 goal with 3 weeks remaining. Major donors (>$5,000) represent 78% of funds raised. Recommend concentrating remaining outreach on mid-tier donors ($500–$5,000) to broaden the donor base.'),
    ('masjid-expansion','The Masjid Expansion Fund has secured 17% of its $500,000 goal. Corporate donors represent 43% of current pledges. Recommend launching a community matching challenge to accelerate momentum.')
) AS i(slug, insight)
JOIN campaigns c ON c.slug = i.slug
   AND c.organization_id = (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')
ON CONFLICT DO NOTHING;

INSERT INTO ai_insights (id, organization_id, entity_type, entity_id, insight, model, generated_by, created_by, modified_by)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
    'org', (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
    'Organization health summary: 12 active donors, $195,750 in total pledges, $28,000 collected this month. Donor retention rate is strong at 85%. Three donors have open follow-ups past due. Recommend prioritising follow-up on Zainab Abdullah (new donor) and the Halal Foods Inc. Q3 installment.',
    'gpt-4o-mini',
    (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
    (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
    (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')
)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 14. AI Conversations
-- =============================================================================

INSERT INTO ai_conversations (id, organization_id, user_id, question, answer, status, model, created_by, modified_by)
VALUES
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     'Which donors have not yet made a payment against their Ramadan pledge?',
     'Based on current data, the following donors have open Ramadan pledges with no payments recorded: Zainab Abdullah ($1,000 pending) and Layla Qureshi (pledge confirmed, no payment method). Both have open follow-ups assigned to the ambassador.',
     'answered', 'gpt-4o-mini',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org')),
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'),
     'What is the total amount collected in March 2025?',
     'In March 2025, a total of $10,500 was collected: $7,500 from the Ramadan Campaign (Ahmed Hassan x2) and $3,000 from Fatima Khan bank transfer. Outstanding for March is approximately $5,000.',
     'answered', 'gpt-4o-mini',
     (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='campaign@demo.donorly.org')),
    (gen_random_uuid(),
     (SELECT id FROM organizations WHERE slug='jamia-masjid-chicago'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     'Summarise the expansion campaign status for the board meeting.',
     'The Masjid Expansion Fund stands at $35,000 collected of $500,000 goal (7%). Corporate donors lead contributions. Two quarterly installments are pending from Yusuf Patel and Halal Foods Inc. The ground-breaking ceremony is scheduled in approximately 45 days.',
     'answered', 'gpt-4o-mini',
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'),
     (SELECT id FROM users WHERE lower(email)='admin@demo.donorly.org'))
ON CONFLICT DO NOTHING;

-- =============================================================================
-- Done
-- =============================================================================
SELECT 'Demo data loaded successfully' AS result,
       (SELECT count(*) FROM donors      WHERE organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')) AS donors,
       (SELECT count(*) FROM campaigns   WHERE organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')) AS campaigns,
       (SELECT count(*) FROM pledges     WHERE organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')) AS pledges,
       (SELECT count(*) FROM payments    WHERE organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')) AS payments,
       (SELECT count(*) FROM follow_ups  WHERE organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')) AS follow_ups,
       (SELECT count(*) FROM events      WHERE organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')) AS events,
       (SELECT count(*) FROM townhalls   WHERE organization_id=(SELECT id FROM organizations WHERE slug='jamia-masjid-chicago')) AS townhalls;
