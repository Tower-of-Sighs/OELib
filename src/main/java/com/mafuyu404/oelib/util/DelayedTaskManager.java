package com.mafuyu404.oelib.util;

import com.mafuyu404.oelib.OElib;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 延迟任务管理器
 * <p>
 * 提供跨平台的延迟任务执行功能，主要用于玩家加入时的数据同步延迟。
 * 自动管理线程池资源，避免内存泄漏。
 * </p>
 */
public class DelayedTaskManager {

    private static volatile ScheduledExecutorService SCHEDULER;
    private static final Object LOCK = new Object();

    /**
     * 默认延迟时间（秒）
     */
    public static final int DEFAULT_DELAY_SECONDS = 5;

    /**
     * 获取或创建线程池实例
     */
    private static ScheduledExecutorService getScheduler() {
        if (SCHEDULER == null || SCHEDULER.isShutdown()) {
            synchronized (LOCK) {
                if (SCHEDULER == null || SCHEDULER.isShutdown()) {
                    SCHEDULER = Executors.newScheduledThreadPool(
                            2, // 使用2个线程处理延迟任务
                            r -> {
                                Thread thread = new Thread(r, "OELib-DelayedTask");
                                thread.setDaemon(true); // 设置为守护线程，避免阻止JVM关闭
                                return thread;
                            }
                    );
                    OElib.LOGGER.debug("Created new DelayedTaskManager scheduler");
                }
            }
        }
        return SCHEDULER;
    }

    /**
     * 在指定延迟后在服务器主线程中执行任务
     *
     * @param server 服务器实例
     * @param task 要执行的任务
     * @param delaySeconds 延迟时间（秒）
     */
    public static void scheduleServerTask(MinecraftServer server, Runnable task, int delaySeconds) {
        if (server == null) {
            OElib.LOGGER.warn("Cannot schedule task: server is null");
            return;
        }

        try {
            getScheduler().schedule(() -> {
                try {
                    // 确保任务在服务器主线程中执行
                    server.execute(task);
                } catch (Exception e) {
                    OElib.LOGGER.error("Error executing delayed server task", e);
                }
            }, delaySeconds, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            OElib.LOGGER.error("Failed to schedule server task - scheduler may be shutting down", e);
        }
    }

    /**
     * 使用默认延迟时间在服务器主线程中执行任务
     *
     * @param server 服务器实例
     * @param task 要执行的任务
     */
    public static void scheduleServerTask(MinecraftServer server, Runnable task) {
        scheduleServerTask(server, task, DEFAULT_DELAY_SECONDS);
    }

    /**
     * 延迟执行玩家相关任务
     *
     * @param server 服务器实例
     * @param player 玩家
     * @param playerTask 玩家任务（接收玩家参数）
     * @param delaySeconds 延迟时间（秒）
     */
    public static void schedulePlayerTask(MinecraftServer server, ServerPlayer player,
                                          Consumer<ServerPlayer> playerTask, int delaySeconds) {
        if (server == null || player == null) {
            OElib.LOGGER.warn("Cannot schedule player task: server or player is null");
            return;
        }

        try {
            getScheduler().schedule(() -> {
                try {
                    server.execute(() -> {
                        // 检查玩家是否仍然在线
                        if (server.getPlayerList().getPlayer(player.getUUID()) != null) {
                            playerTask.accept(player);
                        } else {
                            OElib.LOGGER.debug("Skipping delayed task for offline player: {}",
                                    player.getName().getString());
                        }
                    });
                } catch (Exception e) {
                    OElib.LOGGER.error("Error executing delayed player task for {}",
                            player.getName().getString(), e);
                }
            }, delaySeconds, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            OElib.LOGGER.error("Failed to schedule player task for {} - scheduler may be shutting down",
                    player.getName().getString(), e);
        }
    }

    /**
     * 使用默认延迟时间执行玩家相关任务
     *
     * @param server 服务器实例
     * @param player 玩家
     * @param playerTask 玩家任务（接收玩家参数）
     */
    public static void schedulePlayerTask(MinecraftServer server, ServerPlayer player,
                                          Consumer<ServerPlayer> playerTask) {
        schedulePlayerTask(server, player, playerTask, DEFAULT_DELAY_SECONDS);
    }

    /**
     * 延迟执行数据同步任务（专门用于DataManager）
     *
     * @param server 服务器实例
     * @param player 玩家
     * @param syncTask 同步任务
     * @param delaySeconds 延迟时间（秒）
     */
    public static void scheduleDataSync(MinecraftServer server, ServerPlayer player,
                                        Runnable syncTask, int delaySeconds) {
        schedulePlayerTask(server, player, p -> syncTask.run(), delaySeconds);
    }

    /**
     * 使用默认延迟时间执行数据同步任务
     *
     * @param server 服务器实例
     * @param player 玩家
     * @param syncTask 同步任务
     */
    public static void scheduleDataSync(MinecraftServer server, ServerPlayer player, Runnable syncTask) {
        scheduleDataSync(server, player, syncTask, DEFAULT_DELAY_SECONDS);
    }

    /**
     * 获取当前活跃任务数量（用于调试）
     *
     * @return 活跃任务数量
     */
    public static int getActiveTaskCount() {
        ScheduledExecutorService scheduler = SCHEDULER;
        if (scheduler instanceof ThreadPoolExecutor executor && !scheduler.isShutdown()) {
            return executor.getActiveCount();
        }
        return -1; // 无法获取
    }

    /**
     * 获取队列中等待的任务数量（用于调试）
     *
     * @return 等待任务数量
     */
    public static int getQueuedTaskCount() {
        ScheduledExecutorService scheduler = SCHEDULER;
        if (scheduler instanceof ThreadPoolExecutor executor && !scheduler.isShutdown()) {
            return executor.getQueue().size();
        }
        return -1; // 无法获取
    }

    /**
     * 关闭任务管理器（通常在模组卸载时调用）
     * 注意：关闭后可以重新创建新的实例
     */
    public static void shutdown() {
        synchronized (LOCK) {
            if (SCHEDULER != null && !SCHEDULER.isShutdown()) {
                OElib.LOGGER.info("Shutting down DelayedTaskManager...");
                SCHEDULER.shutdown();
                try {
                    if (!SCHEDULER.awaitTermination(10, TimeUnit.SECONDS)) {
                        OElib.LOGGER.warn("DelayedTaskManager did not terminate gracefully, forcing shutdown");
                        SCHEDULER.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    OElib.LOGGER.warn("Interrupted while waiting for DelayedTaskManager shutdown");
                    SCHEDULER.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 检查任务管理器是否已关闭
     *
     * @return 是否已关闭
     */
    public static boolean isShutdown() {
        ScheduledExecutorService scheduler = SCHEDULER;
        return scheduler == null || scheduler.isShutdown();
    }
}