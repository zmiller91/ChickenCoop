cd /home/ec2-user/coop

if [ $(ps -aux | grep ChickenCoop-1.0-SNAPSHOT.jar | grep -v grep | awk '{print $2}' | wc -l) -ne 0 ]; then
    ps -aux | grep ChickenCoop-1.0-SNAPSHOT.jar | grep -v grep | awk '{print $2}' | xargs sudo kill -9
fi