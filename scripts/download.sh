#!/bin/bash
set -e

./setup.sh

cd /home/pi/projects/ChickenCoop
git checkout pi
git remote update
if [ $(git rev-parse HEAD) != $(git rev-parse @{u}) ]; then
	echo "Git workspace out of date. Updating."
	git pull origin pi --rebase
else
	echo "Git workspace up to date."
fi


INSTALLED=$(cat ~/pi-build/installed.txt)
TARGET=$(git rev-parse HEAD)
if [ "$INSTALLED" != "$TARGET" ]; then
	echo "Build out of date. Updating."
  sudo mvn -Plocal clean package
	cp target/ChickenCoop-1.0-SNAPSHOT.jar ~/pi-build/
	echo $TARGET > ~/pi-build/installed.txt
	echo "Updated to ${TARGET}"
else
	echo "Build up to date."
fi


