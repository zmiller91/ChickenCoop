echo "Executing startup script"

echo "Stopping any running processes"
ps -aux | grep ChickenCoop-1.0-SNAPSHOT.jar | grep -v grep | awk '{print $2}' | xargs sudo kill -9

cd ~/projects/ChickenCoop/scripts
echo "Bootstrapping"
./bootstrap.sh

cd /home/pi/projects/ChickenCoop/target

echo "Starting process"
nohup sudo java -jar ChickenCoop-1.0-SNAPSHOT.jar --spring.profiles.active=pi >> /home/pi/pi-build/log.txt &

