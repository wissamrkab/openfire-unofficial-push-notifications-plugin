package com.twin.handlers;

import com.twin.UnofficialPushNotificationsPlugin;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;

import java.util.Iterator;

public class IQTokenHandler extends IQHandler {
    private static final Logger Log = LoggerFactory.getLogger(UnofficialPushNotificationsPlugin.class);
    private final UserManager userManager;
    public IQTokenHandler() {
        super("TokenIQHandler");
        userManager = XMPPServer.getInstance().getUserManager();
    }

    @Override
    public IQ handleIQ(IQ iq) {
        try {
            User user = userManager.getUser(iq.getFrom());

            Log.trace("Start processing a Token Updater IQ from {}", user.getName());
            if(iq.getType().equals(IQ.Type.set))
            {
                Log.trace("IQ Type : Set");
                for (Iterator<Element> i = iq.getElement().element("query").elementIterator(); i.hasNext(); ) {
                    Element current = i.next();
                    if (current.getName().equalsIgnoreCase("fcm"))
                    {
                        user.getProperties().put("fcmToken",current.getText());
                        Log.trace("new fcmToken : "+current.getText());
                    }
                    if (current.getName().equalsIgnoreCase("apn"))
                    {
                        user.getProperties().put("apnToken",current.getText());
                        Log.trace("new apnToken : "+current.getText());
                    }
                }

                IQ result = IQ.createResultIQ(iq);
                result.getElement().addElement("fcmToken").addText(user.getProperties().getOrDefault("fcmToken",""));
                result.getElement().addElement("apnToken").addText(user.getProperties().getOrDefault("apnToken",""));

                return result;
            }
        } catch (UserNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public IQHandlerInfo getInfo() {
        return new IQHandlerInfo("query","unofficialpushnotifications:prop:token");
    }
}
