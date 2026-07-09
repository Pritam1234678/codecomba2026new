# Private Contest Prometheus Metrics - Quick Reference

## Accessing Metrics

**Endpoint**: `http://localhost:8080/actuator/prometheus`  
**Format**: Prometheus text format  
**Update Frequency**: Real-time for counters, on-scrape for gauges

## Available Metrics

### Counters (Cumulative)

```
private_contest_created_total                      # Total contests created
private_contest_invitation_accepted_total          # Total invitations accepted
private_contest_submission_total                   # Total submissions
private_contest_cache_hits_total                   # Total cache hits
private_contest_cache_misses_total                 # Total cache misses
```

### Gauges (Current Value)

```
private_contest_submission_queue_length            # Current queue length (Redis LLEN)
private_contest_active_count                       # Current number of LIVE contests
private_contest_participants_total                 # Total participants across all contests
```

### Timers/Histograms

```
private_contest_submission_processing_seconds      # Submission processing time distribution
  _bucket{le="0.001"}                             # Count of submissions < 1ms
  _bucket{le="0.01"}                              # Count of submissions < 10ms
  _bucket{le="0.1"}                               # Count of submissions < 100ms
  _bucket{le="1.0"}                               # Count of submissions < 1s
  _bucket{le="+Inf"}                              # Total count
  _count                                          # Total submissions processed
  _sum                                            # Total time spent processing
```

## Common PromQL Queries

### Rates (Operations per Second)

```promql
# Contest creation rate (last 5 minutes)
rate(private_contest_created_total[5m])

# Invitation acceptance rate
rate(private_contest_invitation_accepted_total[5m])

# Submission rate
rate(private_contest_submission_total[5m])
```

### Ratios

```promql
# Cache hit rate (percentage)
100 * rate(private_contest_cache_hits_total[5m]) / 
  (rate(private_contest_cache_hits_total[5m]) + rate(private_contest_cache_misses_total[5m]))

# Cache miss rate (percentage)
100 * rate(private_contest_cache_misses_total[5m]) / 
  (rate(private_contest_cache_hits_total[5m]) + rate(private_contest_cache_misses_total[5m]))
```

### Latencies

```promql
# P50 (median) submission processing time
histogram_quantile(0.50, rate(private_contest_submission_processing_seconds_bucket[5m]))

# P95 submission processing time
histogram_quantile(0.95, rate(private_contest_submission_processing_seconds_bucket[5m]))

# P99 submission processing time
histogram_quantile(0.99, rate(private_contest_submission_processing_seconds_bucket[5m]))

# Average submission processing time
rate(private_contest_submission_processing_seconds_sum[5m]) / 
  rate(private_contest_submission_processing_seconds_count[5m])
```

### Current State

```promql
# Current queue depth
private_contest_submission_queue_length

# Current active contests
private_contest_active_count

# Current total participants
private_contest_participants_total
```

## Alert Examples

### Queue Backlog
```yaml
alert: PrivateContestQueueBacklog
expr: private_contest_submission_queue_length > 100
for: 5m
```

### High Latency
```yaml
alert: PrivateContestHighLatency
expr: histogram_quantile(0.95, rate(private_contest_submission_processing_seconds_bucket[5m])) > 10
for: 5m
```

### Low Cache Hit Rate
```yaml
alert: PrivateContestLowCacheHitRate
expr: |
  rate(private_contest_cache_hits_total[5m]) / 
  (rate(private_contest_cache_hits_total[5m]) + rate(private_contest_cache_misses_total[5m])) < 0.5
for: 10m
```

### No Activity
```yaml
alert: PrivateContestNoActivity
expr: rate(private_contest_created_total[1h]) == 0
for: 24h
```

## Grafana Dashboard Panels

### Single Stat Panels
- **Total Contests**: `private_contest_created_total`
- **Total Invitations**: `private_contest_invitation_accepted_total`
- **Total Submissions**: `private_contest_submission_total`
- **Active Contests**: `private_contest_active_count`
- **Queue Length**: `private_contest_submission_queue_length`

### Graph Panels
- **Contest Creation Rate**: `rate(private_contest_created_total[5m])`
- **Submission Rate**: `rate(private_contest_submission_total[5m])`
- **Cache Hit Rate %**: (see ratio formula above)
- **Queue Depth Over Time**: `private_contest_submission_queue_length`
- **Processing Latency P95**: (see latency formula above)

### Heatmap Panel
- **Submission Processing Time Distribution**: `private_contest_submission_processing_seconds_bucket`

## Testing Commands

```bash
# View all private contest metrics
curl -s http://localhost:8080/actuator/prometheus | grep private_contest

# View specific metric
curl -s http://localhost:8080/actuator/prometheus | grep private_contest_created_total

# Watch metrics in real-time (requires watch command)
watch -n 1 'curl -s http://localhost:8080/actuator/prometheus | grep private_contest_created_total'

# Format output as table (requires promtool)
curl -s http://localhost:8080/actuator/prometheus | promtool check metrics

# Query via Prometheus HTTP API (if Prometheus is running)
curl -s 'http://localhost:9090/api/v1/query?query=private_contest_created_total'
```

## Integration Code Examples

### Incrementing Counters

```java
@Autowired
private PrivateContestMetricsConfig metricsConfig;

// After creating a contest
metricsConfig.incrementContestsCreated();

// After accepting an invitation
metricsConfig.incrementInvitationsAccepted();

// After submitting code
metricsConfig.incrementSubmissions();

// On cache hit
metricsConfig.incrementCacheHits();

// On cache miss
metricsConfig.incrementCacheMisses();
```

### Updating Gauges

```java
// Update active contests count (call from scheduler)
long activeCount = contestRepository.countByStatus(ContestStatus.LIVE);
metricsConfig.setActiveContestsCount(activeCount);

// Update total participants count
long totalParticipants = participantRepository.count();
metricsConfig.setTotalParticipantsCount(totalParticipants);
```

### Recording Timings

```java
// Method 1: Manual timing
Timer.Sample sample = metricsConfig.startSubmissionTimer();
try {
    processSubmission();
} finally {
    sample.stop(metricsConfig.getSubmissionProcessingTimer());
}

// Method 2: Direct recording (if duration already calculated)
long durationMs = calculateProcessingTime();
metricsConfig.recordSubmissionProcessingTime(durationMs);
```

## Troubleshooting

### Metrics Not Showing Up

1. Check endpoint is enabled:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Verify configuration:
   ```properties
   management.endpoint.prometheus.enabled=true
   management.metrics.export.prometheus.enabled=true
   ```

3. Check dependency is present:
   ```bash
   mvn dependency:tree | grep micrometer-registry-prometheus
   ```

### Queue Length Shows 0

- Verify Redis connection:
  ```bash
  redis-cli LLEN private:submission:queue
  ```

- Check Redis template configuration in Spring Boot

### Gauges Not Updating

- Gauges update on Prometheus scrape, not in real-time
- For active contests/participants, ensure scheduled job is running to update values

### Timer Shows No Data

- Timer requires explicit recording (see integration examples above)
- Verify `SubmissionWorkerPool` has been updated to record timings

## References

- [Micrometer Documentation](https://micrometer.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus Query Language (PromQL)](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Prometheus Data Source](https://grafana.com/docs/grafana/latest/datasources/prometheus/)
