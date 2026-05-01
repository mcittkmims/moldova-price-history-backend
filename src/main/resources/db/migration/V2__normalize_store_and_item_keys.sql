create table stores (
    id varchar(40) primary key,
    name varchar(80) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

insert into stores (id, name, created_at, updated_at)
values
    ('darwin', 'Darwin', now(), now()),
    ('enter', 'Enter', now(), now()),
    ('maximum', 'Maximum', now(), now()),
    ('smart', 'Smart.md', now(), now()),
    ('bomba', 'Bomba', now(), now()),
    ('ultra', 'Ultra', now(), now())
on conflict (id) do nothing;

create table products_new (
    id bigserial primary key,
    store_id varchar(40) not null,
    item_id varchar(120) not null,
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
    updated_at timestamp with time zone not null,
    constraint fk_products_new_store
        foreign key (store_id)
        references stores (id),
    constraint uk_products_new_store_item
        unique (store_id, item_id)
);

insert into products_new (
    store_id,
    item_id,
    title,
    category,
    currency,
    availability,
    url,
    image_tone,
    image_url,
    last_scraped_at,
    raw_payload,
    created_at,
    updated_at
)
select
    p.store_id,
    coalesce(nullif(p.source_id, ''), nullif(split_part(p.id, ':', 2), '')),
    p.title,
    p.category,
    p.currency,
    p.availability,
    p.url,
    p.image_tone,
    p.image_url,
    p.last_scraped_at,
    p.raw_payload,
    p.created_at,
    p.updated_at
from products p
where coalesce(nullif(p.source_id, ''), nullif(split_part(p.id, ':', 2), '')) is not null;

create table product_specs_new (
    product_id bigint not null,
    spec_order integer not null,
    spec_value varchar(500) not null,
    primary key (product_id, spec_order),
    constraint fk_product_specs_new_product
        foreign key (product_id)
        references products_new (id)
        on delete cascade
);

insert into product_specs_new (product_id, spec_order, spec_value)
select
    pn.id,
    ps.spec_order,
    ps.spec_value
from product_specs ps
join products p
    on p.id = ps.product_id
join products_new pn
    on pn.store_id = p.store_id
   and pn.item_id = coalesce(nullif(p.source_id, ''), nullif(split_part(p.id, ':', 2), ''));

create table product_prices_new (
    id bigserial primary key,
    product_id bigint not null,
    scraped_at timestamp with time zone not null,
    current_price numeric(19, 2),
    previous_price numeric(19, 2),
    currency varchar(8) not null,
    constraint fk_product_prices_new_product
        foreign key (product_id)
        references products_new (id)
        on delete cascade,
    constraint uk_product_prices_new_product_scraped_at
        unique (product_id, scraped_at)
);

insert into product_prices_new (product_id, scraped_at, current_price, previous_price, currency)
select
    pn.id,
    pp.scraped_at,
    pp.current_price,
    pp.previous_price,
    pp.currency
from product_prices pp
join products p
    on p.id = pp.product_id
join products_new pn
    on pn.store_id = p.store_id
   and pn.item_id = coalesce(nullif(p.source_id, ''), nullif(split_part(p.id, ':', 2), ''));

create index idx_product_prices_new_product_scraped_at
    on product_prices_new (product_id, scraped_at);

drop table product_specs;
drop table product_prices;
drop table products;

alter table products_new rename to products;
alter table product_specs_new rename to product_specs;
alter table product_prices_new rename to product_prices;
alter index uk_products_new_store_item rename to uk_products_store_item;
alter index uk_product_prices_new_product_scraped_at rename to uk_product_prices_product_scraped_at;
alter index idx_product_prices_new_product_scraped_at rename to idx_product_prices_product_scraped_at;
