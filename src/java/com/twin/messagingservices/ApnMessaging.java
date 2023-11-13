package com.twin.messagingservices;

import com.eatthepath.pushy.apns.*;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;


public class ApnMessaging {

    private static final Logger Log = LoggerFactory.getLogger(ApnMessaging.class);

    private ApnsClient apnsClient;
    private static ApnMessaging apnMessaging = null;

    //@Param({"10000"})
    public int notificationCount;

//    @Param({"1", "4", "8"})
    public int concurrentConnections;

    private NioEventLoopGroup clientEventLoopGroup ;
    private NioEventLoopGroup serverEventLoopGroup ;


    private static final String CA_CERTIFICATE_FILENAME = "/ca.pem";
    private static final String SERVER_CERTIFICATES_FILENAME = "/server_certs.pem";
    private static final String SERVER_KEY_FILENAME = "/server_key.pem";

    private static final String P8FILEPATH = "/AuthKey_HK488R4F75.p8";
    private static final String TOPIC = "com.futchat.dib.voip";
    private static final String TEAM_ID = "LUL848QZ5J";
    private static final String KEY_ID = "HK488R4F75";
    private static final int TOKEN_LENGTH = 32;
    private static final int MESSAGE_BODY_LENGTH = 1024;

    private static final String HOST = "localhost";
    private static final int PORT = 8443;

    private static final int KEY_SIZE = 256;

    private ApnMessaging() throws NoSuchAlgorithmException, IOException, InvalidKeyException, URISyntaxException {
        setUp();
    }

    public static ApnMessaging getInstance() throws NoSuchAlgorithmException, IOException, InvalidKeyException, URISyntaxException {
        if (apnMessaging == null)
            apnMessaging = new ApnMessaging();
        return apnMessaging ;
    }

    public void setUp() throws NoSuchAlgorithmException, InvalidKeyException, IOException, URISyntaxException {

        Log.debug("ApnMessaging : initializing apn client.");

//        final ApnsSigningKey signingKey;
//        {
//            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
//            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
//
//            signingKey = new ApnsSigningKey(KEY_ID, TEAM_ID, (ECPrivateKey) keyPairGenerator.generateKeyPair().getPrivate());
//        }



        File p8File = null;
        try {
            InputStream in = this.getClass().getResourceAsStream("/AuthKey_HK488R4F75.p8");
            File tempFile = File.createTempFile("AuthKey_HK488R4F75","p8");
            FileOutputStream out = new FileOutputStream(tempFile);
            IOUtils.copy(in, out);
            p8File = tempFile ;
            Log.debug("CallbackOnOffline : temp file exist : {}" , tempFile.exists());
        } catch (IOException e) {
            Log.debug("CallbackOnOffline : temp file failed to init file p8 not found ");
            throw new RuntimeException(e);
        }
//        Log.debug("ApnMessaging : File123 {}" , ApnMessaging.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + P8FILEPATH);
        this.apnsClient = new ApnsClientBuilder()
            .setApnsServer("api.sandbox.push.apple.com")
            .setSigningKey(ApnsSigningKey.loadFromPkcs8File(p8File, TEAM_ID , KEY_ID))
            .build();
        Log.debug("ApnMessaging : Apn client ready to use.");
    }

    public PushNotificationFuture<SimpleApnsPushNotification
        , PushNotificationResponse<SimpleApnsPushNotification>> sendSingleNotification(String devicetoken){

        Log.debug("ApnMessaging : start sending notification ");

        ApnsPayloadBuilder apnsPayloadBuilder = new SimpleApnsPayloadBuilder();
        String payload = apnsPayloadBuilder.setAlertBody("{\"uuid\":\"982cf533-7b1b-4cf6-a6e0-004aab68c503\",\"incoming_caller_id\":\"0123456789\",\"incoming_caller_name\":\"Tester\"}").build();
        Log.debug("ApnMessaging : apn payload builder : {} ",apnsPayloadBuilder);
        SimpleApnsPushNotification simpleNotification = new SimpleApnsPushNotification(devicetoken , TOPIC , payload, Instant.now().plusSeconds(86400), DeliveryPriority.IMMEDIATE, PushType.VOIP );

        Log.debug("ApnMessaging : apn notification prepared to send : {} ",simpleNotification);
        PushNotificationFuture<SimpleApnsPushNotification
            , PushNotificationResponse<SimpleApnsPushNotification>> response
            = this.apnsClient.sendNotification(simpleNotification);
        response.whenComplete((respons, cause) -> {
            if (respons != null) {

                if (respons.isAccepted()) {
                    Log.debug("ApnMessaging : Push notification accepted by APNs gateway : {}." , respons);
                } else {
                    Log.debug("ApnMessaging : Notification rejected by the APNs gateway: {} .", respons.getRejectionReason() );
                    System.out.println("Notification rejected by the APNs gateway: " +
                        respons.getRejectionReason());

                    respons.getTokenInvalidationTimestamp().ifPresent(timestamp -> {
                        System.out.println("\t…and the token is invalid as of " + timestamp);
                    });
                }
            } else {
                    Log.debug("ApnMessaging : Notification rejected by exception : {} .", (Object) cause.getStackTrace());
                // Something went wrong when trying to send the notification to the
                // APNs server. Note that this is distinct from a rejection from
                // the server, and indicates that something went wrong when actually
                // sending the notification or waiting for a reply.
                cause.printStackTrace();
            }
        });
//        Log.debug("ApnMessaging : Apn client response : {} ." , response);


        return response;


    }



//    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidKeyException, URISyntaxException {
////        ApnMessaging.getInstance().sendSingleNotification("375676a1057dcace4450e3c6e63e8a3e87ec45dbfe6ebb06e473e2bb7688200d");
//        System.out.println(f.getAbsolutePath());
//        System.out.println(f.exists());
//
//
////        File f = new File("C:\\Users\\Jehad\\IdeaProjects\\Twin.Openfire.Plugin.CallbackOnOffline\\src\\j.txt");
//
//
//    }

}
