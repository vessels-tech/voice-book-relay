#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

lastBuildNumber=`cat "$DIR"/buildnumber`
echo -n "$((lastBuildNumber + 1))" > "$DIR"/buildnumber