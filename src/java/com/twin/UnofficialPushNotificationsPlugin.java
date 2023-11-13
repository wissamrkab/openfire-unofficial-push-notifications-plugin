package com.twin;


import com.twin.handlers.IQTokenHandler;
import org.jivesoftware.openfire.OfflineMessageStrategy;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class UnofficialPushNotificationsPlugin implements Plugin {

    private static final Logger Log = LoggerFactory.getLogger(UnofficialPushNotificationsPlugin.class);
    OfflineMessageStoreListener listener;

    public void initializePlugin(PluginManager pManager, File pluginDirectory) {
        Log.trace("initialize UnofficialPushNotificationsPlugin plugin. Start.");

        listener = new OfflineMessageStoreListener();
        OfflineMessageStrategy.addListener(listener);

        XMPPServer.getInstance().getIQRouter().addHandler(new IQTokenHandler());

        Log.trace("initialize UnofficialPushNotificationsPlugin plugin. Finish.");
    }

    public void destroyPlugin() {
        // unregister with interceptor manager
        OfflineMessageStrategy.removeListener(listener);
        Log.trace("destroy CallbackOnOffline plugin.");
    }
}
