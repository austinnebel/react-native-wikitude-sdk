package com.joaquinlom.wikitude;
import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WikitudePackage implements ReactPackage {

  private Activity mActivity = null;
  private WikitudeViewManager wikManager;

  //@Override
  public List<Class<? extends JavaScriptModule>> createJSModules() {
    return Collections.emptyList();
  }

  /**
   * This is used by React to import native modules into the React application.
   * @param reactContext React Application Context.
   * @return Collection of importable modules.
   */
  @NonNull
  @Override
  public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
    List<NativeModule> modules = new ArrayList<>();
    //modules.add(new WikitudeModule(reactContext,singleViewManager(reactContext)));
    //return modules;
    return Collections.emptyList();
  }

  /**
   * Registers the WikitudeViewManager in this package.
   * This works similarly to createNativeModules.
   *
   * @param reactContext React Application Context.
   * @return Array They array of ViewManager objects.
   */
  @NonNull
  @Override
  public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
    return Arrays.asList(
            singleViewManager(reactContext)
    );
  }

  /**
   * Returns a new Wikitude View manager as long as it doesn't already exist.
   * @param context Application context.
   * @return WikitudeViewManager A new view manager.
   */
  public WikitudeViewManager singleViewManager(ReactApplicationContext context) {
    if(wikManager == null){
      Log.d("WikitudePackage","WikiManager is null, creating new instance.");
      wikManager = new WikitudeViewManager(context);
    }else{
      Log.d("WikitudePackage","Returning the same WikiManager.");
    }
    return wikManager;
  }
}
