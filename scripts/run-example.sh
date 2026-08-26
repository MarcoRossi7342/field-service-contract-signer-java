#!/usr/bin/env sh
set -eu

mvn -q test exec:java
