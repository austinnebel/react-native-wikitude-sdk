package com.joaquinlom.wikitude;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;

import java.io.IOException;
import java.net.URL;

/**
 * Wikitude view that is shown in React.
 * Implements lifecycle methods.
 * See more at: https://developer.android.com/topic/libraries/architecture/lifecycle#lc
 */
class WikitudeView  extends ArchitectView implements LifecycleOwner {

    Context ctx;
    Activity activity;
    String licenseKey = "";
    String url = "";
    String javascript = "";
    Double lat = 0.0;
    Double lng = 0.0;
    String TAG = "WikitudeView";
    WikitudeViewManager viewManager;

    private LifecycleRegistry lifecycleRegistry;
    private ArchitectStartupConfiguration startUpConfig = new ArchitectStartupConfiguration();

    public WikitudeView(Activity activity){
        super(activity);
    }
    public WikitudeView(Activity activity, Context ctx, String licenseKey, WikitudeViewManager manager){
        super(activity);
        this.activity = activity;
        this.licenseKey = licenseKey;
        this.viewManager = manager;
    }

    @NonNull
    public LifecycleRegistry getLifecycle(){
        return this.lifecycleRegistry;
    }

    /**
     * Method to call in the corresponding life-cycle method of the containing activity.
     * Configuration file may have more optional information besides the license key.
     * Sets this views state to CREATED.
     * @param config advanced configuration file, in case you want to pass more than only the license key.
     * @throws ArchitectView.CamNotAccessibleException - when no camera could be found or accessed.
     * @throws ArchitectView.MissingFeatureException - when the features set in config are not supported by the device.
     */
    @Override
    public void onCreate(ArchitectStartupConfiguration config){
        super.onCreate(config);
        this.lifecycleRegistry = new LifecycleRegistry(this);
        this.lifecycleRegistry.markState(Lifecycle.State.CREATED);
    }

    /**
     * Life-cycle method to called in the corresponding method of the containing activity.
     * Sets this views state to RESUMED.
     * @throws ArchitectView.CamNotAccessibleException - when camera permissions are not granted or no camera could be found or accessed.
     */
    @Override
    public void onResume(){
        super.onResume();
        this.lifecycleRegistry.markState(Lifecycle.State.RESUMED);

    }

    /**
     * Life-cycle method that should be called in the corresponding method of the activity.
     * Sets this views state to STARTED.
     * @throws IllegalStateException If method is called without preceding life-cycle-method calls
     */
    @Override
    public void onPause(){
        super.onPause();
        this.lifecycleRegistry.markState(Lifecycle.State.STARTED);
    }

    /**
     * onDestroy life-cycle method that should be called in the corresponding method of the activity.
     * Sets this views state to DESTROYED.
     */
    @Override
    public void onDestroy(){
        //clear cache before destroying
        try{
            this.clearCache();
        }catch (java.lang.Exception e){
            Log.e(TAG, "Error clearing cache on destroy: " + e);
        }
        super.onDestroy();
        this.lifecycleRegistry.markState(Lifecycle.State.DESTROYED);
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
        this.startUpConfig.setLicenseKey( license );
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
        this.captureScreen(insideMode, this.viewManager);
    }

    public void createWikitude(){
        Log.d(TAG,"Creating Wikitude view");
        this.onCreate(startUpConfig);
        this.onPostCreate();
        this.registerWorldLoadedListener(this.viewManager);
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

