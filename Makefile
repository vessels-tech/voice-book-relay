#
# VoiceBook Relay
#

PROJECT = "Voicebook Relay"
dir = $(shell pwd)


build:
	@echo 'Building debug only'
	./gradlew assembleDebug --stacktrace

build-production:
	@echo 'Building production only'
	./gradlew assembleRelease --stacktrace
	@echo 'signing with debug key'
	cd ${dir}/app/build/outputs/apk/release/ && jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/.android/debug.keystore  app-release-unsigned.apk -storepass android androiddebugkey

install-android:
	cd ${dir}/app/build/outputs/apk/debug/ && adb install app-debug.apk

install-android-production:
	cd ${dir}/app/build/outputs/apk/release/ && adb install app-release-unsigned.apk

build-and-install:
	@make build install-android

build-and-install-production:
	@make build-production install-android-production

hockey:
	source ../env/.env.deployment.sh && \
	cd ${dir}/android/fastlane && \
	fastlane upload_hockey


.PHONY: build build-production