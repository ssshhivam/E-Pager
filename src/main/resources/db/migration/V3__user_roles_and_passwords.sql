alter table app_users
    add column if not exists password_hash varchar(255);

alter table app_users
    add column if not exists role varchar(255);

update app_users
set password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
where password_hash is null or password_hash = '';

update app_users
set role = case
    when lower(email) = 'admin@epager.local' then 'ADMIN'
    when lower(email) in ('ravi.lead@example.com', 'manish.manager@example.com') then 'MANAGER'
    else 'ENGINEER'
end
where role is null or role = '';

alter table app_users
    alter column password_hash set not null;

alter table app_users
    alter column role set not null;

insert into app_users(name, email, phone_number, password_hash, role)
select 'E-Pager Admin',
       'admin@epager.local',
       '+10000000000',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'ADMIN'
where not exists (
    select 1 from app_users where lower(email) = 'admin@epager.local'
);
