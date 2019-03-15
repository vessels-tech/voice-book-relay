#
# VoiceBook Relay
#
PROJECT = "Voicebook Relay"
dir = $(shell pwd)
include /tmp/relay_env 
include .config


env:
	cat ./env/.env.${stage}.sh ./env/env.${stage}.sh > /tmp/relay_env

#
# eg. make switch stage="development"
#
switch:
	@echo switching stage: ${stage}
	echo 'export stage=${stage}\n' > .config
	@make env
	@echo 'Copying google-services.json across'
	@cp ${dir}/app/google-services.${stage}.json ${dir}/app/google-services.json

switch-dev:
	make switch stage="development"

switch-prod:
	make switch stage="production"

build:
	@echo 'Building debug only'
	./env/increment_build.sh
	source ./env/.env.deployment.sh; source /tmp/relay_env; ./gradlew assembleDebug --stacktrace

build-production:
	@make env
	@echo 'Building production only'
	./env/increment_build.sh
	source /tmp/relay_env &&  source ./env/.env.deployment.sh && cd ${dir}/ && ENVFILE=/tmp/relay_env ./gradlew assembleRelease --stacktrace
	@echo "signing with key: '${KEYSTORE_PATH}'"
	cd ${dir}/app/build/outputs/apk/release/ && jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ${KEYSTORE_PATH} app-release-unsigned.apk -storepass ${KEYSTORE_PASSWORD} ${KEYSTORE_ALIAS}
	cd ${dir}/app/build/outputs/apk/release/ && $(ANDROID_HOME)/build-tools/27.0.3/zipalign -f -v 4 app-release-unsigned.apk app-release-signed-aligned.apk
	cd ${dir}/app/build/outputs/apk/release/ && $(ANDROID_HOME)/build-tools/27.0.3/zipalign -c -v 4 app-release-signed-aligned.apk

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

get-sha:
	keytool -list -v -keystore ./env/relay_release.keystore -alias ${KEYSTORE_ALIAS} -storepass ${KEYSTORE_PASSWORD} -keypass ${KEYSTORE_PASSWORD}

#Ref: https://stackoverflow.com/a/49595693/1539479
clean-idea:
	rm -rf .idea
	rm -rf build/
	rm -rf app/build

.PHONY: build build-production env