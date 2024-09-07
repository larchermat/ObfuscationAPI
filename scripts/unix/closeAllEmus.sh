#!/bin/bash

pids=$(ps -al | grep qemu-system-x86 | awk '{print $4}')

if [ -z "$pids" ]; then
  for pid in $pids; do
    kill -9 "$pid"
  done
fi

sleep 10