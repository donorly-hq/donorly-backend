-- Donorly Phase 5: Communications (templates + outbound message log)

CREATE TABLE message_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(200) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'email'
        CHECK (channel IN ('email', 'sms')),
    subject VARCHAR(300),
    body TEXT NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_message_templates_org ON message_templates (organization_id);

CREATE TABLE communication_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    channel VARCHAR(20) NOT NULL
        CHECK (channel IN ('email', 'sms')),
    recipient VARCHAR(255) NOT NULL,
    donor_id UUID REFERENCES donors(id),
    template_id UUID REFERENCES message_templates(id),
    subject VARCHAR(300),
    body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'queued'
        CHECK (status IN ('queued', 'sent', 'failed', 'skipped')),
    error_message TEXT,
    sent_by UUID REFERENCES users(id),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_communication_messages_org ON communication_messages (organization_id);
CREATE INDEX idx_communication_messages_org_created ON communication_messages (organization_id, created_at DESC);
CREATE INDEX idx_communication_messages_donor ON communication_messages (donor_id);
