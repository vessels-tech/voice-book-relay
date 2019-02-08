#
# VoiceBook Relay
#

PROJECT = "Voicebook Relay"
dir = $(shell pwd)


build:
	@echo 'Building debug only'
	./gradlew assembleDebug --stacktrace

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
	cd ${dir}/android/fastlane && \
	fastlane upload_hockey


.PHONY: build build-production