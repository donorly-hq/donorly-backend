-- Donorly Phase 3: Donor 360 & Finance

-- Individual payment transactions recorded against pledges.
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    pledge_id UUID NOT NULL REFERENCES pledges(id),
    donor_id UUID NOT NULL REFERENCES donors(id),
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(50),
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    reference VARCHAR(200),
    notes TEXT,
    recorded_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_org ON payments (organization_id, created_at DESC);
CREATE INDEX idx_payments_pledge ON payments (pledge_id);
CREATE INDEX idx_payments_donor ON payments (organization_id, donor_id);

-- Tax / acknowledgement receipts issued for payments.
CREATE TABLE receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    payment_id UUID NOT NULL REFERENCES payments(id),
    receipt_number VARCHAR(50) NOT NULL,
    issued_to VARCHAR(200) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    issued_by UUID REFERENCES users(id),
    UNIQUE (organization_id, receipt_number)
);
CREATE INDEX idx_receipts_org ON receipts (organization_id, issued_at DESC);
