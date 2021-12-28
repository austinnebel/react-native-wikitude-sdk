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

class WikitudeView extends React.Component {
  constructor(props) {
    super(props);

    this.state = {hasCameraPermissions: false, isRunning: false};
    this.requestPermission = this.requestPermission.bind(this);

    this.iosCommands = UIManager.getViewManagerConfig('RNWikitude').Commands;
    this.androidCommands = UIManager.RNWikitude.Commands;
  }

  async componentDidMount() {
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

  requestPermission = function () {
    if (Platform.OS === 'android') {
      try {
        PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.CAMERA, {
          title: 'Wikitude Needs the Camera',
          message: 'Wikitude needs the camera to use AR',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        }).then(granted => {
          if (granted === PermissionsAndroid.RESULTS.GRANTED) {
            this.setState({hasCameraPermissions: true});
          } else {
            this.setState({hasCameraPermissions: false});
          }
        });
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
        findNodeHandle(this.refs.wikitudeView),
      );
    }
  };

  callAndroid = (command, params) => {
    if (this.state.hasCameraPermissions) {
      UIManager.dispatchViewManagerCommand(
        findNodeHandle(this.refs.wikitudeView),
        command,
        params,
      );
    }
  };
  callIOS = (command, params) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this.refs.wikitudeRef),
      command,
      params,
    );
  };

  setWorldUrl = function (newUrl) {
    console.log('Set WORLD component');
    //this.stopRendering();
    if (Platform.OS === 'android') {
      this.callAndroid(this.androidCommands.setUrlMode, [newUrl]);
    } else if (Platform.OS === 'ios') {
      this.callIOS(this.iosCommands.setUrl, [newUrl]);
    }
  };

  callJavascript = function (js) {
    if (Platform.OS === 'android') {
      this.callAndroid(this.androidCommands.callJSMode, [js]);
    } else if (Platform.OS === 'ios') {
      this.callIOS(this.iosCommands.callJavascript, [js]);
    }
  };

  injectLocation = function (lat, lng) {
    if (Platform.OS === 'android') {
      this.callAndroid(this.androidCommands.injectLocationMode, [lat, lng]);
    } else {
      this.callIOS(this.iosCommands.injectLocation, [lat, lng]);
    }
  };

  stopRendering = function () {
    if (Platform.OS === 'android') {
      this.callAndroid(this.androidCommands.stopARMode, []);
    } else {
      this.callIOS(this.iosCommands.stopAR, []);
    }
  };

  resumeRendering = function () {
    console.log('RN-SDK: Calling resumeRendering');

    if (Platform.OS === 'android') {
      this.callAndroid(this.androidCommands.resumeARMode, []);
    } else {
      this.callIOS(this.iosCommands.resumeAR, []);
    }
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
    if (Platform.OS === 'android') {
      this.callAndroid(this.androidCommands.captureScreen, [mode]);
    } else if (Platform.OS === 'ios') {
      this.callIOS(this.iosCommands.captureScreen, [mode]);
    }
  };

  render() {
    const hasPermission = this.state.hasCameraPermissions;

    if (Platform.OS === 'android') {
      if (hasPermission) {
        return (
          <WKTView
            ref="wikitudeView"
            {...this.props}
            onJsonReceived={this.onJsonReceived}
            onFailLoading={this.onFailLoading}
            onFinishLoading={this.onFinishLoading}
            onScreenCaptured={this.onScreenCaptured}
          />
        );
      } else {
        return (
          <Button title="Request Permission" onPress={this.requestPermission} />
        );
      }
    } else {
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
