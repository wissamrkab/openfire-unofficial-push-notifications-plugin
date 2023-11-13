package com.twin.messagingservices;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.twin.UnofficialPushNotificationsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;

public class FcmMessaging {

    private static FcmMessaging fcmMessaging = null ;
    private static final String PROJECT_ID = "YOUR_PROJECT_ID";
    private static final String BASE_URL = "https://fcm.googleapis.com";
    private static final String FCM_SEND_ENDPOINT = "/v1/projects/" + PROJECT_ID + "/messages:send";

    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String[] SCOPES = { MESSAGING_SCOPE };

    private static final String TITLE = "futchat";
    public static final String MESSAGE_KEY = "message";

    private static final Logger Log = LoggerFactory.getLogger(UnofficialPushNotificationsPlugin.class);
    private final String key = "YOUR_FCM_KEY_HERE";


    private FcmMessaging(){

    }
    public static FcmMessaging getInstance(){
        if (fcmMessaging == null)
            fcmMessaging = new FcmMessaging();
        return fcmMessaging;
    }

    /**
     * Retrieve a valid access token that can be use to authorize requests to the FCM REST
     * API.
     *
     * @return Access token.
     * @throws IOException
     */
    // [START retrieve_access_token]
    private String getAccessToken() throws IOException {
        InputStream targetStream = new ByteArrayInputStream(key.getBytes());
        GoogleCredentials googleCredentials = GoogleCredentials
            .fromStream(targetStream)
            .createScoped(Arrays.asList(SCOPES));
        googleCredentials.refresh();
        return googleCredentials.getAccessToken().getTokenValue();
    }
    // [END retrieve_access_token]

    /**
     * Create HttpURLConnection that can be used for both retrieving and publishing.
     *
     * @return Base HttpURLConnection.
     * @throws IOException
     */
    private HttpURLConnection getConnection() throws IOException {
        // [START use_access_token]
        URL url = new URL(BASE_URL + FCM_SEND_ENDPOINT);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8");
        return httpURLConnection;
        // [END use_access_token]
    }

    /**
     * Send request to FCM message using HTTP.
     * Encoded with UTF-8 and support special characters.
     *
     * @param fcmMessage Body of the HTTP request.
     * @throws IOException
     */
    private void sendMessage(JsonObject fcmMessage) throws IOException {
        HttpURLConnection connection = getConnection();
        connection.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
        writer.write(fcmMessage.toString());
        writer.flush();
        writer.close();

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            String response = inputstreamToString(connection.getInputStream());
            Log.debug("CallbackOnOffline : Message sent to Firebase for delivery with response: {}", response);

        } else {
            String response = inputstreamToString(connection.getErrorStream());
            Log.debug("CallbackOnOffline : Unable to send message to Firebase with response : {}", response);
        }
    }

    /**
     * Send a message that uses the common FCM fields to send a notification message to all
     * platforms. Also platform specific overrides are used to customize how the message is
     * received on Android and iOS.
     *
     * @throws IOException
     */

    /**
     * Build the body of an FCM request. This body defines the common notification object
     * as well as platform specific customizations using the android and apns objects.
     *
     * @return JSON representation of the FCM request body.
     */

    /**
     * Build the android payload that will customize how a message is received on Android.
     *
     * @return android payload of an FCM request.
     */
    private JsonObject buildAndroidOverridePayload() {
        JsonObject androidNotification = new JsonObject();
        androidNotification.addProperty("click_action", "android.intent.action.MAIN");

        JsonObject androidNotificationPayload = new JsonObject();
        androidNotificationPayload.add("notification", androidNotification);

        return androidNotificationPayload;
    }

    /**
     * Build the apns payload that will customize how a message is received on iOS.
     *
     * @return apns payload of an FCM request.
     */
    private JsonObject buildApnsHeadersOverridePayload() {
        JsonObject apnsHeaders = new JsonObject();
        apnsHeaders.addProperty("apns-priority", "10");

        return apnsHeaders;
    }

    /**
     * Build aps payload that will add a badge field to the message being sent to
     * iOS devices.
     *
     * @return JSON object with aps payload defined.
     */
    private JsonObject buildApsOverridePayload() {
        JsonObject badgePayload = new JsonObject();
        badgePayload.addProperty("badge", 1);

        JsonObject apsPayload = new JsonObject();
        apsPayload.add("aps", badgePayload);

        return apsPayload;
    }

    /**
     * Send notification message to FCM for delivery to registered devices.
     *
     * @throws IOException
     */
    public void sendCommonMessage( String from   , String userToken) throws IOException {

        JsonObject notificationMessage = buildNotificationMessage(from  , userToken);
        Log.debug("CallbackOnOffline : FCM notification object is : {}", notificationMessage.toString());
        sendMessage(notificationMessage);
    }

    /**
     * Construct the body of a notification message request.
     *
     * @return JSON of notification message.
     */
    private JsonObject buildNotificationMessage( String from ,  String userToken) {
        JsonObject jNotification = new JsonObject();
        jNotification.addProperty("title", TITLE);
//        jNotification.addProperty("body", body);
        jNotification.addProperty("tag", "Twin");

        JsonObject Jandroid = new JsonObject();
        Jandroid.addProperty("priority" , "high" );
//        Jandroid.addProperty("collapseKey" , "Twin");
      //  Jandroid.add("notification", jNotification);

        JsonObject Jdata = new JsonObject();
        Jdata.addProperty("param0" , from);
//        Jdata.addProperty("param1" , body);
        Jdata.addProperty("eventType" , "NewMessage");

        JsonObject jMessage = new JsonObject();
        jMessage.addProperty("token", userToken);
        jMessage.add("data" , Jdata);
        jMessage.add("android" , Jandroid);


        JsonObject jFcm = new JsonObject();
        jFcm.add(MESSAGE_KEY, jMessage);

        return jFcm;
    }

    /**
     * Read contents of InputStream into String.
     *
     * @param inputStream InputStream to read.
     * @return String containing contents of InputStream.
     * @throws IOException
     */
    private String inputstreamToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine());
        }
        return stringBuilder.toString();
    }

    /**
     * Pretty print a JsonObject.
     *
     * @param jsonObject JsonObject to pretty print.
     */
    private void prettyPrint(JsonObject jsonObject) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(jsonObject) + "\n");
    }

//    public static void main(String[] args) {
////        this.prettyPrint(this.buildNotificationMessage("from" , "body" , "token"));
//    }


}
