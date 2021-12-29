package com.joaquinlom.wikitude;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WikitudePackage implements ReactPackage {

  private WikitudeViewManager wikitudeViewManager;

  //@Override
  public List<Class<? extends JavaScriptModule>> createJSModules() {
    return Collections.emptyList();
  }

  /**
   * This is used by React to import native, non-view based classes into the React application.
   * @param reactContext React Application Context.
   * @return Collection of importable modules.
   */
  @NonNull
  @Override
  public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
    return Collections.emptyList();
  }

  /**
   * Registers the WikitudeViewManager in this package with React.
   * This works similarly to createNativeModules.
   * @param reactContext React Application Context.
   * @return Array They array of ViewManager objects.
   */
  @NonNull
  @Override
  public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
    return Arrays.asList(singleViewManager(reactContext));
  }

  /**
   * Returns a new Wikitude View manager as long as it doesn't already exist.
   * @param context Application context.
   * @return WikitudeViewManager A new view manager.
   */
  public WikitudeViewManager singleViewManager(ReactApplicationContext context) {
    Log.d("WikitudePackage","Requested creation of new WikitudeViewManager.");

    if(this.wikitudeViewManager == null){
      Log.d("WikitudePackage","WikitudeViewManager is null, creating new instance.");
      this.wikitudeViewManager = new WikitudeViewManager(context);
    }else{
      Log.d("WikitudePackage","Returning the same WikitudeViewManager.");
    }
    return this.wikitudeViewManager;
  }
}
