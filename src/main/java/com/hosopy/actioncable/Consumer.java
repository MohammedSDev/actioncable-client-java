package com.hosopy.actioncable;

import java.net.URI;

/**
 * The Consumer establishes the connection to a server-side Ruby Connection object.
 * Once established, the ConnectionMonitor will ensure that its properly maintained through heartbeats and checking for stale updates.
 * The Consumer instance is also the gateway to establishing subscriptions to desired channels.
 * <pre>{@code
 * // Default Subscription Interface
 * Channel appearanceChannel = new Channel("AppearanceChannel");
 * Subscription subscription = consumer.getSubscriptions().create(appearanceChannel);
 * // Custom Subscription Interface
 * ChatSubscription chatSubscription = consumer.getSubscriptions().create(appearanceChannel, ChatSubscription.class);
 * consumer.connect();
 * }</pre>
 *
 * @author hosopy
 */
public class Consumer {

    public static class Options extends Connection.Options {
    }

    private Connection connection;

    private ConnectionMonitor connectionMonitor;

    private Subscriptions subscriptions;

    private GeneralListener generalListener;

    /*package*/ Consumer(URI uri, Options options) {
        this.subscriptions = new Subscriptions(this);
        this.connection = new Connection(uri, options);
        this.connectionMonitor = new ConnectionMonitor(connection, options);
        this.connection.setListener(new Connection.Listener() {
            @Override
            public void onOpen() {
                connectionMonitor.recordConnect();
                subscriptions.reload();
                if (generalListener != null) generalListener.onOpen();
            }

            @Override
            public void onFailure(Exception e) {
                subscriptions.notifyFailed(new ActionCableException(e));
                if (generalListener != null) generalListener.onFailure(new ActionCableException(e));
            }

            @Override
            public void onMessage(String string) {
                final Message message = Message.fromJson(string);
                if (generalListener != null) generalListener.onMessage(string);
                if (message.isWelcome()) {
                    onOpen();
                } else if (message.isPing()) {
                    connectionMonitor.recordPing();
                } else if (message.isConfirmation()) {
                    subscriptions.notifyConnected(message.getIdentifier());
                } else if (message.isRejection()) {
                    subscriptions.reject(message.getIdentifier());
                } else {
                    subscriptions.notifyReceived(message.getIdentifier(), message.getMessage());
                }
            }

            @Override
            public void onClosing() {
                subscriptions.notifyDisconnected();
                connectionMonitor.recordDisconnect();
                if (generalListener != null) generalListener.onClosing();
            }

            @Override
            public void onClosed() {
                if (generalListener != null) generalListener.onClosed();
            }
        });
    }

    /*package*/ Consumer(URI uri) {
        this(uri, new Options());
    }

    /**
     * Get subscriptions container.
     *
     * @return {@link Subscriptions} instance
     */
    public Subscriptions getSubscriptions() {
        return subscriptions;
    }

    /**
     * Establish connection.
     */
    public void connect() {
        connection.open();
        connectionMonitor.start();
    }

    /**
     * Disconnect the underlying connection.
     */
    public void disconnect() {
        connection.close();
        connectionMonitor.stop();
    }

    public void unsubscribeAndDisconnect() {
        subscriptions.removeAll();
        connection.close();
        connectionMonitor.stop();
    }

    public void setGeneralListener(GeneralListener listener) {
        generalListener = listener;
        connection.setGeneralListener(listener);
    }

    public boolean send(Command command) {
        return connection.send(command.toJson());
    }

    public void setStaleThresholdInSecond(int staleThresholdInSecond){
        if(connectionMonitor !=null) connectionMonitor.setStaleThresholdInSecond(staleThresholdInSecond);
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isConnecting() {
        return this.connection != null && this.connection.isConnecting();
    }

    public boolean isConnected() {
        return this.connection != null && this.connection.isOpen();
    }

    public boolean isClosing() {
        return this.connection != null && this.connection.isClosing();
    }

    public boolean isClosed() {
        return this.connection == null || this.connection.isClosed();
    }
}
