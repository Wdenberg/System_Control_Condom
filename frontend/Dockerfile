FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_PROMETHEUS_URL=
ARG VITE_GRAFANA_URL=
ENV VITE_PROMETHEUS_URL=$VITE_PROMETHEUS_URL VITE_GRAFANA_URL=$VITE_GRAFANA_URL
RUN npm run build

FROM nginx:1.29-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx-main.conf /etc/nginx/nginx.conf
USER nginx
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
