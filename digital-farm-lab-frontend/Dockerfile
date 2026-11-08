# syntax=docker/dockerfile:1
FROM node:24-alpine AS build
WORKDIR /app

RUN corepack enable && corepack prepare pnpm@12 --activate

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --no-frozen-lockfile --config.strictDepBuilds=false

COPY . .
RUN pnpm build

FROM nginx:alpine
RUN apk add --no-cache curl
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -sf http://localhost/ > /dev/null || exit 1