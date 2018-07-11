package org.apis.rpc;

import org.apis.facade.EthereumFactory;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class SSLServer {

    /*
     * Keystore with certificate created like so (in JKS format):
     *
     *keytool -genkey -validity 3650 -keystore "keystore.jks" -storepass "storepassword" -keypass "keypassword" -alias "default" -dname "CN=127.0.0.1, OU=MyOrgUnit, O=MyOrg, L=MyCity, S=MyRegion, C=MyCountry"
     */
    public static void main( String[] args ) throws Exception {
        WebSocketImpl.DEBUG = true;

        RPCServer rpcServer = new RPCServer( 8880 ); // Firefox does allow multible ssl connection only via port 443 //tested on FF16
        //rpcServer.mEthereum = EthereumFactory.createEthereum();

        // load up the key store
        String STORETYPE = "JKS";
        String KEYSTORE = "keystore.jks";
        String STOREPASSWORD = "storepassword";
        String KEYPASSWORD = "keypassword";

        java.net.URL a = SSLServer.class.getClassLoader().getResource(KEYSTORE);



        KeyStore ks = KeyStore.getInstance( STORETYPE );
        File kf = new File( a.toURI() );
        ks.load( new FileInputStream( kf ), STOREPASSWORD.toCharArray() );

        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
        kmf.init( ks, KEYPASSWORD.toCharArray() );
        TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
        tmf.init( ks );

        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );

        rpcServer.setWebSocketFactory( new DefaultSSLWebSocketServerFactory( sslContext ) );

        rpcServer.start();

    }
}