@echo off

call ".\gradlew" "installDist"
call "./build/install/jmm/bin/jmm.bat" %*
