package com.hosopy.actioncable;


/**
 * GeneralListener provides a number of callbacks  for calling remote procedure calls
 * on the corresponding Channel instance on the server side.
 *
 */
public interface GeneralListener {
    public void onMessage(String string);
    /**
     * onOpen can be called multi-times
     * e.g.[on connection establish, on welcome message received]
     *
     */
    public void onOpen();
    public void onSend(String data);
    public void onClosing();
    public void onClosed();
    public void onFailure(Exception e);
}
