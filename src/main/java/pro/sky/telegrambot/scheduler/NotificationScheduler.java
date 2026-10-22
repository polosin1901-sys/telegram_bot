package pro.sky.telegrambot.scheduler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class NotificationScheduler {

    private final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationTaskRepository notificationTaskRepository;
    private final TelegramBot telegramBot;

    public NotificationScheduler(NotificationTaskRepository notificationTaskRepository, TelegramBot telegramBot) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
    }

    @Scheduled(cron = "0 * * * * *")
    public void sendNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        logger.debug("Checking notifications for time: {}", now);

        List<NotificationTask> tasks = notificationTaskRepository.findAllByNotificationTime(now);

        if (!tasks.isEmpty()) {
            logger.info("Found {} tasks to send", tasks.size());
            for (NotificationTask task : tasks) {
                SendMessage message = new SendMessage(task.getChatId(), task.getMessage());
                telegramBot.execute(message);

                logger.info("Sent notification to chatId: {}", task.getChatId());

                notificationTaskRepository.delete(task);
            }
        }
    }
}
