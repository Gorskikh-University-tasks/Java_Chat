package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        ConsoleHelper.writeMessage("Введите порт сервера: ");
        int port = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Сервер запущен");

            while (true)
            {
               Socket clientSocket = serverSocket.accept();
               new Handler(clientSocket).start();
            }

        } catch (IOException e) {}
    }

    public static void sendBroadcastMessage(Message message)
    {
        for(Map.Entry<String, Connection> pair : connectionMap.entrySet())
        {
            try {
                pair.getValue().send(message);
            } catch (IOException e) {
                System.out.println("Не удалось отправить сообщение пользователю: " + pair.getKey());
            }
        }
    }

    private static class Handler extends Thread{
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException
        {
            while (true)
            {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message messageName = connection.receive();
                if(messageName.getType() == MessageType.USER_NAME && messageName.getData() != null && !messageName.getData().equals(""))
                {
                    if(!connectionMap.containsKey(messageName.getData()))
                    {
                        connectionMap.put(messageName.getData(), connection);
                        connection.send(new Message(MessageType.NAME_ACCEPTED));
                        return messageName.getData();
                    }
                }
            }
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException
        {
            for(Map.Entry<String, Connection> pair : connectionMap.entrySet())
            {
                if(!pair.getKey().equals(userName))
                    connection.send(new Message(MessageType.USER_ADDED, pair.getKey()));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException
        {
            while(true)
            {
                Message clientMes = connection.receive();
                if(clientMes.getType() == MessageType.TEXT && clientMes.getData() != null && !clientMes.getData().equals(""))
                {
                    String stringMes = String.format("%s: %s", userName, clientMes.getData());
                    sendBroadcastMessage(new Message(MessageType.TEXT, stringMes));
                }
                else if(clientMes.getType() != MessageType.TEXT)
                    ConsoleHelper.writeMessage("Невозможно отправить это сообщение");
            }
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом: " + socket.getRemoteSocketAddress());
            String userName = null;
            try(Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                sendListOfUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (Exception e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом");
                 }
                 if(userName != null){
                     connectionMap.remove(userName);
                     sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                 }
                 ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");

        }
    }
}















