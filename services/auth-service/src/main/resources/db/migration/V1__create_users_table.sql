create table users (
    id              uuid                     primary key,
    email           varchar(255)             not null,
    password_hash   varchar(255)             not null,
    role            varchar(50)              not null default 'OWNER',
    created_at      timestamp with time zone not null,
    updated_at      timestamp with time zone not null,

    constraint uk_users_email unique (email),
    constraint chk_users_role check (role in ('OWNER'))
);
