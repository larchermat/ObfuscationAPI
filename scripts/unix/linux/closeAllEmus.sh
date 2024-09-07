#!/bin/bash

pids=$(ps -al | grep qemu-system-x86 | awk '{print $4}')

for pid in $pids; do
  kill -9 $pid
done

sleep 10