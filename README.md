

```bash
keytool -genkey -v -keystore production.keystore -alias relay -keyalg RSA -keysize 2048 -validity 10000

keytool -list -v -keystore ./env/relay_release.keystore -alias ${KEYSTORE_ALIAS} -storepass ${KEYSTORE_PASS} -keypass ${KEYSTORE_PASS}

````
