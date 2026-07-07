alter table escalation_level_users
add column active boolean not null default true;

create table if not exists incident_assigned_users (
    incident_id bigint not null,
    user_id bigint not null,

    primary key (incident_id, user_id),

    constraint fk_incident_assigned_users_incident
        foreign key (incident_id)
        references incidents(id)
        on delete cascade,

    constraint fk_incident_assigned_users_user
        foreign key (user_id)
        references app_users(id)
        on delete cascade
);