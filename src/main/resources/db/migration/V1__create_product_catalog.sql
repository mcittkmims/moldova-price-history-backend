create table products (
    id varchar(120) primary key,
    store_id varchar(40) not null,
    store_name varchar(80) not null,
    source_id varchar(120),
    title varchar(255) not null,
    category varchar(80) not null,
    currency varchar(8) not null,
    availability varchar(40) not null,
    url varchar(1000) not null,
    image_tone varchar(16) not null,
    image_url varchar(1000),
    last_scraped_at timestamp with time zone not null,
    raw_payload text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table product_specs (
    product_id varchar(120) not null,
    spec_order integer not null,
    spec_value varchar(500) not null,
    primary key (product_id, spec_order),
    constraint fk_product_specs_product
        foreign key (product_id)
        references products (id)
        on delete cascade
);

create table product_prices (
    id bigserial primary key,
    product_id varchar(120) not null,
    scraped_at timestamp with time zone not null,
    current_price numeric(19, 2),
    previous_price numeric(19, 2),
    currency varchar(8) not null,
    constraint fk_product_prices_product
        foreign key (product_id)
        references products (id)
        on delete cascade,
    constraint uk_product_prices_product_scraped_at
        unique (product_id, scraped_at)
);

create index idx_product_prices_product_scraped_at
    on product_prices (product_id, scraped_at);
