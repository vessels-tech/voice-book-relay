#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ENV_DIR="$DIR/../../../env/"
source "$ENV_DIR"/.env.deployment.sh
fastlane beta 