#! /bin/bash

# get script options
while getopts "12ec" opt ; do
  case $opt in
    1  ) echo "this goes to stdout"
         ;;
    2  ) echo "this goes to stderr" >&2
         ;;
    e  ) exit 1
         ;;
    # this wait for stdin
    c  ) cat
         ;;
  esac
done

exit 0
