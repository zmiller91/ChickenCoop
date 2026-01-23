cd /home/ec2-user/coop
nohup sudo java -jar ChickenCoop-1.0-SNAPSHOT.jar --spring.profiles.active=aws > /dev/null 2> /dev/null < /dev/null &