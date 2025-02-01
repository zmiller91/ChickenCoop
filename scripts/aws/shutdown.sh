cd /home/ec2-user/coop


ps -aux | grep ChickenCoop-1.0-SNAPSHOT.jar | grep -v grep | awk '{print $2}' | if [ -s /dev/stdin ]; then
  xargs sudo kill -9
fi