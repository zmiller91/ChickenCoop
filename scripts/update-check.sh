cd ~/projects/ChickenCoop/scripts

OLD=$(cat ~/pi-build/installed.txt)
./download.sh
NEW=$(cat ~/pi-build/installed.txt)
if [ "$OLD" != "$NEW" ]; then
        "New update found, restarting"
 	./restart.sh
fi       


