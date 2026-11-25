# Build React
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_AUTH_MODE=oidc
ARG VITE_OIDC_AUTHORITY=https://auth.3-111-205-87.sslip.io/realms/capstone
ARG VITE_OIDC_CLIENT_ID=capstone-frontend

RUN npm run build

# Serve React and proxy API requests
FROM nginxinc/nginx-unprivileged:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
