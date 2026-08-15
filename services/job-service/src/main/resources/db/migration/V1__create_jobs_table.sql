CREATE TABLE jobs (
    id              UUID            PRIMARY KEY,
    customer_name   VARCHAR(255)    NOT NULL,
    site_address    VARCHAR(500)    NOT NULL,
    description     TEXT,
    price           NUMERIC(12, 2),
    status          VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    paid            BOOLEAN         NOT NULL DEFAULT FALSE,
    start_date      DATE,
    end_date        DATE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_jobs_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED'))
);
