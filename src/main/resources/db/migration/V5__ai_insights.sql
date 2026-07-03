-- Donorly Phase 6: AI & Insights

-- Cached AI-generated insights about donors, campaigns, or the org overall.
CREATE TABLE ai_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('donor','campaign','org')),
    entity_id UUID,
    insight TEXT NOT NULL,
    model VARCHAR(100),
    generated_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_insights_entity ON ai_insights (organization_id, entity_type, entity_id);
CREATE INDEX idx_ai_insights_org ON ai_insights (organization_id, created_at DESC);

-- Log of free-form Q&A conversations with the AI assistant.
CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    question TEXT NOT NULL,
    answer TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','answered','failed')),
    model VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_conversations_org ON ai_conversations (organization_id, created_at DESC);
CREATE INDEX idx_ai_conversations_user ON ai_conversations (organization_id, user_id);
