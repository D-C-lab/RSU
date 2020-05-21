package ac.ds.wstest; 

import java.util.Map;

public class RSU {
    public static void main(String[] args) throws Exception {
        System.out.println("started");

        String attHost = System.getenv("ATTACH_HOST");
        String attPort = System.getenv("ATTACH_PORT");
        String rmtHost = System.getenv("REMOTE_HOST");
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
