create table jobs (
    id              uuid             primary key,
    customer_name   varchar(255)     not null,
    site_address    varchar(500)     not null,
    description     text,
    price           numeric(12, 2),
    status          varchar(50)      not null default 'PENDING',
    paid            boolean          not null default false,
    start_date      date,
    end_date        date,
    created_at      timestamp with time zone not null,
    updated_at      timestamp with time zone not null,

    constraint chk_jobs_status check (status in ('PENDING', 'IN_PROGRESS', 'COMPLETED'))
);
