package com.javarush.task.task30.task3008.client;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

public class BotClient extends Client {

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    @Override
    protected String getUserName() {
        return String.format("date_bot_%d", (int)(Math.random() * 100));
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    public class BotSocketThread extends SocketThread
    {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            super.processIncomingMessage(message);
            if(message.contains(": "))
            {
                String[] info = message.split(": ");
                HashMap<String, String> formats = new HashMap<>();
                formats.put("дата", "d.MM.YYYY");
                formats.put("день", "d");
                formats.put("месяц", "MMMM");
                formats.put("год", "YYYY");
                formats.put("время", "H:mm:ss");
                formats.put("час", "H");
                formats.put("минуты", "m");
                formats.put("секунды", "s");
                if(formats.containsKey(info[1]))
                {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(formats.get(info[1]));
                    Calendar calendar = new GregorianCalendar();
                    String response = String.format("Информация для %s: %s" , info[0], dateFormat.format(calendar.getTime()));
                    sendTextMessage(response);
                }

            }
        }
    }
}
