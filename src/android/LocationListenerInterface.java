package org.apache.cordova.geolocation;

import org.apache.cordova.CallbackContext;

/**
 * Created with IntelliJ IDEA.
 * User: mamonth
 * Date: 18.01.14
 * Time: 19:07
 * To change this template use File | Settings | File Templates.
 */
public interface LocationListenerInterface {

    public void clearWatch(String timerId);

    public void addCallback(CallbackContext callbackContext, int timeout);

    public void addWatch(String timerId, CallbackContext callbackContext);

}
