ps -aux | grep ChickenCoop-1.0-SNAPSHOT.jar | grep -v grep | awk '{print $2}' | xargs sudo kill -9