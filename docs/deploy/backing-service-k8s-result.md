# 백킹서비스 K8s 배포 결과서

## 구성 환경
- Cloud: GCP
- Cluster: gce-ondal (GKE Autopilot)
- Namespace: lunchpick
- 배포 일시: 2026-03-03
- StorageClass: standard-rwo (GKE 기본, provisioner: pd.csi.storage.gke.io)

## 서비스 연결 정보

### PostgreSQL
| 항목 | 값 |
|------|---|
| Host (동일 네임스페이스) | postgresql |
| Host (FQDN) | postgresql.lunchpick.svc.cluster.local |
| Host (외부 접근) | `kubectl port-forward svc/postgresql 5432:5432` → localhost:5432 |
| Port | 5432 |
| Database | member, recommendation, payment |
| User | lunchpick |
| Password | P@ssw0rd$ |
| JDBC URL (member) | jdbc:postgresql://postgresql.lunchpick.svc.cluster.local:5432/member |
| JDBC URL (recommendation) | jdbc:postgresql://postgresql.lunchpick.svc.cluster.local:5432/recommendation |
| JDBC URL (payment) | jdbc:postgresql://postgresql.lunchpick.svc.cluster.local:5432/payment |

### Redis
| 항목 | 값 |
|------|---|
| Host (동일 네임스페이스) | redis-master |
| Host (FQDN) | redis-master.lunchpick.svc.cluster.local |
| Host (외부 접근) | `kubectl port-forward svc/redis-master 6379:6379` → localhost:6379 |
| Port | 6379 |
| 인증 | 없음 (auth.enabled: false) |
| DB 분할 | DB0: Redis Streams MQ, DB1: member 캐시, DB2: recommendation 캐시, DB3: payment 캐시, DB4: ai-pipeline 캐시 |

## 릴리즈 정보
| 서비스 | 릴리즈명 | 설치 방식 | Helm 차트 버전 | App 버전 | 이미지 |
|--------|---------|----------|--------------|---------|--------|
| PostgreSQL | postgresql | Helm bitnami/postgresql | 14.3.2 | 16.2.0 | docker.io/bitnamilegacy/postgresql:latest |
| Redis | redis | Helm bitnami/redis | 18.4.0 | 7.2.3 | docker.io/bitnamilegacy/redis:latest |

## Health Check 결과
- [x] PostgreSQL: pg_isready 정상 (`/tmp:5432 - accepting connections`)
- [x] Redis: PONG 확인
- [x] member DB 존재 확인 (Owner: lunchpick, 권한: CTc)
- [x] recommendation DB 존재 확인 (Owner: lunchpick, 권한: CTc)
- [x] payment DB 존재 확인 (Owner: lunchpick, 권한: CTc)
- [x] `helm list` → postgresql/redis 모두 `deployed` 상태 확인
- [x] `kubectl get sts` → postgresql 1/1, redis-master 1/1 Ready 확인

## K8s 리소스 상태
```
NAME                 READY   STATUS    RESTARTS   AGE
pod/postgresql-0     1/1     Running   0          3m32s
pod/redis-master-0   1/1     Running   0          3m15s

NAME                        STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS
data-postgresql-0           Bound    pvc-8ffccf89-78f2-46b0-b150-4ecfc1987921   8Gi        RWO            standard-rwo
redis-data-redis-master-0   Bound    pvc-47576c32-ed57-40cf-b63a-c00e7efaf2ff   8Gi        RWO            standard-rwo

NAME                     TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)
service/postgresql       ClusterIP   34.118.232.126   <none>        5432/TCP
service/postgresql-hl    ClusterIP   None             <none>        5432/TCP
service/redis-headless   ClusterIP   None             <none>        6379/TCP
service/redis-master     ClusterIP   34.118.236.66    <none>        6379/TCP

NAME                            READY
statefulset.apps/postgresql     1/1
statefulset.apps/redis-master   1/1
```

## PostgreSQL DB 목록 (`\l` 결과)
```
      Name      |   Owner   | Encoding |   Collate   |    Ctype    |    Access privileges
----------------+-----------+----------+-------------+-------------+-------------------------
 member         | lunchpick | UTF8     | en_US.UTF-8 | en_US.UTF-8 | lunchpick=CTc/lunchpick
 payment        | lunchpick | UTF8     | en_US.UTF-8 | en_US.UTF-8 | lunchpick=CTc/lunchpick
 postgres       | postgres  | UTF8     | en_US.UTF-8 | en_US.UTF-8 |
 recommendation | lunchpick | UTF8     | en_US.UTF-8 | en_US.UTF-8 | lunchpick=CTc/lunchpick
 template0      | postgres  | UTF8     | en_US.UTF-8 | en_US.UTF-8 | =c/postgres
 template1      | postgres  | UTF8     | en_US.UTF-8 | en_US.UTF-8 | =c/postgres
```

## 앱 서비스 환경변수 참조

### Spring Boot 서비스 공통
```yaml
# PostgreSQL (서비스별 DB 분리)
SPRING_DATASOURCE_URL: jdbc:postgresql://postgresql.lunchpick.svc.cluster.local:5432/{서비스명}
SPRING_DATASOURCE_USERNAME: lunchpick
SPRING_DATASOURCE_PASSWORD: P@ssw0rd$

# Redis
SPRING_DATA_REDIS_HOST: redis-master.lunchpick.svc.cluster.local
SPRING_DATA_REDIS_PORT: "6379"
```

### 서비스별 DB 연결
| 서비스 | DB | JDBC URL |
|--------|-----|---------|
| member-service | member | jdbc:postgresql://postgresql.lunchpick.svc.cluster.local:5432/member |
| recommendation-service | recommendation | jdbc:postgresql://postgresql.lunchpick.svc.cluster.local:5432/recommendation |
| payment-service | payment | jdbc:postgresql://postgresql.lunchpick.svc.cluster.local:5432/payment |

## 설치 명령 요약

```bash
# Bitnami Helm repo 추가
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Namespace 생성
kubectl create namespace lunchpick

# PostgreSQL 설치 (bitnami/postgresql 14.3.2)
helm install postgresql bitnami/postgresql \
  --version 14.3.2 \
  --namespace lunchpick \
  --set global.imageRegistry=docker.io \
  --set image.repository=bitnamilegacy/postgresql \
  --set image.tag=latest \
  --set auth.username=lunchpick \
  --set auth.password='P@ssw0rd$' \
  --set auth.database=member \
  --set primary.persistence.storageClass=standard-rwo \
  --set primary.persistence.size=8Gi \
  --set architecture=standalone \
  --set primary.initdb.scripts."init\.sql"="CREATE DATABASE recommendation; CREATE DATABASE payment; GRANT ALL PRIVILEGES ON DATABASE member TO lunchpick; GRANT ALL PRIVILEGES ON DATABASE recommendation TO lunchpick; GRANT ALL PRIVILEGES ON DATABASE payment TO lunchpick;"

# Redis 설치 (bitnami/redis 18.4.0)
helm install redis bitnami/redis \
  --version 18.4.0 \
  --namespace lunchpick \
  --set global.imageRegistry=docker.io \
  --set image.repository=bitnamilegacy/redis \
  --set image.tag=latest \
  --set architecture=standalone \
  --set auth.enabled=false \
  --set master.persistence.storageClass=standard-rwo \
  --set master.persistence.size=8Gi
```
