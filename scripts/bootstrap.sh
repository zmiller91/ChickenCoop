DB_USER=$(grep '^db_user=' context.txt | cut -d= -f2-)
DB_PASSWORD=$(grep '^db_password=' context.txt | cut -d= -f2-)

# Create local web database if it doesnt exist
mysql -u "$DB_USER" -p"$DB_PASSWORD" <<EOF
CREATE DATABASE IF NOT EXISTS local_pi;
EOF

# Create pi state database if it doesnt exist
mysql -u "$DB_USER" -p"$DB_PASSWORD" <<EOF
CREATE DATABASE IF NOT EXISTS pi_state;
EOF