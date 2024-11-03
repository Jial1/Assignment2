public class performance {
  private final long startTime;
  private final String response_type;
  private final long latency;
  private final int response_code;

  public performance(long startTime, String response_type, long latency, int responseCode) {
    this.startTime = startTime;
    this.response_type = response_type;
    this.latency = latency;
    this.response_code = responseCode;
  }

  public long getStartTime() {
    return startTime;
  }

  public String getResponse_type() {
    return response_type;
  }

  public int getResponse_code() {
    return response_code;
  }

  public long getLatency() {
    return latency;
  }
}
