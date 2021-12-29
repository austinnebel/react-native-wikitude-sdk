package com.joaquinlom.wikitude;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.content.res.AssetManager;
import java.net.URL;
import java.io.ByteArrayOutputStream;
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
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.joaquinlom.wikitude.JsonConvert;
import com.facebook.react.views.image.ReactImageView;
import com.wikitude.architect.ArchitectJavaScriptInterfaceListener;
import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

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
    public static final int COMMAND_SET_URL = 1;
    public static final int COMMAND_CALL_JAVASCRIPT = 2;
    public static final int COMMAND_INJECT_LOCATION = 3;
    public static final int COMMAND_STOP_AR = 4;
    public static final int COMMAND_RESUME_AR = 5;
    public static final int COMMAND_CAPTURE_SCREEN = 6;
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
    }

    /**
     * This function is called when a new WikitudeView needs to be created by this manager.
     * @param context ThemedReactContext
     * @return WikitudeView A new Wikitude View instance.
     */
    @NonNull
    @Override
    public WikitudeView createViewInstance(ThemedReactContext context) {

        this.activity  = context.getCurrentActivity();

        // Views should be created in a default state, and later updated by a followup call to updateView
        wikitude = new WikitudeView(activity, context, this.licenseKey,this);

        this.ctx = context;
        wikitude.addArchitectJavaScriptInterfaceListener(this);

        context.addLifecycleEventListener(mLifeEventListener);

        // return this.container;
        return wikitude;
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
    public void setLicenseKey(WikitudeView view,String licenseKey) {
        Log.d(TAG,"Setting License"+licenseKey);
        view.setLicenseKey(licenseKey);
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

    public WikitudeView getView(){
        return this.wikitude;
    }

    public void setActivity(Activity activity){
        this.activity = activity;
    }

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
     * Maps command constants to React function names.
     *
     * Example: MapBuilder.of("create", COMMAND_CREATE);
     *      maps the `create` function to an integer `COMMAND_CREATE`.
     *
     *      To access this in react, you would pass UIManager.RNWikitude.Commands.create
     *      to the UIManager.
     *
     * @return java.util.Map<K, V>
     */
    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "setUrlMode",           COMMAND_SET_URL,
                "callJSMode",           COMMAND_CALL_JAVASCRIPT,
                "injectLocationMode",   COMMAND_INJECT_LOCATION,
                "stopARMode",           COMMAND_STOP_AR,
                "resumeARMode",         COMMAND_RESUME_AR,
                "captureScreen",        COMMAND_CAPTURE_SCREEN
        );
    }

    /**
     * Handles all commands defined/mapped in `getCommandsMap`.
     * These are originally called in JS and are automatically sent here.
     *
     * @param view The Wikitude React view that called the command.
     * @param commandId ID of the command called.
     * @param args Args passed with the command.
     */
    @Override
    public void receiveCommand(@NonNull WikitudeView view, int commandId, @javax.annotation.Nullable ReadableArray args) {
        Log.d(TAG, "Received command " + commandId);
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
                //without thread handling
                view.onResume();
                Log.d(TAG, "ON RESUME CALLED");
                view.loadWorld();
                break;
            case COMMAND_STOP_AR:
                view.onPause();
                break;
            case COMMAND_CAPTURE_SCREEN:
                assert args != null;
                view.captureScreen(args.getBoolean(0));
                break;
        }
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


    @Override
    public void onDropViewInstance(@NonNull WikitudeView view) {
        super.onDropViewInstance(view);
        Log.d(TAG,"Dropping View");
        try{
            view.onPause();
            view.clearCache();
        }catch(Exception e){
            Log.e(TAG,"Error pausing view: " + e);
        }
        try{
            view.onDestroy();
        }catch(Exception e){
            Log.e(TAG,"Error destroying view: " + e);
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
     * This maps native events called inside receiveEvent() to React callback functions.
     *
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

    @Override
    public void onScreenCaptured(Bitmap image){
        //Bitmap to byte[]
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  
        image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

        this.emitEvent("image", encoded);
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
            this.emitEvent(map.toString());
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
        this.emitEvent(s);
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
        this.emitEvent(message);
    }

    /**
     * Sends an event to the React application.
     *
     * @param message Message to send.
     */
    public void emitEvent(String message){
        WritableMap event = Arguments.createMap();
        event.putString("message", message);

        Log.d("Wikitude", "Sending event message '" + message + "' to React");

        ReactContext reactContext = this.ctx;
        reactContext
                .getJSModule(RCTEventEmitter.class)
                .receiveEvent(this.wikitude.getId(), "onFailLoading", event);
    }

    /**
     * Sends an event to the React application.
     *
     * @param type Type of message. Ex. 'message' or 'image'
     * @param message Message to send.
     */
    public void emitEvent(String type, String message){
        WritableMap event = Arguments.createMap();
        event.putString(type, message);

        Log.d("Wikitude", "Sending event message '" + message + "' to React");

        ReactContext reactContext = this.ctx;
        reactContext
                .getJSModule(RCTEventEmitter.class)
                .receiveEvent(this.wikitude.getId(), "onFailLoading", event);
    }
}

class WikitudeView extends ArchitectView{

    Context ctx;
    Activity activity;
    String licenseKey = "";
    String url = "";
    String javascript = "";
    Double lat = 0.0;
    Double lng = 0.0;
    String TAG = "WikitudeView";
    WikitudeViewManager ctxManager;

    private ArchitectStartupConfiguration startUpConfig = new ArchitectStartupConfiguration();

    public WikitudeView(Activity activity){
        super(activity);
    }
    public WikitudeView(Activity activity, Context ctx, String licenseKey, WikitudeViewManager manager){
        super(activity);
        this.activity = activity;
        this.licenseKey = licenseKey;
        this.ctxManager = manager;
    }

    /**
     * Method to call in the corresponding life-cycle method of the containing activity.
     * Configuration file may have more optional information besides the license key
     *
     * @param config advanced configuration file, in case you want to pass more than only the license key.
     * @throws ArchitectView.CamNotAccessibleException - when no camera could be found or accessed.
     * @throws ArchitectView.MissingFeatureException - when the features set in config are not supported by the device.
     */
    @Override
    public void onCreate(ArchitectStartupConfiguration config){
        super.onCreate(config);
    }

    /**
     * Life-cycle method to called in the corresponding method of the containing activity.
     * @throws ArchitectView.CamNotAccessibleException - when camera permissions are not granted or no camera could be found or accessed.
     */
    @Override
    public void onResume(){
        super.onResume();
    }

    /**
     * Life-cycle method that should be called in the corresponding method of the activity.
     * @throws IllegalStateException If method is called without preceding life-cycle-method calls
     */
    @Override
    public void onPause(){
        super.onPause();
    }

    /**
     * onDestroy life-cycle method that should be called in the corresponding method of the activity.
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    /**
     * Deletes all cached files of this instance of the ArchitectView.
     * This guarantees that internal storage is cleaned and app-memory does not grow each session.
     */
    @Override
    public void clearCache(){
        super.clearCache();
    }

    public void setUrl(String newUrl){
        if(isUrl(newUrl)){
            Log.d(TAG,"setURL: Received web URL");
            this.url = newUrl;
        }else{
            Log.d(TAG,"setURL: Received local URL");
            this.url = newUrl+".html";
        }
        this.loadWorld();
    }
    public void setLicenseKey(String license){
        startUpConfig.setLicenseKey( license );
        createWikitude();
    }
    public void setLat(Double lat){
        this.lat = lat;
    }
    public void setLng(Double lng){
        this.lng = lng;
    }
    public void updateLocation(){
        this.setLocation(this.lat,this.lng,100f);
    }
    public void setJS(String s){
        this.javascript = s;
    }
    public void callJS(){
        this.callJavascript(this.javascript);
    }

    /**
     * Loads the "world" i.e. the index JS file containing the view.
     */
    public void loadWorld(){
        if(this.url.equals("")){
            Log.d(TAG, "World URL not received yet.");
            return;
        }
        try{
            Log.d(TAG,this.url);
            this.load(this.url);
        }catch(IOException e){
            Log.e(TAG,e.getMessage());
        }
        Log.d(TAG, "Loaded world " + this.url);
    }

    /**
     * Performs a screen capture. Calls ArchitectView screencapture after determining mode.
     * @param mode Capture mode to use.
     */
    public void captureScreen(Boolean mode){
        int insideMode;
        if(mode){
            insideMode = ArchitectView.CaptureScreenCallback.CAPTURE_MODE_CAM_AND_WEBVIEW;
        }else{
            insideMode = ArchitectView.CaptureScreenCallback.CAPTURE_MODE_CAM;
        }
        Log.d(TAG,"CaptureScreen called, MODE: "+ String.valueOf(insideMode));

        // calls ArchitectView captureScreen
        this.captureScreen(insideMode, ctxManager);
    }

    public void createWikitude(){
        Log.d(TAG,"Creating Wikitude view");
        this.onCreate(startUpConfig);
        this.onPostCreate();
        this.registerWorldLoadedListener(ctxManager);
        this.loadWorld();
    }

    public boolean isUrl(String url){
        try{
            new URL(url).toURI();
            return true;
        }catch(Exception e){
            return false;
        }
    }
}

