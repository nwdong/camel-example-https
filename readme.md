# concepts
## one-way authentication (TLS)
On client side, client requests to verify server certificate to make sure the server is really the server expected.
A truststore is NOT required if the cert is signed by a valid CA. If the certificate received by a TLS client is signed by a valid CA, then the client makes a request to the CA to authenticate the certificate.
## mutual authentication (TLS)
server also requests to verify client certificate to tell if the client is allowed. 
It's controlled on server side.

# how to build
```
mvn clean install
```
# how to run server
```
mvn spring-boot:run
```

# curl test
go to the folder ./doc, which contains required keystore and key files, then
## run without server authentication
```
curl -X GET -H "Content-Type: application/json" --insecure https://localhost:8888/dnwtest/hello
```
## run with server authentication
```
curl -X GET -H "Content-Type: application/json" --cacert dnw-server.cer.pem --key dnw-client.p12:password https://localhost:8888/dnwtest/hello
```
Note: client requests server certificate authentication. And it's mutual unless server requests authentication as well, it's controlled on server side.
## output
```
your input info is hello
```
Note: in HttpsServerRoute, you can see code "scsp.setClientAuthentication("NONE");", which means actually we didn't do any client authentication. If it's changed to "REQUIRE", there will be client authentication, but now there will be error "fatal error: 42: null cert chain" since the client certificate is self-signed. If we could get client certificate signed by valid CA and import it with its intermediate and root certificates into server keystoe (or dedicated truststore), then it would work.

# keystores and self-signed certificates generation (using "password" for all keystore and private key passcode)
## generate server keystore (remember copy it to src\main\resources)
```
// On client side, when visiting the server with server authentiation, the CN in server certificate will be checked if the same as the domain name in URL.
// so, using "localhost" here for local test. Otherwise, there will error as follows:
// ... certificate subject name '<what you enter for CN here>' does not match target host name 'localhost'
keytool -genkeypair -keyalg rsa -keysize 2048 -alias dnw-ssl-server -keystore dnw-server.jks -dname "CN=localhost" -validity 3650
```
## export server self-signed certificate
```
//export to pem format for curl
keytool -exportcert -alias dnw-ssl-server -file dnw-server.cer.pem -rfc -keystore dnw-server.jks
```
## generate client keystore
```
keytool -genkeypair -keyalg rsa -keysize 2048 -alias dnw-ssl-client -keystore dnw-client.jks -dname "CN=theclient" -validity 3650
//convert to p12 for curl, and only client private key required in the store
keytool -importkeystore -srckeystore dnw-client.jks -destkeystore dnw-client.p12 -srcalias dnw-ssl-client -srcstoretype jks -deststoretype pkcs12
```
## export client certificate
```
keytool -exportcert -keystore dnw-client.jks -alias dnw-ssl-client -file dnw-client.cer
```
## import client certificate into server keystore (or the dedicated truststore)
```
keytool -import -alias dnw-ssl-client -file dnw-client.cer -keystore dnw-server.jks
```
Note: actually client certificate is not used in this test. In real world, needs to export csr from client keystore, then get signed by valid CA, finally import the client certificate with all certificates in the chain into the server keystore/truststore.
## if the client is application using keystore/truststore and server certificate is not signed by valid CA, import server certificate into client keystore (or the dedicated truststore)
```
// not required if using curl as above since the server certificate file path has been in command
keytool -import -alias dnw-ssl-server -file dnw-server.cer.pem -keystore dnw-client.jks
```
