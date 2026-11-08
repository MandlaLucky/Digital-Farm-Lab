import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [
      react(),
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['apple-touch-icon.png', 'pwa-192.png', 'pwa-512.png'],
        manifest: {
          name: 'Digital Farm Lab — AgriConnect 360',
          short_name: 'FarmLab',
          description: 'Digital-twin farm simulator for A14 AgriConnect 360',
          theme_color: '#07100e',
          background_color: '#07100e',
          display: 'standalone',
          orientation: 'portrait-primary',
          start_url: '/',
          scope: '/',
          categories: ['education', 'productivity'],
          icons: [
            { src: 'pwa-192.png', sizes: '192x192', type: 'image/png' },
            { src: 'pwa-512.png', sizes: '512x512', type: 'image/png' },
            { src: 'pwa-512-maskable.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
          ]
        },
        workbox: {
          globPatterns: ['**/*.{js,css,html,png,svg,woff2}'],
          navigateFallback: 'index.html',
          navigateFallbackDenylist: [/^\/api\//],
          runtimeCaching: [
            {
              urlPattern: ({ url }) => url.pathname.startsWith('/api/'),
              handler: 'NetworkFirst',
              options: {
                cacheName: 'farmlab-api',
                expiration: { maxEntries: 100, maxAgeSeconds: 3600 },
                networkTimeoutSeconds: 10,
                cacheableResponse: { statuses: [0, 200, 201, 204] }
              }
            }
          ]
        },
        devOptions: { enabled: false }
      })
    ],
    server: {
      port: 3000,
      host: '127.0.0.1',
      proxy: {
        '/api': {
          target: env.SPRING_API_URL || 'http://localhost:8081',
          changeOrigin: true,
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              const token = env.SPRING_DEV_TOKEN
              if (token) proxyReq.setHeader('Authorization', `Bearer ${token}`)
              proxyReq.removeHeader('origin')
              proxyReq.removeHeader('referer')
            })
          }
        }
      }
    }
  }
})