# 백킹서비스 배포 결과서

## 구성 환경
- 환경: docker-compose (VM 배포)
- VM: azure (20.249.211.13)
- Cloud: Azure
- 기동 일시: 2026-02-28

## VM 접속 방법
```
ssh azure
```

## 서비스 연결 정보

### PostgreSQL
| 항목 | 값 |
|------|---|
| Host (VM 내부) | localhost |
| Host (외부 접근) | 20.249.211.13 |
| Host (docker 네트워크) | postgres |
| Port (호스트 매핑) | 15432 |
| Port (컨테이너 내부) | 5432 |
| Database | member, recommendation, payment (init 스크립트로 자동 생성) |
| User | lunchpick |
| Password | P@ssw0rd$ |
| JDBC URL (VM 내부) | jdbc:postgresql://localhost:15432/{서비스명} |
| JDBC URL (docker 네트워크) | jdbc:postgresql://postgres:5432/{서비스명} |
| JDBC URL (외부 접근) | jdbc:postgresql://20.249.211.13:15432/{서비스명} |

### Redis
| 항목 | 값 |
|------|---|
| Host (VM 내부) | localhost |
| Host (외부 접근) | 20.249.211.13 |
| Host (docker 네트워크) | redis |
| Port (호스트 매핑) | 16379 |
| Port (컨테이너 내부) | 6379 |
| Connection (VM 내부) | redis://localhost:16379 |
| Connection (docker 네트워크) | redis://redis:6379 |
| Connection (외부 접근) | redis://20.249.211.13:16379 |
| DB 분할 | DB0: Redis Streams MQ, DB1: member 캐시, DB2: recommendation 캐시, DB3: payment 캐시, DB4: ai-pipeline 캐시 |

## Docker 네트워크

| 항목 | 값 |
|------|---|
| 네트워크명 | lunchpick_default (docker compose 자동 생성) |
| 앱 컨테이너 참여 방법 | `docker run --network lunchpick_default ...` |
| 백킹서비스 호스트명 | postgres, redis (docker-compose 서비스명) |

> 앱 컨테이너에서 `localhost`가 아닌 **서비스명**을 호스트로 사용해야 한다.
> 예: `DB_HOST=postgres`, `REDIS_HOST=redis`

## 기동 명령어

```bash
# VM 접속
ssh azure

# 프로젝트 디렉토리 이동
cd ~/workspace/lunchpick

# 백킹서비스 기동
docker compose up -d
```

## Health Check 결과
- [x] PostgreSQL: pg_isready 정상 (accepting connections)
- [x] Redis: PONG 확인
- [x] 서비스별 database 존재 확인 (member, recommendation, payment)
