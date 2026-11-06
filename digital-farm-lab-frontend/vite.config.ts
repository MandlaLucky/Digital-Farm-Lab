import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      port: 3000,
      host: '127.0.0.1',
      proxy: {
        '/api': {
          target: env.SPRING_API_URL || 'http://localhost:8081',
          changeOrigin: true,
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              // Inject the JWT so the browser never has to hold it
              const token = env.SPRING_DEV_TOKEN
              if (token) {
                proxyReq.setHeader('Authorization', `Bearer ${token}`)
              }
              // Strip Origin/Referer — this is a server-to-server call,
              // CORS must not apply. Spring Boot's CORS filter rejects
              // 127.0.0.1:3000 because only localhost:3000 is allowed.
              proxyReq.removeHeader('origin')
              proxyReq.removeHeader('referer')
            })
          },
        },
      },
    },
  }
})