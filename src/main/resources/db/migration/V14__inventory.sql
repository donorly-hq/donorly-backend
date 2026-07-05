-- Inventory module: org-defined equipment with per-unit accountability.
-- Each item has a quantity; individual units (1..quantity) are checked out
-- to a holder and checked back in, so "who has standee #3" is answerable.

CREATE TABLE inventory_items (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID          NOT NULL REFERENCES organizations(id),
    name            VARCHAR(200)  NOT NULL,
    category        VARCHAR(100),
    quantity        INT           NOT NULL DEFAULT 1 CHECK (quantity >= 1),
    notes           TEXT,
    -- audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID          REFERENCES users(id),
    modified_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modified_by     UUID          REFERENCES users(id)
);
CREATE INDEX idx_inventory_items_org ON inventory_items (organization_id);

CREATE TABLE inventory_assignments (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id      UUID         NOT NULL REFERENCES organizations(id),
    item_id              UUID         NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    unit_number          INT          NOT NULL CHECK (unit_number >= 1),
    holder_user_id       UUID         REFERENCES users(id),
    holder_name          VARCHAR(200),
    assigned_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expected_return_date DATE,
    returned_at          TIMESTAMPTZ,
    notes                TEXT,
    -- audit
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by           UUID         REFERENCES users(id),
    modified_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_by          UUID         REFERENCES users(id)
);
CREATE INDEX idx_inventory_assignments_item ON inventory_assignments (item_id);
CREATE INDEX idx_inventory_assignments_org ON inventory_assignments (organization_id);
-- One active (unreturned) assignment per physical unit.
CREATE UNIQUE INDEX uq_inventory_active_unit
    ON inventory_assignments (item_id, unit_number)
    WHERE returned_at IS NULL;
