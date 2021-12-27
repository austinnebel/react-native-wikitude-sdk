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
        Log.d(TAG,"Setting url:"+url);
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

    public WikitudeView getWikitudeView(){
        return this.wikitude;
    }

    public void setActivity(Activity activity){
        this.activity = activity;
    }

    final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
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
     * Maps command constants to function names.
     *
     * Example: MapBuilder.of("create", COMMAND_CREATE);
     *      maps the `create` function to an integer `COMMAND_CREATE`.
     *
     * @return java.util.Map<K, V>
     */
    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "setUrlMode",
                COMMAND_SET_URL,
                "callJSMode",
                COMMAND_CALL_JAVASCRIPT,
                "injectLocationMode",
                COMMAND_INJECT_LOCATION,
                "stopARMode",
                COMMAND_STOP_AR,
                "resumeARMode",
                COMMAND_RESUME_AR,
                "captureScreen",
                COMMAND_CAPTURE_SCREEN);
    }

    /**
     * Handles all commands defined/mapped in `getCommandsMap`.
     * These are originally called in JS and are automatically sent here.
     *
     * @param root The WikitudeView that called the command.
     * @param commandId ID of the command called.
     * @param args Args passed with the command.
     */
    @Override
    public void receiveCommand(@NonNull WikitudeView root, int commandId, @javax.annotation.Nullable ReadableArray args) {
        switch (commandId){
            case COMMAND_SET_URL:
                assert args != null;
                root.setUrl(args.getString(0));
                break;
            case COMMAND_CALL_JAVASCRIPT:
                assert args != null;
                root.callJavascript(args.getString(0));
                break;
            case COMMAND_INJECT_LOCATION:
                assert args != null;
                root.setLocation(args.getDouble(0),args.getDouble(1),100f);
                break;
            case COMMAND_RESUME_AR:
                //without thread handling
                root.onResume();
                root.loadWorld();
                break;
            case COMMAND_STOP_AR:
                root.onPause();
                break;
            case COMMAND_CAPTURE_SCREEN:
                assert args != null;
                root.captureScreen(args.getBoolean(0));
                break;
        }
    }

    @ReactMethod
    public void setNewUrl(String url){
        if(this.wikitude != null){
            if(this.activity != null ){
                wikitude.setUrl(url);
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
            view.onDestroy();
        }catch(Exception e){
            Log.d(TAG,"Error");
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
        if(wikitude != null){
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
        WritableMap event = Arguments.createMap();

        //Bitmap to byte[]
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  
        image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        
        event.putString("image",encoded);
        ReactContext reactContext = this.ctx;
        Log.d(TAG,"Screenshot capture");
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                this.wikitude.getId(),
                "onScreenCaptured",
                event);        
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

        WritableMap map;
        try {
            map = JsonConvert.jsonToReact(jsonObject);

            ReactContext reactContext = this.ctx;
            reactContext
                    .getJSModule(RCTEventEmitter.class)
                    .receiveEvent(this.wikitude.getId(), "onJsonReceived", map);
        }catch(org.json.JSONException ex){
            System.out.println("Exception while parsing received JSON: " + ex);
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
        WritableMap event = Arguments.createMap();
        event.putString("message",s);

        Log.d(TAG,"World Loaded");

        ReactContext reactContext = this.ctx;
        reactContext
                .getJSModule(RCTEventEmitter.class)
                .receiveEvent(this.wikitude.getId(), "onFinishLoading", event);
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
        WritableMap event = Arguments.createMap();
        String message = error_code + ": " + desc + " + " + fail_url;
        event.putString("message", message);

        Log.e("Wikitude",message);

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

    public void setUrl(String newUrl){
        if(isUrl(newUrl)){
            Log.d(TAG,"Received web URL");
            this.url = newUrl;
        }else{
            Log.d(TAG,"Received local URL");
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
            Log.i(TAG, "World URL not received yet.");
            return;
        }
        try{
            Log.d(TAG,this.url);
            this.load(this.url);
        }catch(IOException e){
            Log.e(TAG,e.getMessage());
        }
        Log.i(TAG, "Loaded world " + this.url);
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

