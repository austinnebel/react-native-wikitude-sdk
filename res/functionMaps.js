import {UIManager} from 'react-native';

const iosCommands = UIManager.getViewManagerConfig('RNWikitude').Commands;
const androidCommands = UIManager.RNWikitude.Commands;

const nativeFuncs = {
  setUrl: {
    android: androidCommands.setUrlMode,
    ios: iosCommands.setUrl,
  },
  callJS: {
    android: androidCommands.callJSMode,
    ios: iosCommands.callJavascript,
  },
  injectLocation: {
    android: androidCommands.injectLocationMode,
    ios: iosCommands.injectLocation,
  },
  stopAR: {
    android: androidCommands.stopARMode,
    ios: iosCommands.stopAR,
  },
  resumeAR: {
    android: androidCommands.resumeARMode,
    ios: iosCommands.resumeAR,
  },
  captureScreen: {
    android: androidCommands.captureScreen,
    ios: iosCommands.captureScreen,
  },
};

export default nativeFuncs;
