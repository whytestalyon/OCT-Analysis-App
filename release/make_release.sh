#!/bin/bash
if [ $# -ne 1 ] 
then
	echo Must supply version number to make the release packages
	exit 1
fi
cp posix-release.sh ora_v$1.sh
cat ../dist/OCT_Analysis_Application.jar >> ora_v$1.sh
cp ms-release.bat ora_v$1.bat
cat ../dist/OCT_Analysis_Application.jar >> ora_v$1.bat
