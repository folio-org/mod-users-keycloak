TRUNCATE TABLE testtenant_mod_users_keycloak.user_migration_job CASCADE;

INSERT INTO testtenant_mod_users_keycloak.user_migration_job (id, status, total_records, started_at)
VALUES ('9971c946-c449-46b6-968b-77b66280b044', 'IN_PROGRESS',
        20, TIMESTAMP WITH TIME ZONE '2023-01-02 12:01:01+04');
