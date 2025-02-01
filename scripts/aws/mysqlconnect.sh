SECRET_STRING=$(aws secretsmanager get-secret-value --secret-id MysqlInstanceSecretC144D3B7-2LUy4Xg4043M --query SecretString --output text)
HOST=$(echo $SECRET_STRING | jq -r .host)
USERNAME=$(echo $SECRET_STRING | jq -r .username)
PASSWORD=$(echo $SECRET_STRING | jq -r .password)

mysql -u $USERNAME -p"${PASSWORD}" -h "${HOST}"