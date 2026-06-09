update app_users
set password_hash = '$2a$10$DTrJ3.WtUt9omIdgHZQpeeX6mlyFYDplTNnRBrEtdgi9ww.Iwazha'
where lower(email) in (
    'admin@epager.local',
    'shivam.engineer@example.com',
    'ravi.lead@example.com',
    'manish.manager@example.com'
);
