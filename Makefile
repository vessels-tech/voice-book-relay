#
# VoiceBook Relay
#
PROJECT = "Voicebook Relay"
dir = $(shell pwd)
include /tmp/relay_env 
include .config


env:
	cat ./env/.env.deployment.sh ./env/.env.${stage}.sh ./env/env.${stage}.sh > /tmp/relay_env

#
# eg. make switch stage="development"
#
switch:
	@echo switching stage: ${stage}
	@echo 'export stage=${stage}\n' > .config
	@make env
	@echo 'Copying google-services.json across'
	@cp ${dir}/app/google-services.${stage}.json ${dir}/app/google-services.json


switch-dev:
	make stage="development"

switch-prod:
	make stage="production"

build:
	@echo 'Building debug only'
	./env/increment_build.sh
	source /tmp/relay_env; ./gradlew assembleDebug --stacktrace

install-android:
	cd ${dir}/app/build/outputs/apk/debug/ && adb install app-debug.apk

install-android-production:
	cd ${dir}/app/build/outputs/apk/release/ && adb install app-release-unsigned.apk

build-and-install:
	@make build install-android

build-and-install-production:
	@make build-production install-android-production

hockey:
	source ${dir}/env/.env.deployment.sh && \
	cd ${dir}/fastlane && \
	fastlane upload_hockey

inspect-apk:
	cd ${ANDROID_HOME}/build-tools/27.0.3/; ./aapt dump badging /Users/ldaly/developer/vessels/tz/simple-phone/app/build/outputs/apk/debug/app-debug.apk

#Ref: https://stackoverflow.com/a/49595693/1539479
clean-idea:
	rm -rf .idea
	rm -rf build/
	rm -rf app/build

.PHONY: build build-production env