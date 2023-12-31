package io.github.aaronr92.rockpaperscissorsserver.service;

import com.esotericsoftware.kryonet.Connection;
import io.github.aaronr92.rockpaperscissorsserver.component.EventPublisher;
import io.github.aaronr92.rockpaperscissorsserver.packet.server.ServerboundRemainingTimePacket;
import io.github.aaronr92.rockpaperscissorsserver.util.GameTimerTask;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class GameTimerTaskService {

    public static final Map<Long, Pair<ScheduledFuture<?>, GameTimerTask>> taskMap = new HashMap<>();
    private final ThreadPoolTaskScheduler taskScheduler;
    private final EventPublisher eventPublisher;

    /**
     * Checks whether player in taskMap or not
     * @param playerId id of player that plays game
     * @return true if still in game
     */
    public boolean isPlayerInGame(long playerId) {
        return taskMap.containsKey(playerId);
    }

    /**
     * Removes and cancels timer
     * @param playerId id of player that plays game
     */
    public GameTimerTask removeTimerTask(final long playerId) {
        var taskPair = taskMap.remove(playerId);
        taskPair.getFirst().cancel(true);
        return taskPair.getSecond();
    }

    /**
     * Updates GameTimerTask, removing and cancelling old timer and creating new
     * @param playerId id of player that plays game
     * @param connection players connection
     */
    public void updateTimerTask(
            final long playerId,
            final Connection connection
    ) {
        try {
            taskMap.remove(playerId).getFirst().cancel(true);
        } catch (Exception ignored) {}
        createTimerTask(playerId, connection);
    }

    /**
     * Creates, saves in map and launches new GameTimerTask
     * @param playerId id of player that plays game
     * @param connection players connection
     */
    public void createTimerTask(
            final long playerId,
            final Connection connection
    ) {
        createTimerTask(30, playerId, connection);
    }

    /**
     * Creates, saves in map and launches new GameTimerTask
     * @param remainingTime to game to finish
     * @param playerId id of player that plays game
     * @param connection players connection
     * @return created, saved and launched GameTimerTask
     */
    public GameTimerTask createTimerTask(
            final int remainingTime,
            final long playerId,
            final Connection connection
    ) {
        // Looks like an absolute mess
        return switch (remainingTime) {
            case 30 -> {
                connection.sendTCP(new ServerboundRemainingTimePacket(30));
                var task = new GameTimerTask(15) {
                    @Override
                    public void run() {
                        taskMap.remove(playerId);

                        createTimerTask(getRemainingTime(), playerId, connection);
                    }
                };
                var future = taskScheduler
                        .schedule(task, Instant.now().plusSeconds(task.getRemainingTime()));
                taskMap.put(playerId, Pair.of(future, task));

                yield task;
            }
            case 15 -> {
                connection.sendTCP(new ServerboundRemainingTimePacket(15));
                var task = new GameTimerTask(5) {
                    @Override
                    public void run() {
                        taskMap.remove(playerId);

                        createTimerTask(getRemainingTime(), playerId, connection);
                    }
                };
                var future = taskScheduler
                         .schedule(task, Instant.now().plusSeconds(task.getRemainingTime()));
                taskMap.put(playerId, Pair.of(future, task));

                yield task;
            }
            case 5 -> {
                connection.sendTCP(new ServerboundRemainingTimePacket(5));
                var task = new GameTimerTask(3) {
                    @Override
                    public void run() {
                        taskMap.remove(playerId);

                        createTimerTask(getRemainingTime(), playerId, connection);
                    }
                };
                var future = taskScheduler
                        .schedule(task, Instant.now().plusSeconds(task.getRemainingTime()));
                taskMap.put(playerId, Pair.of(future, task));

                yield task;
            }
            case 3 -> {
                connection.sendTCP(new ServerboundRemainingTimePacket(3));
                var task = new GameTimerTask(1) {
                    @Override
                    public void run() {
                        taskMap.remove(playerId);
                        connection.sendTCP(new ServerboundRemainingTimePacket(remainingTime));

                        createTimerTask(getRemainingTime(), playerId, connection);
                    }
                };
                var future = taskScheduler
                        .schedule(task, Instant.now().plusSeconds(task.getRemainingTime()));
                taskMap.put(playerId, Pair.of(future, task));

                yield task;
            }
            case 1 -> {
                connection.sendTCP(new ServerboundRemainingTimePacket(1));
                var task = new GameTimerTask(remainingTime) {
                    @Override
                    public void run() {
                        taskMap.remove(playerId);
                        eventPublisher.publishTimeExpiredEvent(playerId, connection);
                    }
                };
                taskScheduler
                        .schedule(task, Instant.now().plusSeconds(task.getRemainingTime()));

                yield task;
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + remainingTime);
        };
    }

}
