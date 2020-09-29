#!/bin/sh
set -ex

scp build/libs/beer-fridge-controller-1.0.0-all.jar service:/home/vlada/services/beer-fridge-controller/beer-fridge-controller.jar
ssh service "pkill -9 -f beer-fridge-controller.jar"
