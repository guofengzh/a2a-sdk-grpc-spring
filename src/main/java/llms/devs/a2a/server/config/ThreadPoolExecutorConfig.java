package llms.devs.a2a.server.config;

import org.a2aproject.sdk.server.util.async.Internal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolExecutorConfig {
    @Bean
    @Internal
    public Executor a2aInternal() {
        // 1. Get available cores
        int cpuCores = Runtime.getRuntime().availableProcessors();

        // 2. Define pool sizes (Example for CPU-bound tasks)
        int corePoolSize = cpuCores;
        int maxPoolSize = cpuCores * 2; // Upper limit if queue fills up
        long keepAliveTime = 60L;       // Time to destroy idle extra threads

        // 3. Define a bounded queue to prevent OutOfMemoryError
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);

        // 4. Create the executor
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy() // Handles overflow safely
        );

        // Optional: Pre-start core threads to reduce initial latency
        executor.prestartAllCoreThreads();

        return executor;
    }
}
