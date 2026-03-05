# frontend Dockerfile (Next.js standalone)
FROM node:20-alpine AS builder

WORKDIR /app

# 의존성 복사 및 설치
COPY package*.json ./
RUN npm ci

# 소스 복사 (runtime-env.js는 로컬에서 미리 생성)
COPY . .

# prebuild 스킵하고 직접 next build 실행
RUN npx next build

# Runner 스테이지
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000

CMD ["node", "server.js"]
