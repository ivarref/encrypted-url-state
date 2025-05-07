#!/usr/bin/env bash

if [[ "$1" == "--entr" ]]; then
#  printf '\033[3J' # clear scrollback
#  printf '\033[2J' # clear whole screen without moving the cursor
#  printf '\033[H' # move cursor to top left of the screen
  # shellcheck disable=SC2046
  if grep -q "deftest \^:focus" $(find "test" -name '*.clj'); then
    clj -X:test :includes '[:focus]'
  else
    clj -X:test :excludes '[:ignore]'
  fi
else
  git ls-files | entr -ccr "$0" --entr
fi
