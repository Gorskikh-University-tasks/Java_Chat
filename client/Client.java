package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    protected String getServerAddress() { return ConsoleHelper.readString(); }
    protected int getServerPort() { return ConsoleHelper.readInt(); }
    protected String getUserName() { return ConsoleHelper.readString();}
    protected boolean shouldSendTextFromConsole() { return true; }
    protected SocketThread getSocketThread() { return new SocketThread();}

    protected void sendTextMessage(String text)
    {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Во время отправки messageText произошло исключение IOException");
            clientConnected = false;
        }
    }

    public void run()
    {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        try {
            synchronized (this){
                wait();
            }
            if(!clientConnected)
                ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
            else
            {
                ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
                while (clientConnected)
                {
                    String str = ConsoleHelper.readString();
                    if(str.equals("exit")) break;
                    if(shouldSendTextFromConsole())
                        sendTextMessage(str);
                }
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Ошибка подключения");
        }
    }

    public class SocketThread extends Thread
    {
        protected void processIncomingMessage(String message) { ConsoleHelper.writeMessage(message);}
        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("Участник с именем %s присоединился к чату.", userName));
        }
        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("Участник с именем %s покинул чат.", userName));
        }
        protected void notifyConnectionStatusChanged(boolean clientConnected)
        {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException
        {
            while(true)
            {
                Message message = connection.receive();
                MessageType type = message.getType();
                if(type == MessageType.NAME_REQUEST)
                {
                    ConsoleHelper.writeMessage("Введите ваше имя: ");
                    String  userName = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, userName));
                }
                else if(type == MessageType.NAME_ACCEPTED)
                {
                    notifyConnectionStatusChanged(true); return;
                }
                else
                    throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException
        {
            while (true)
            {
                Message servMessage = connection.receive();
                MessageType type = servMessage.getType();
                if(type == MessageType.TEXT)
                    processIncomingMessage(servMessage.getData());
                else if(type == MessageType.USER_ADDED)
                    informAboutAddingNewUser(servMessage.getData());
                else if(type == MessageType.USER_REMOVED)
                    informAboutDeletingNewUser(servMessage.getData());
                else
                    throw new IOException("Unexpected MessageType");
            }

        }

        @Override
        public void run() {
            try {
                ConsoleHelper.writeMessage("Введите адрес сервера: ");
                String host = getServerAddress();
                ConsoleHelper.writeMessage("Введите порт сервера: ");
                int port = getServerPort();
                Socket socket = new Socket(host, port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (Exception e) {
                notifyConnectionStatusChanged(false);
            }
        }
    }
}
