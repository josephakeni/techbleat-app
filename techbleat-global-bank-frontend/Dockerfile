FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

ARG VITE_USER_API=http://localhost:8000
ARG VITE_TX_API=http://localhost:8080
ARG VITE_ACTIVITY_API=http://localhost:8001

ENV VITE_USER_API=$VITE_USER_API
ENV VITE_TX_API=$VITE_TX_API
ENV VITE_ACTIVITY_API=$VITE_ACTIVITY_API

RUN npm run build

FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
