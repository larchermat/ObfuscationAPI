#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "Usage: $0 <name of AVD> <number of devices> <system image for device>"
  exit 1
fi

avdmanager=~/Android/Sdk/cmdline-tools/latest/bin/avdmanager

if [ ! -e "$avdmanager" ]; then
  echo "No avdmanager installed"
  exit 1
fi

i=1

while [ $2 -ge $i ];do
  echo no | "$avdmanager" create avd -n "$1_$i" -k "$3" -f
  i=$((i + 1))
done