SECRET_STRING=$(aws secretsmanager get-secret-value --secret-id MysqlInstanceSecretC144D3B7-aU4JVlDRkqeW --query SecretString --output text)
HOST=$(echo $SECRET_STRING | jq -r .host)
USERNAME=$(echo $SECRET_STRING | jq -r .username)
PASSWORD=$(echo $SECRET_STRING | jq -r .password)

cd /home/ec2-user/coop
mysql -u $USERNAME -p"${PASSWORD}" -h "${HOST}" < sql/shared/V1__init_web_tables.sql