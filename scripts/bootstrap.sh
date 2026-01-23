#!/usr/bin/env bash
set -euo pipefail

CONTEXT="$HOME/pi_conf/context.txt"

# helper to read key=value from context.txt
get_ctx() {
  grep "^$1=" "$CONTEXT" | cut -d= -f2-
}

# root creds (used ONLY for bootstrap)
DB_ROOT_USER=$(get_ctx db_root)
DB_ROOT_PASSWORD=$(get_ctx db_root_password)

# app creds (can be same or different; your choice)
APP_DB_USER=$(get_ctx db_user)
APP_DB_PASSWORD=$(get_ctx db_password)

# databases
DB_WEB="local_pi"
DB_PI="pi_state"

echo "Bootstrapping MySQL databases..."

mysql -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" <<EOF
-- create databases
CREATE DATABASE IF NOT EXISTS \`${DB_WEB}\`;
CREATE DATABASE IF NOT EXISTS \`${DB_PI}\`;

-- create app user if needed
CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'localhost'
IDENTIFIED BY '${APP_DB_PASSWORD}';

-- grant access
GRANT ALL PRIVILEGES ON \`${DB_WEB}\`.* TO '${APP_DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON \`${DB_PI}\`.* TO '${APP_DB_USER}'@'localhost';

FLUSH PRIVILEGES;
EOF

echo "MySQL bootstrap complete."
