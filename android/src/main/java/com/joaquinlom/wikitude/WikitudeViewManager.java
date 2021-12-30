package com.joaquinlom.wikitude;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Lifecycle;

import android.util.Log;
import android.util.Base64;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.wikitude.architect.ArchitectJavaScriptInterfaceListener;
import com.wikitude.architect.ArchitectView;


/**
 * This class is what handles creating new WikitudeView instances on the React-Native side.
 * It extends SimpleViewManager and implements Wikitude's Architect classes.
 *
 * See more at: https://reactnative.dev/docs/native-components-android#1-create-the-viewmanager-subclass
 *         And: https://reactnative.dev/docs/native-components-android#3-create-the-viewmanager-subclass
 */
public class WikitudeViewManager
        extends SimpleViewManager<WikitudeView>
        implements ArchitectJavaScriptInterfaceListener, ArchitectView.ArchitectWorldLoadedListener, ArchitectView.CaptureScreenCallback {

    //Commands
    public static final String COMMAND_SET_URL = "setUrl";
    public static final String COMMAND_CALL_JAVASCRIPT = "callJS";
    public static final String COMMAND_INJECT_LOCATION = "injectLocation";
    public static final String COMMAND_PAUSE_AR = "stopAR";
    public static final String COMMAND_RESUME_AR = "resumeAR";
    public static final String COMMAND_CAPTURE_SCREEN = "captureScreen";
    //public static final int COMMAND_GET_ANNOTATIONS = 4;

    WikitudeView wikitude;
    ArchitectView architectView;
    ReactContext ctx;
    String licenseKey = "";
    String url = "";
    Activity activity;
    ConstraintLayout container;
    Boolean firstTime = true;
    Boolean hasCameraPermission = false;

    // Log tag
    private final String TAG ="WikitudeViewManager";

    // Value used to access this class in React
    public static final String REACT_CLASS = "RNWikitude";

    // Constructor
    public WikitudeViewManager(ReactApplicationContext context){
        super();
        this.ctx = context;
        Log.d(TAG, "WikitudeViewManager instantiated.");
    }

    /**
     * Required by React Native; this is how to knows what Class to call.
     * @return String
     */
    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    /**
     * This function is called when a new WikitudeView is created in React.
     * It creates the view in a default statue, it does not pass any props yet.
     * Similar to componentDidMount; this executes directly before.
     * @param context ThemedReactContext
     * @return WikitudeView A new Wikitude View instance.
     */
    @NonNull
    @Override
    public WikitudeView createViewInstance(ThemedReactContext context) {
        this.ctx = context;
        this.activity  = context.getCurrentActivity();

        // Views should be created in a default state, and later updated by a followup call to updateView
        this.wikitude = new WikitudeView(this.activity, this.ctx, this.licenseKey,this);
        // add JS listener so we can send the view JS code if needed
        this.wikitude.addArchitectJavaScriptInterfaceListener(this);
        // add lifecycle listener so that we can invoke lifecycle methods
        this.ctx.addLifecycleEventListener(this.mLifeEventListener);

        return this.wikitude;
    }

    /**
     * Called when view stops being rendered in React.
     * Similar to componentWillUnmount; this executes directly after.
     * @param view The view that stopped being rendered.
     */
    @Override
    public void onDropViewInstance(@NonNull WikitudeView view) {
        super.onDropViewInstance(view);
        Log.d(TAG,"View was removed from screen.");
        Lifecycle.State state = view.getLifecycle().getCurrentState();
        if(state == Lifecycle.State.RESUMED){
            view.onPause();
        }
        view.onDestroy();
    }

    /**
     * Below are the props for React-Native access. The `name` property is
     * the name of the accessor on the JS side. All setter methods should be
     * public, return void, include a view as its first argument, and the set
     * parameter as its second argument.
     *
     * See more at: https://reactnative.dev/docs/native-components-android#3-expose-view-property-setters-using-reactprop-or-reactpropgroup-annotation
     */

    @ReactProp(name = "isRunning")
    public void setIsRunning(WikitudeView view, boolean isRunning){
        Log.d(TAG,"set Is running to " + isRunning);
    }
    @ReactProp(name = "feature")
    public void setFeature(WikitudeView view, int feature){
        // TODO
    }
    @ReactProp(name = "url")
    public void setUrl(WikitudeView view, String url) {
        Log.d(TAG,"Setting url: "+url);
        view.setUrl(url);
    }
    @ReactProp(name = "licenseKey")
    public void setLicenseKey(WikitudeView view, String licenseKey) {
        Log.d(TAG,"Setting License"+licenseKey);
        view.setLicenseKey(licenseKey);
    }

    @ReactMethod
    public void setNewUrl(String url){
        if(this.wikitude != null){
            if(this.activity != null ){
                this.wikitude.setUrl(url);
                Handler mainHandler = new Handler(this.activity.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"Changed url to " + url);
                        wikitude.loadWorld();
                    }
                };
                mainHandler.post(myRunnable);
            }
        }
    }

    // TODO: Either implement or remove this
    final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            Log.d(TAG, "Activity event " + activity);
            if(requestCode == 13){
            }
        }
    };

    // Event listener for react LifeCycle methods
    final LifecycleEventListener mLifeEventListener = new LifecycleEventListener() {
        @Override
        public void onHostResume() {
            if(wikitude != null){
                Log.d(TAG,"onResume Wikitude");
            }
        }
        @Override
        public void onHostPause() {
            if(wikitude != null) {
                Log.d(TAG,"onPause Wikitude");
            }
        }
        @Override
        public void onHostDestroy() {
            if(wikitude != null){
                Log.d(TAG,"onDestroy Wikitude");
            }
        }
    };

    /**
     * Handles all commands sent from React using the UIManager.
     * @param view The Wikitude React view that called the command.
     * @param commandId ID string of the command called. Ex. 'setURL'
     * @param args Args passed with the command.
     */
    @Override
    public void receiveCommand(@NonNull WikitudeView view, String commandId, @Nullable ReadableArray args) {
        Log.d(TAG, "Received command " + commandId + " from React.");
        switch (commandId){
            case COMMAND_SET_URL:
                assert args != null;
                view.setUrl(args.getString(0));
                break;
            case COMMAND_CALL_JAVASCRIPT:
                assert args != null;
                view.callJavascript(args.getString(0));
                break;
            case COMMAND_INJECT_LOCATION:
                assert args != null;
                view.setLocation(args.getDouble(0),args.getDouble(1),100f);
                break;
            case COMMAND_RESUME_AR:
                view.onResume();
                view.loadWorld();
                break;
            case COMMAND_PAUSE_AR:
                view.onPause();
                break;
            case COMMAND_CAPTURE_SCREEN:
                assert args != null;
                view.captureScreen(args.getBoolean(0));
                break;
            default:
                Log.e(TAG, "Invalid command " + commandId + " received from React.");
        }
    }

    /**
     * This creates events for reactContext.getJSModule.receiveEvent() calls, and maps them to
     * callback functions in React.
     * For example:
     *      ```
     *      MapBuilder.builder()
     *          .put("onReceived",
     *              MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onJsonReceived")))
     *      ```
     * Will map the onReceived native event to the onJsonReceived callback prop in React.
     *
     * See more at: https://reactnative.dev/docs/native-components-android#events
     *
     * @return MapBuilder.Builder
     */
    @Override
    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder()
                .put("onJsonReceived",
                        MapBuilder.of("phasedRegistrationNames",   MapBuilder.of("bubbled", "onJsonReceived")))
                .put("onFinishLoading",
                        MapBuilder.of("phasedRegistrationNames",   MapBuilder.of("bubbled", "onFinishLoading"))).
                        put("onFailLoading",
                                MapBuilder.of( "phasedRegistrationNames",  MapBuilder.of("bubbled", "onFailLoading")))
                .put("onScreenCaptured",
                        MapBuilder.of("phasedRegistrationNames",   MapBuilder.of("bubbled", "onScreenCaptured"))
                ).build();
    }

    public void setLocation(Double lat,Double lng){
        if(this.wikitude != null){
            if(this.activity != null ){
                wikitude.setLat(lat);
                wikitude.setLng(lng);
                Handler mainHandler = new Handler(this.activity.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                            Log.d(TAG,"Change location");
                            wikitude.updateLocation();
                    }
                };
                mainHandler.post(myRunnable);
            }
        }
    }

    public void callJavascript(String s){
        if(this.wikitude != null){
            if(this.activity != null ){
                wikitude.setJS(s);
                Handler mainHandler = new Handler(this.activity.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                            Log.d(TAG,"Call JS");
                            wikitude.callJS();
                    }
                };
                mainHandler.post(myRunnable);
            }
        }
    }

    public void resumeAR(){
        if(this.activity != null ){
            Handler mainHandler = new Handler(this.activity.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    wikitude.clearCache();
                    wikitude.loadWorld();
                    Log.d(TAG,"On resume en handler");
                    wikitude.onResume();
                    wikitude.loadWorld();
                }
            };
            mainHandler.post(myRunnable);
        }
    }
    public Thread getThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) return t;
        }
        return null;
    }
    public void stopAR(){
        //wikitude.onPause();
        if(this.wikitude != null){
            if(this.activity != null ){
                Handler mainHandler = new Handler(this.activity.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"OnPause en handler");
                        wikitude.clearCache();
                        wikitude.onPause();
                    }
                };
                mainHandler.post(myRunnable);
            }
        }
        
    }

    /**
     * Event handler triggered when the screenCapture function is called..
     * Is linked to `onJsonReceived` event in React.
     *
     * @param image A bitmap image.
     */
    @Override
    public void onScreenCaptured(Bitmap image){
        //Bitmap to byte[]
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  
        image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

        this.emitEvent("image","onScreenCaptured", encoded);
    }

    /**
     * Event handler for parsing JSON.
     * Is linked to `onJsonReceived` event in React.
     *
     * @param jsonObject JSON that was received.
     */
    @Override
    public void onJSONObjectReceived(JSONObject jsonObject) {
        Log.d("Wikitude onJsonReceived","jsonObject receive");
        try {
            WritableMap map = JsonConvert.jsonToReact(jsonObject);
            this.emitEvent("onJsonReceived", map.toString());
        }catch(org.json.JSONException ex){
            Log.d(TAG, "Exception while parsing received JSON: " + ex);
        }
    }

    /**
     * Event handler for when a world loaded successfully.
     * Is linked to `onFinishLoading` event in React.
     *
     * @param s Message to send with event.
     */
    @Override
    public void worldWasLoaded(String s) {
        Log.d(TAG,"World Loaded: " + s);
        this.emitEvent("onFinishLoading", s);
    }

    /**
     * Event handler when world loading fails.
     * Is linked to `onFailLoading` event in React.
     *
     * @param error_code Error code of fail event.
     * @param desc Description of what happened.
     * @param fail_url The url of the world that failed to load.
     */
    @Override
    public void worldLoadFailed(int error_code, String desc, String fail_url) {
        Log.e("Wikitude", "World loading failed for " + fail_url);

        String message = error_code + ": " + desc + " + " + fail_url;
        this.emitEvent("onFailLoading", message);
    }

    /**
     * Sends an event message to the React application.
     * @param eventName Name of event. Ex. 'onFailLoading' or 'onJsonReceived'
     * @param message Message to send.
     */
    public void emitEvent(String eventName, String message){
        this.emitEvent("message", eventName, message);
    }

    /**
     * Sends an event to the React application.
     *
     * @param type Type of message. Ex. 'message' or 'image'
     * @param eventName Name of event. Ex. 'onFailLoading' or 'onJsonReceived'
     * @param message Message to send.
     */
    public void emitEvent(String type, String eventName, String message){
        WritableMap event = Arguments.createMap();
        event.putString(type, message);

        Log.d("Wikitude", "Sending '" + eventName + ":"+ type +"' event '" + message + "' to React");

        ReactContext reactContext = this.ctx;
        reactContext
                .getJSModule(RCTEventEmitter.class)
                .receiveEvent(this.wikitude.getId(), eventName, event);
    }
}
