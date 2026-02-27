# frontend Dockerfile
FROM node:18-alpine

WORKDIR /app

# 의존성 복사
COPY package*.json ./

# 의존성 설치
RUN npm ci --omit=dev

# 소스 복사
COPY . .

# 빌드
RUN npm run build

# 빌드 결과를 nginx로 서빙 (또는 standalone)
FROM node:20-alpine AS runner
WORKDIR /app
COPY --from=0 /app/public ./public
COPY --from=0 /app/.next/standalone ./

# 포트 노출
EXPOSE 3000

# 애플리케이션 실행
CMD ["node", "server.js"]
