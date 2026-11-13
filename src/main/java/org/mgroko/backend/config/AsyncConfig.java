package org.mgroko.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración del executor dedicado a la detección automática de
 * colaboraciones.
 *
 * <p>Se mantiene separado del resto de las tareas asíncronas para que una
 * subida con muchas imágenes no bloquee otros procesos y viceversa.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "colaboracionAutomaticaExecutor")
    public ThreadPoolTaskExecutor colaboracionAutomaticaExecutor(
            @Value("${app.colaboracion-automatica.async.core-pool-size:2}") int corePoolSize,
            @Value("${app.colaboracion-automatica.async.max-pool-size:4}") int maxPoolSize,
            @Value("${app.colaboracion-automatica.async.queue-capacity:100}") int queueCapacity,
            @Value("${app.colaboracion-automatica.async.thread-name-prefix:colab-auto-}")
            String threadNamePrefix,
            @Value("${app.colaboracion-automatica.async.await-termination-seconds:60}")
            int awaitTerminationSeconds) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }

    @Bean(name = "procesamientoHistoricoExecutor")
    public ThreadPoolTaskExecutor procesamientoHistoricoExecutor(
            @Value("${app.colaboracion-automatica.historico.queue-capacity:10}")
            int queueCapacity,
            @Value("${app.colaboracion-automatica.historico.await-termination-seconds:120}")
            int awaitTerminationSeconds) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("colab-hist-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }
}
