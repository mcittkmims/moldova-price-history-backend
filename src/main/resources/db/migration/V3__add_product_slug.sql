alter table products add column slug varchar(255);

update products
set slug = concat(
    coalesce(nullif(trim(both '-' from regexp_replace(lower(title), '[^a-z0-9]+', '-', 'g')), ''), 'product'),
    '-',
    store_id,
    '-',
    coalesce(nullif(trim(both '-' from regexp_replace(lower(item_id), '[^a-z0-9]+', '-', 'g')), ''), 'item')
)
where slug is null;

alter table products alter column slug set not null;
create unique index uk_products_slug on products (slug);
