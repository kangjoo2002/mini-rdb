package dev.minirdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        Table table = new Table();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("mini-rdb> ");
            System.out.flush();

            String input = reader.readLine();

            if (input == null) {
                break;
            }

            String command = input.trim();

            if (command.equals(".exit")) {
                break;
            }

            if (command.isEmpty()) {
                continue;
            }

            if (command.equals("select")) {
                for (Row row : table.rows()) {
                    System.out.println(row.id() + " " + row.name());
                }
                continue;
            }

            if (command.startsWith("insert ")) {
                String[] parts = command.split("\\s+");

                if (parts.length != 3) {
                    System.out.println("usage: insert <id> <name>");
                    continue;
                }

                int id;
                try {
                    id = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    System.out.println("id must be a number");
                    continue;
                }

                String name = parts[2];
                table.insert(new Row(id, name));
                System.out.println("ok");
                continue;
            }

            System.out.println("unrecognized command: " + command);
        }
    }
}
