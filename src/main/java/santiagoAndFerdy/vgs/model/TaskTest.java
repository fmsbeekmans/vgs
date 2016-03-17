package santiagoAndFerdy.vgs.model;

import com.linkedin.parseq.*;

import java.util.concurrent.*;

/**
 * Created by Fydio on 3/16/16.
 */
public class TaskTest {
    public static void main(String[] args) throws InterruptedException {
        final int numCores = Runtime.getRuntime().availableProcessors();
        final ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        final ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();

        final Engine engine = new EngineBuilder()
                .setTaskExecutor(taskScheduler)
                .setTimerScheduler(timerScheduler)
                .build();

        Task<Integer> toInt = Task.value(1);
        Task<Integer> twice = toInt.map(i -> {
            System.out.println(i);
            System.out.println(i);

            return i + i;
        });

        Thread.sleep(3000);

        engine.run(twice);
        engine.run(twice);
    }
}
