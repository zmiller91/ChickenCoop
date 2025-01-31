#install mysql
#https://dev.mysql.com/doc/refman/8.4/en/linux-installation-yum-repo.html

wget https://dev.mysql.com/get/mysql84-community-release-el9-1.noarch.rpm
sudo yum localinstall mysql84-community-release-el9-1.noarch.rpm
yes y | sudo yum install mysql-community-server
sudo systemctl start mysqld
PASSWORD=$(sudo grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}')
mysql -u root -p"${PASSWORD}" --connect-expired-password -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '...';"


# install code deploy agent
# https://docs.aws.amazon.com/codedeploy/latest/userguide/codedeploy-agent-operations-install-linux.html
sudo yum update
sudo yum install ruby
sudo yum install wget
cd ~
wget https://aws-codedeploy-us-east-1.s3.us-east-1.amazonaws.com/latest/install
chmod +x ./install
sudo ./install auto
systemctl status codedeploy-agent

