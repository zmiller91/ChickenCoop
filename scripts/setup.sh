if [ -d ~/pi-build ]; then
	echo "Build directory already exists"
else
	echo "Making build directory"
	mkdir ~/pi-build
fi

if [ -f ~/pi-build/installed.txt ]; then
	echo "installed.txt file already exists"
else 
	echo "Creating installed.txt file"
	touch ~/pi-build/installed.txt
fi

if [ -f ~/pi-build/log.txt ]; then
	echo "Log file already created."
else
	echo "Creating log file"
	touch ~/pi-build/log.txt
fi


