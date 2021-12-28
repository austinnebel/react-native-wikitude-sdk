import {
  NativeModules,
  requireNativeComponent,
  findNodeHandle,
  UIManager,
  PermissionsAndroid,
  Button,
  Platform,
} from 'react-native';
import React from 'react';
import PropTypes from 'prop-types';
import NativeFunctions from './res/functionMaps';

class WikitudeView extends React.Component {
  constructor(props) {
    super(props);

    this.state = {hasCameraPermissions: false, isRunning: false};
    this.requestPermission = this.requestPermission.bind(this);
  }

  async componentDidMount() {
    await this.requestPermission();
    console.log('didmount Wikitude SDK index.js');

    //Sometimes the resume is not calling because the references is wrong
    this.resumeRendering();
  }

  componentWillUnmount() {
    console.log('RN-SDK: componentWillUnmount');
    this.stopRendering();
  }

  componentDidUpdate() {
    console.log('RN-SDK: ComponentDidUpdate');
    console.log('RN-SDK: Value of URL: ', this.props.url);
    //this.resumeRendering();
  }

  requestPermission = async () => {
    if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.CAMERA,
          {
            title: 'Wikitude Needs the Camera',
            message: 'Wikitude needs the camaera to show cool AR',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK',
          },
        );
        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          this.setState({hasCameraPermissions: true});
        } else {
          this.setState({hasCameraPermissions: false});
        }
      } catch (err) {
        console.warn(err);
      }
    }
  };

  // returns if this device supports the specified feature
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
   * First, this method determines whether the user has permission to
   * use the Wikitude instance, then it determines the command to use based
   * on the current platform OS.
   *
   * @param {NativeFunctions} command The native function to call.
   * @param {list} params A list of parameters to pass to the native function.
   * @returns
   */
  callNative = (command, params: list) => {
    if (this.hasPermission()) {
      return;
    }
    let platformCommand = command[Platform.OS];

    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this.wikitudeRef),
      platformCommand,
      params,
    );
  };

  setWorldUrl = function (newUrl) {
    console.log('Set WORLD component');
    this.callNative(NativeFunctions.setUrl, [newUrl]);
  };

  callJavascript = function (js) {
    this.callNative(NativeFunctions.callJS, [js]);
  };

  injectLocation = function (lat, lng) {
    this.callNative(NativeFunctions.injectLocation, [lat, lng]);
  };

  stopRendering = function () {
    this.callNative(NativeFunctions.stopAR, []);
  };

  resumeRendering = function () {
    console.log('RN-SDK: Calling resumeRendering');
    this.callNative(NativeFunctions.resumeAR, []);
  };

  onJsonReceived = event => {
    if (!this.props.onJsonReceived) {
      return;
    }
    // process native event
    this.props.onJsonReceived(event.nativeEvent);
  };

  onFinishLoading = event => {
    if (!this.props.onFinishLoading) {
      return;
    }
    this.props.onFinishLoading(event.nativeEvent);

    if (Platform.OS === 'android') {
      //this.resumeRendering();
    } else {
      this.resumeRendering();
    }
  };

  onFailLoading = event => {
    if (!this.props.onFailLoading) {
      return;
    }
    this.props.onFailLoading(event.nativeEvent);
  };

  onScreenCaptured = event => {
    if (!this.props.onScreenCaptured) {
      return;
    }
    this.props.onScreenCaptured(event.nativeEvent);
  };

  captureScreen = mode => {
    this.callNative(NativeFunctions.captureScreen, [mode]);
  };

  hasPermission = () => {
    if (Platform.OS === 'android') {
      return this.state.hasCameraPermission;
    }
    return true;
  };
  render() {
    const hasPermission = this.state.hasCameraPermissions;

    // only android needs button to request permission
    if (Platform.OS === 'android') {
      if (!hasPermission) {
        return (
          <Button title="Request Permission" onPress={this.requestPermission} />
        );
      }
    }

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

var WKTView = requireNativeComponent('RNWikitude', WikitudeView);

module.exports = {
  WikitudeView,
};
