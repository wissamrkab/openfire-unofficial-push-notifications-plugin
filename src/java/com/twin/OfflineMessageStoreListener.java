package com.twin;

import com.twin.messagingservices.ApnMessaging;
import com.twin.messagingservices.FcmMessaging;
import org.jivesoftware.openfire.OfflineMessage;
import org.jivesoftware.openfire.OfflineMessageListener;
import org.jivesoftware.openfire.OfflineMessageStore;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class OfflineMessageStoreListener implements OfflineMessageListener {

    private static final int MAXIMUM_NOTIFICATION_NUMBER = 1;
    private static final Logger Log = LoggerFactory.getLogger(UnofficialPushNotificationsPlugin.class);
    private final UserManager userManager;
    JSONParser jsonParser;
    public OfflineMessageStoreListener() {
        userManager = XMPPServer.getInstance().getUserManager();
        jsonParser = new JSONParser();
    }

    private void sendNotification(User userTo, Message msg, String fromName) throws NoSuchAlgorithmException, IOException, URISyntaxException, InvalidKeyException, ParseException, ParseException, InterruptedException {
        String fcmToken = userTo.getProperties().get("fcmToken");
        String apnToken = userTo.getProperties().get("apnToken");



        if (fcmToken != null && !fcmToken.trim().isEmpty()) {

            Log.trace("Prepare FCM notification when one or more massage stored in offline massage {} to {}", msg.getFrom().toBareJID(), msg.getTo().toBareJID());


            FcmMessaging.getInstance().sendCommonMessage(msg.getFrom().getNode() , fcmToken);
        }

        if (apnToken != null && !apnToken.trim().isEmpty()) {
            Log.trace("prepare APN notification when one or more massage stored in offline massage {} to {}", msg.getFrom().toBareJID(), msg.getTo().toBareJID());

            ApnMessaging.getInstance().sendSingleNotification(apnToken);
        }
    }

    @Override
    public void messageBounced(Message message) {
        
    }

    @Override
    public void messageStored(OfflineMessage offlineMessage) {
        
        JID to = offlineMessage.getTo();
        JID from = offlineMessage.getFrom();

        User userTo, userFrom;
        String userFromName;

        try{
            userFrom = userManager.getUser(from.getNode());
            userFromName = userFrom.getName();
        } catch (UserNotFoundException e) {
            userFromName = "MUC (" + XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService("conference").getChatRoom(from.getNode()).getSubject() + ")";
        }

        try {
            userTo = userManager.getUser(to.getNode());
            long offlineMessagesCount = OfflineMessageStore.getInstance().getMessages(to.getNode(), false).size();

            Log.trace("Start process message from {} to {}", userFromName , userTo.getName());
            Log.trace("Message type {}", offlineMessage.getType().toString());
            Log.trace("Message XML: {}", offlineMessage.toXML());
            Log.trace("User {} offline message store count is : {}", userTo.getName(), offlineMessagesCount );

            if ( offlineMessage.getElement().element("shouldSendNotification") != null ) {
                sendNotification(userTo, offlineMessage, userFromName);
                Log.trace("Successfully sent the notification from {} to {}" , userFromName , userTo.getName());
            } else {
                Log.trace("Not sending notification (message don't have a shouldSendNotification extension)");
            }
        } catch (UserNotFoundException | NoSuchAlgorithmException | IOException | URISyntaxException |
                 InvalidKeyException | ParseException e) {
            Log.trace("Receiver is {} - Message not sent to user....stop processing", to.getNode());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
