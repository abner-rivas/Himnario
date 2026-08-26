CREATE TABLE hymns (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NULL,
    lyrics TEXT NULL,
    musical_key VARCHAR(16) NULL,
    musical_mode VARCHAR(16) NULL,
    bpm INTEGER NULL,
    tempo VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT hymns_bpm_positive CHECK (bpm IS NULL OR bpm > 0),
    CONSTRAINT hymns_version_positive CHECK (version > 0),
    CONSTRAINT hymns_tempo_valid CHECK (tempo IS NULL OR tempo IN ('SLOW', 'MEDIUM', 'FAST')),
    CONSTRAINT hymns_status_valid CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT hymns_musical_mode_valid CHECK (
        musical_mode IS NULL OR musical_mode IN ('MAJOR', 'MINOR')
    )
);

CREATE INDEX idx_hymns_status ON hymns (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_hymns_tempo ON hymns (tempo) WHERE deleted_at IS NULL;
CREATE INDEX idx_hymns_musical_key ON hymns (musical_key) WHERE deleted_at IS NULL;
CREATE INDEX idx_hymns_title_lower ON hymns (LOWER(title)) WHERE deleted_at IS NULL;
