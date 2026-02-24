# Monitoring Setup - Prometheus & Grafana

This directory contains the monitoring stack configuration for the Mini Amazon microservices project.

## Architecture

- **Prometheus**: Collects and stores metrics from all Spring Boot services
- **Grafana**: Visualizes metrics with pre-configured dashboards

## Quick Start

### 1. Start your Spring Boot services

```bash
# From the project root
./mvnw spring-boot:run -pl api-gateway
./mvnw spring-boot:run -pl order-service
./mvnw spring-boot:run -pl warehouse-service
./mvnw spring-boot:run -pl world-connector
```

### 2. Start Prometheus and Grafana

```bash
# From the project root
docker-compose up -d
```

### 3. Access the dashboards

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001
  - Default credentials: `admin` / `admin`

## Available Metrics

Each service exposes the following endpoints:

- **Health**: `http://localhost:<port>/actuator/health`
- **Metrics**: `http://localhost:<port>/actuator/metrics`
- **Prometheus**: `http://localhost:<port>/actuator/prometheus`

### Service Ports

- API Gateway: 8080
- Order Service: 8081
- Warehouse Service: 8082
- World Connector: 8089

## Grafana Dashboards

The setup includes a pre-configured dashboard that shows:

- HTTP request rates per service
- JVM heap memory usage
- CPU usage
- Service health status

Access it at: **Grafana > Dashboards > Mini Amazon - Spring Boot Microservices**

## Customization

### Adding Custom Metrics

In your Spring Boot code:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

@Service
public class MyService {
    private final Counter myCounter;

    public MyService(MeterRegistry registry) {
        this.myCounter = Counter.builder("my_custom_metric")
            .description("Description of my metric")
            .register(registry);
    }

    public void doSomething() {
        myCounter.increment();
        // your logic
    }
}
```

### Modifying Prometheus Scrape Interval

Edit `monitoring/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s  # Change this value
```

### Creating New Grafana Dashboards

1. Create your dashboard in the Grafana UI
2. Export as JSON
3. Save to `monitoring/grafana/dashboards/`
4. Restart Grafana: `docker-compose restart grafana`

## Stopping the Monitoring Stack

```bash
docker-compose down
```

To remove volumes (will delete all data):

```bash
docker-compose down -v
```

## Troubleshooting

### Services not showing in Prometheus

1. Verify services are running: `curl http://localhost:8080/actuator/health`
2. Check Prometheus targets: http://localhost:9090/targets
3. Ensure `host.docker.internal` resolves (on Linux, use `172.17.0.1` instead)

### Grafana shows "No Data"

1. Verify Prometheus datasource is configured
2. Check that metrics are being collected in Prometheus
3. Adjust the time range in Grafana (top right corner)

## Production Considerations

Before deploying to production:

1. **Change default credentials** in `docker-compose.yml`
2. **Enable authentication** for Prometheus
3. **Configure persistent storage** properly
4. **Set up alerting** in Prometheus/Grafana
5. **Use proper service discovery** instead of static targets
6. **Enable HTTPS** for both Prometheus and Grafana
