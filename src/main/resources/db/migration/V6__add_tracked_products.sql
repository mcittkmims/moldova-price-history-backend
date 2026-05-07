create table tracked_products (
    id bigserial primary key,
    user_account_id bigint not null references user_accounts (id) on delete cascade,
    product_id bigint not null references products (id) on delete cascade,
    created_at timestamp with time zone not null
);

create unique index uk_tracked_products_user_product
    on tracked_products (user_account_id, product_id);

create index idx_tracked_products_product_id
    on tracked_products (product_id);
