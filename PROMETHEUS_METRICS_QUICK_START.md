# Prometheus Metrics Quick Start Guide

## Access the Metrics Endpoint

```bash
curl http://localhost:8080/actuator/prometheus | grep private_contest
```

## Available Metrics

### Counters (Always Increasing)
```
private_contest_created_total               # Contests created
private_contest_invitation_accepted_total   # Invitations accepted
private_contest_submission_total            # Submissions made
private_contest_cache_hits_total            # Cache hits
private_contest_cache_misses_total          # Cache misses
```

### Gauges (Current State)
```
private_contest_submission_queue_length     # Queue depth (from Redis)
private_contest_active_count                # LIVE contests count
private_contest_participants_total          # Total participants
```

### Histograms (Latency)
```
private_contest_submission_processing_seconds_bucket   # Latency buckets
private_contest_submission_processing_seconds_count    # Total processed
private_contest_submission_processing_seconds_sum      # Total time
```

## Quick Queries

### Current State
```promql
# Active contests right now
private_contest_active_count

# Total participants
private_contest_participants_total

# Queue depth
private_contest_submission_queue_length
```

### Rates (Operations per Second)
```promql
# Contest creation rate (last 5 min)
rate(private_contest_created_total[5m])

# Submission rate
rate(private_contest_submission_total[5m])
```

### Percentiles
```promql
# P95 submission processing time
histogram_quantile(0.95, rate(private_contest_submission_processing_seconds_bucket[5m]))

# P99 submission processing time
histogram_quantile(0.99, rate(private_contest_submission_processing_seconds_bucket[5m]))
```

### Cache Performance
```promql
# Cache hit rate (percentage)
100 * rate(private_contest_cache_hits_total[5m]) / 
  (rate(private_contest_cache_hits_total[5m]) + rate(private_contest_cache_misses_total[5m]))
```

## Sample Alerts

### Queue Backlog
```yaml
- alert: PrivateContestQueueBacklog
  expr: private_contest_submission_queue_length > 100
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Private contest submission queue is backing up"
```

### High Latency
```yaml
- alert: PrivateContestHighLatency
  expr: histogram_quantile(0.95, rate(private_contest_submission_processing_seconds_bucket[5m])) > 10
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High submission processing latency (P95 > 10s)"
```

## Grafana Dashboard Panels

### Single Stats
- **Total Contests**: `private_contest_created_total`
- **Active Now**: `private_contest_active_count`
- **Queue Depth**: `private_contest_submission_queue_length`

### Graphs
- **Creation Rate**: `rate(private_contest_created_total[5m])`
- **Submission Rate**: `rate(private_contest_submission_total[5m])`
- **Queue Over Time**: `private_contest_submission_queue_length`

### Gauge
- **Cache Hit %**: `100 * rate(private_contest_cache_hits_total[5m]) / (rate(private_contest_cache_hits_total[5m]) + rate(private_contest_cache_misses_total[5m]))`

## Testing

1. Start the application:
   ```bash
   mvn spring-boot:run
   ```

2. Access metrics:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

3. Create a contest, accept invitations, submit code

4. Verify metrics increased:
   ```bash
   curl http://localhost:8080/actuator/prometheus | grep private_contest_created_total
   ```

## Update Frequency

- **Counters**: Immediate (when event occurs)
- **Queue Length Gauge**: On Prometheus scrape (typically 15-30s)
- **Active Contests/Participants**: Every 5 minutes (scheduler)

## For More Details

See `PROMETHEUS_METRICS_REFERENCE.md` for comprehensive documentation.
