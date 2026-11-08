import type { CapacitorConfig } from '@capacitor/cli'

// Set CAPACITOR_SERVER_URL before building the APK, e.g.:
//   $env:CAPACITOR_SERVER_URL = 'http://192.168.1.42:8080'; npx cap sync android
// Falls back to the dev machine's LAN IP below when unset.
const serverUrl = process.env.CAPACITOR_SERVER_URL || 'http://10.2.43.191:8080'

const config: CapacitorConfig = {
  appId: 'com.mlm.farmlab',
  appName: 'Digital Farm Lab',
  webDir: 'dist',
  server: {
    url: serverUrl,
    cleartext: true,
    androidScheme: 'http'
  }
}

export default config