export PGPASSWORD=${DB_PASSWORD} && pg_dump -U ${DB_USERNAME} -h ${DB_HOST} ${DB_NAME} >> coccu.sql