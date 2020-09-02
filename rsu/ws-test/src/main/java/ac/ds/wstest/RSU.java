package ac.ds.wstest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Scanner;

public class RSU {
    public static void main(String[] args) throws Exception {
        System.out.println("rsu started");

        String attHost = System.getenv("ATTACH_HOST"); // obu(송신)
        String attPort = System.getenv("ATTACH_PORT");
        String rmtHost = System.getenv("REMOTE_HOST"); // Cloud-server(수신)
        String rmtPort = System.getenv("REMOTE_PORT");

        if (attHost.equals("")) {
            attHost = "localhost";
        }
        if (attPort.equals("")) {
            attPort = "5122";
        }
        if (rmtHost.equals("")) {
            rmtHost = "0.0.0.0";
        }
        if (rmtPort.equals("")) {
            rmtPort = "80";
        }

        String attURL = String.format("ws://%s:%s/rsu", attHost, attPort);
        String rmtURL = String.format("http://%s:%s", rmtHost, rmtPort);

         WS ws = new WS(attURL, rmtURL);

         ws.TryStartUntilConnected();  
         
    }
}
