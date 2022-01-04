import {
  NativeModules,
  requireNativeComponent,
  findNodeHandle,
  UIManager,
  Platform,
} from 'react-native';
import React from 'react';
import PropTypes from 'prop-types';
import NativeFunctions from './res/functionMaps';

/**
 * Base class for using the wikitude SDK.
 *
 * To implement this you can create a component that
 * creates necessary configurations and returns something
 * like this:
 *      <WikitudeView
 *         ref={wikitudeView}
 *         licenseKey={config.licenseKey}
 *         url={config.url}
 *         style={styles.AR}
 *         onFailLoading={onFailLoading}
 *         onJsonReceived={onJsonReceived}
 *         onFinishLoading={onFinishLoading}
 *         onScreenCaptured={onScreenCaptured}
 *       />
 */
class WikitudeView extends React.Component {
  constructor(props) {
    super(props);

    this.state = {hasCameraPermissions: false, isRunning: false};
  }

  async componentDidMount() {
    console.log('didmount Wikitude SDK index.js');

    this.resumeRendering();
  }

  componentWillUnmount() {
    console.log('RN-SDK: componentWillUnmount');
  }

  componentDidUpdate() {
    console.log('RN-SDK: ComponentDidUpdate');
    console.log('RN-SDK: Value of URL: ', this.props.url);
  }

  // implemented in Java with @ReactMethod tag
  // TODO: Not implemented in java, needs package export
  isDeviceSupportingFeature = feature => {
    if (Platform.OS === 'android') {
    } else {
      return NativeModules.RNWikitude.isDeviceSupportingFeatures(
        feature,
        findNodeHandle(this.wikitudeRef),
      );
    }
  };

  /**
   * Calls a method on native Wikitude code.
   *
   * It determines the command to use based on the current platform OS.
   *
   * @param {NativeFunctions} command The native function to call.
   * @param {list} params A list of parameters to pass to the native function.
   * @returns
   */
  callNative = (command, params: list) => {
    let platformCommand = command[Platform.OS];

    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this.wikitudeRef),
      platformCommand,
      params,
    );
  };

  // Below are functions that make calls to native code

  setWorldUrl = newUrl => {
    console.log('RN-SDK: Calling setWorldUrl');
    this.callNative(NativeFunctions.setUrl, [newUrl]);
  };

  callJavascript = js => {
    console.log('RN-SDK: Calling callJavascript');
    this.callNative(NativeFunctions.callJS, [js]);
  };

  injectLocation = (lat, lng) => {
    console.log('RN-SDK: Calling injectLocation');
    this.callNative(NativeFunctions.injectLocation, [lat, lng]);
  };

  captureScreen = mode => {
    this.callNative(NativeFunctions.captureScreen, [mode]);
  };

  // called when component unmounts
  stopRendering = () => {
    console.log('RN-SDK: Calling stopRendering');
    this.callNative(NativeFunctions.stopAR, []);
  };
  // called when component mounts
  resumeRendering = () => {
    console.log('RN-SDK: Calling resumeRendering');
    this.callNative(NativeFunctions.resumeAR, []);
  };

  // Below are the event handlers triggered by the native WikitudeViewManager class

  onJsonReceived = event => {
    console.log('RN-SDK: Calling onJsonReceived');
    if (this.props.onJsonReceived) {
      this.props.onJsonReceived(event.nativeEvent);
    }
  };
  onFinishLoading = event => {
    if (this.props.onFinishLoading) {
      this.props.onFinishLoading(event.nativeEvent);
    }
  };
  onFailLoading = event => {
    if (this.props.onFailLoading) {
      this.props.onFailLoading(event.nativeEvent);
    }
  };
  onScreenCaptured = event => {
    if (!this.props.onScreenCaptured) {
      this.props.onScreenCaptured(event.nativeEvent);
    }
  };

  // returns native view

  render() {
    return (
      <WKTView
        ref={e => (this.wikitudeRef = e)}
        {...this.props}
        onJsonReceived={this.onJsonReceived}
        onFailLoading={this.onFailLoading}
        onFinishLoading={this.onFinishLoading}
        onScreenCaptured={this.onScreenCaptured}
      />
    );
  }
}

WikitudeView.propTypes = {
  /**
   * A Boolean value that determines whether the user may use pinch
   * gestures to zoom in and out of the map.
   */
  licenseKey: PropTypes.string,
  url: PropTypes.string,
  feature: PropTypes.number,
  onJsonReceived: PropTypes.func,
  onFinishLoading: PropTypes.func,
  onFailLoading: PropTypes.func,
  onScreenCaptured: PropTypes.func,
  isPOI: PropTypes.bool,
};

/**
 *
 */
var WKTView = requireNativeComponent('RNWikitude', WikitudeView);

module.exports = {
  WikitudeView,
};
