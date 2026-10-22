package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final NotificationTaskRepository notificationTaskRepository;

    private final TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository, TelegramBot telegramBot) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
    }

    private static final Pattern MESSAGE_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    private void processingUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }
        String text = update.message().text();
        Long chatId = update.message().chat().id();

        if ("/start".equals(text)) {
            SendMessage greeting = new SendMessage(chatId, "Добро пожаловать в бот-напоминалку!");
            telegramBot.execute(greeting);
            return;
        }
        Matcher matcher = MESSAGE_PATTERN.matcher(text);

        if (matcher.matches()) {
            String dateString = matcher.group(1);
            String message = matcher.group(3);
            try {
                LocalDateTime notificationTime = LocalDateTime.parse(dateString, DATE_FORMATTER);

                NotificationTask notificationTask = new NotificationTask(chatId, notificationTime, message);

                notificationTaskRepository.save(notificationTask);

                telegramBot.execute(new SendMessage(chatId, "Напоминание успешно сохранено"));
                logger.info("Saved new task - {}", notificationTask);
            } catch (DateTimeParseException e) {
                logger.warn("User entered invalid date format: - {}", dateString, e);
                telegramBot.execute(new SendMessage(chatId, "Ошибка: неверный формат даты. Используй: дд.мм.гггг чч:мм Текст"));
            }
            catch (Exception e) {
                logger.error("Unexpected error while saving task for chatId: {}", chatId, e);
                telegramBot.execute(new SendMessage(chatId,
                        "Произошла внутренняя ошибка сервера. Попробуйте позже."));
            }
        } else {
            telegramBot.execute(new SendMessage(chatId, "Я тебя не понял. Используй формат: дд.мм.гггг чч:мм Текст задачи"));
        }
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(this::processingUpdate);
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
