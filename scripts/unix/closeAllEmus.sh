#!/bin/bash

pids=$(ps -al | grep qemu-system-x86 | awk '{print $4}')

if [ -n "$pids" ]; then
  for pid in $pids; do
    kill -9 "$pid"
  done
fi

pids=$(ps -al | grep resize2fs | awk '{print $4}')

if [ -n "$pids" ]; then
  for pid in $pids; do
    kill -9 "$pid"
  done
fi

pids=$(ps -al | grep emulator | awk '{print $4}')

if [ -n "$pids" ]; then
  for pid in $pids; do
    kill -9 "$pid"
  done
fi

sleep 10