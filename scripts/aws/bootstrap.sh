#!/usr/bin/env bash
set -euo pipefail
cd /home/ec2-user/coop

# root creds (used ONLY for bootstrap)
ROOT_SECRET_STRING=$(aws secretsmanager get-secret-value --secret-id MysqlInstanceSecretC144D3B7-aU4JVlDRkqeW --query SecretString --output text)
HOST=$(echo $ROOT_SECRET_STRING | jq -r .host)
DB_ROOT_USER=$(echo $ROOT_SECRET_STRING | jq -r .username)
DB_ROOT_PASSWORD=$(echo $ROOT_SECRET_STRING | jq -r .password)

# app creds (can be same or different; your choice)
APP_SECRET_STRING=$(aws secretsmanager get-secret-value --secret-id app-user-creds --query SecretString --output text)
APP_DB_USER=$(echo $APP_SECRET_STRING | jq -r .username)
APP_DB_PASSWORD=$(echo $APP_SECRET_STRING | jq -r .password)

# databases
DB_WEB="web"

echo "Bootstrapping MySQL databases..."

mysql -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" -h "${HOST}" <<EOF
-- create databases
CREATE DATABASE IF NOT EXISTS \`${DB_WEB}\`;

-- create app user if needed
CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'10.%'
IDENTIFIED BY '${APP_DB_PASSWORD}';

-- grant access
GRANT ALL PRIVILEGES ON \`${DB_WEB}\`.* TO '${APP_DB_USER}'@'10.%';

FLUSH PRIVILEGES;
EOF

echo "MySQL bootstrap complete."
