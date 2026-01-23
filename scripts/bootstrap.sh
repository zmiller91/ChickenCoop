DB_USER=$(grep '^db_user=' ~/pi_conf/context.txt | cut -d= -f2-)
DB_PASSWORD=$(grep '^db_password=' ~/pi_conf/context.txt | cut -d= -f2-)

sudo mysql <<EOF
CREATE DATABASE IF NOT EXISTS local_pi;
CREATE DATABASE IF NOT EXISTS pi_state;

CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost'
IDENTIFIED BY '${DB_PASSWORD}';

GRANT ALL PRIVILEGES ON local_pi.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON pi_state.* TO '${DB_USER}'@'localhost';

FLUSH PRIVILEGES;
EOF
