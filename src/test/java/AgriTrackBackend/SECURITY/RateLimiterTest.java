package AgriTrackBackend.SECURITY;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allowsUpToTheLimitThenRejects() {
        RateLimiter limiter = new RateLimiter();
        String key = "1.2.3.4:login";

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryConsume(key, 5, 60_000)).isTrue();
        }
        assertThat(limiter.tryConsume(key, 5, 60_000)).isFalse();
    }

    @Test
    void differentKeysAreIndependent() {
        RateLimiter limiter = new RateLimiter();
        assertThat(limiter.tryConsume("ip1:login", 1, 60_000)).isTrue();
        assertThat(limiter.tryConsume("ip1:login", 1, 60_000)).isFalse();
        assertThat(limiter.tryConsume("ip2:login", 1, 60_000)).isTrue();
    }

    @Test
    void windowExpiryAllowsRequestsAgain() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        String key = "ip:otp";
        assertThat(limiter.tryConsume(key, 1, 50)).isTrue();
        assertThat(limiter.tryConsume(key, 1, 50)).isFalse();
        Thread.sleep(60);
        assertThat(limiter.tryConsume(key, 1, 50)).isTrue();
    }
}
