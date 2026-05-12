package dev.minirdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        // 터미널에서 한 줄씩 입력받기 위한 객체다.
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // 사용자가 종료 명령을 입력할 때까지 계속 반복한다.
        while (true) {
            // 사용자가 명령을 입력할 수 있다는 표시를 출력한다.
            System.out.print("mini-rdb> ");
            System.out.flush();

            // 사용자가 입력한 한 줄을 읽는다.
            String input = reader.readLine();

            // Ctrl+D 같은 입력 종료가 들어오면 프로그램을 종료한다.
            if (input == null) {
                break;
            }

            // 앞뒤 공백과 줄바꿈을 제거한다.
            String command = input.trim();

            // .exit 명령이 들어오면 프로그램을 종료한다.
            if (command.equals(".exit")) {
                break;
            }

            // 빈 입력은 무시하고 다시 입력을 기다린다.
            if (command.isEmpty()) {
                continue;
            }

            // 아직 작업 2에서는 실제 명령 실행을 구현하지 않는다.
            System.out.println("unrecognized command: " + command);
        }
    }
}
