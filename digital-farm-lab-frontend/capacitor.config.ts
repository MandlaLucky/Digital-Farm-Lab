import type { CapacitorConfig } from '@capacitor/cli'

// Production build — loads the live Vercel frontend.
// The frontend itself calls Render backend (hardcoded in api.ts).
const config: CapacitorConfig = {
  appId: 'com.mlm.farmlab',
  appName: 'Digital Farm Lab',
  webDir: 'dist',
  server: {
    url: 'https://digital-farm-lab.vercel.app',
    cleartext: false,
    androidScheme: 'https'
  }
}

export default config