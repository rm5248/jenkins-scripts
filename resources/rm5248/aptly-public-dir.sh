#!/bin/bash
#
# Print the directory that aptly publishes to: <rootDir>/public
#
# Environment:
#   APTLY_CONFIG   optional aptly config file

set -euo pipefail

root_dir=$(aptly ${APTLY_CONFIG:+-config="$APTLY_CONFIG"} config show 2> /dev/null \
	| sed -n 's/^ *"rootDir": *"\(.*\)",\{0,1\}$/\1/p' || true)
root_dir="${root_dir/#\~/$HOME}"
echo "${root_dir:-$HOME/.aptly}/public"
