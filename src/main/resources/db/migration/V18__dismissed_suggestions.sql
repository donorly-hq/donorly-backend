-- Dashboard suggestions are computed on the fly from live data; only the
-- user's dismissals persist. A dismissal snoozes one suggestion key until
-- dismissed_until (re-dismissing just pushes the date out).

CREATE TABLE dismissed_suggestions (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID          NOT NULL REFERENCES organizations(id),
    suggestion_key  VARCHAR(120)  NOT NULL,
    dismissed_by    UUID          REFERENCES users(id),
    dismissed_until TIMESTAMPTZ   NOT NULL,
    -- audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID          REFERENCES users(id),
    modified_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modified_by     UUID          REFERENCES users(id),
    CONSTRAINT uq_dismissed_suggestions_org_key UNIQUE (organization_id, suggestion_key)
);
CREATE INDEX idx_dismissed_suggestions_org ON dismissed_suggestions (organization_id);
