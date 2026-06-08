DO
$$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'epager'
   ) THEN
      CREATE ROLE epager LOGIN PASSWORD 'epager';
   ELSE
      ALTER ROLE epager WITH LOGIN PASSWORD 'epager';
   END IF;
END
$$;

SELECT 'CREATE DATABASE epager OWNER epager'
WHERE NOT EXISTS (
   SELECT FROM pg_database WHERE datname = 'epager'
)\gexec

GRANT ALL PRIVILEGES ON DATABASE epager TO epager;
