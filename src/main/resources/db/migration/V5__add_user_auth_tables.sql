create table user_accounts (
    id bigserial primary key,
    username varchar(40) not null,
    password_hash varchar(100) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index uk_user_accounts_username on user_accounts (username);

create table user_account_permissions (
    user_account_id bigint not null references user_accounts (id) on delete cascade,
    permission varchar(80) not null,
    primary key (user_account_id, permission)
);
