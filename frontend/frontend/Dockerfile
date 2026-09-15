# ---------- faza 1: build ----------
FROM node:22-alpine AS build
WORKDIR /app

# Isti trik kao kod Mavena: package*.json prvi, da se npm ci kesira.
COPY package*.json ./
RUN npm ci

COPY . .
RUN npx ng build --configuration production

# ---------- faza 2: runtime ----------
# Angular je na kraju samo staticki HTML/JS/CSS - Node u produkciji ne treba.
# nginx ih servira i prosljedjuje /api i /ws backendu (isto sto proxy.conf.json
# radi u razvoju).
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
