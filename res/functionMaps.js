import {UIManager} from 'react-native';

const iosCommands = UIManager.getViewManagerConfig('RNWikitude').Commands;
const androidCommands = UIManager.RNWikitude.Commands;

const nativeFuncs = {
  setUrl: {
    android: 'setUrl',
    // ios: iosCommands.setUrl,
  },
  callJS: {
    android: 'callJS',
    // ios: iosCommands.callJavascript,
  },
  injectLocation: {
    android: 'injectLocation',
    // ios: iosCommands.injectLocation,
  },
  stopAR: {
    android: 'stopAR',
    // ios: iosCommands.stopAR,
  },
  resumeAR: {
    android: 'resumeAR',
    // ios: iosCommands.resumeAR,
  },
  captureScreen: {
    android: 'captureScreen',
    // ios: iosCommands.captureScreen,
  },
};

export default nativeFuncs;
