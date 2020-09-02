package ac.ds.wstest;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.neovisionaries.ws.client.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;


import java.io.IOException;

import java.net.Socket;

import java.util.UUID;

import javax.swing.text.html.parser.Entity;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.Timer;
import java.util.TimerTask;


public class WS { // RSU.java: WS ws = new WS(attURL, rmtURL);
    private WebSocket mConn = null;
    private boolean mStopped = false;
    private Retrofit mRequester;
    private TestAPI mATGService;
    private ATGReportMessage mReportMessage = new ATGReportMessage();
    private ArrayList<Evaluator> mEvaluators;
    private Message mMsg = new Message();
   
    
    HashMap<String, Boolean> car = new HashMap<String, Boolean>(); // 차량번호 저장.
    HashMap<String, Long> carTime = new HashMap<String, Long>(); // 차량시간 저장.
    HashMap<String, String> conResource_C = new HashMap<String, String>(); // 해당 차량의 컨테이너의 cpu 자원 저장
    HashMap<String, String> conResource_M = new HashMap<String, String>(); // 해당 차량의 컨테이너의 memory 자원 저장
    
    //HashMap<String, Container> carToContainer = new HashMap<String, Container>();

    private String carNum;

    /**
     * 
     * @param src Address of the data source (the data might be provided from the
     *            OBU).
     * @param dst Address of the filtered data or alert to be sent (e.g. ATG server
     *            in cloud).
     * @throws Exception
     */
    public WS(String src, String dst) throws Exception {
        // TODO: ATG: set vehicle info
        // uncomment following line:
        // mReportMessage.setVihicleInfo(id, lisense);

   	    // Create a web socket and set 5000 milliseconds as a timeout(default)
        this.mConn = new WebSocketFactory().createSocket(src);

	    // Register a listener to receive web socket events
        this.mConn.addListener(new WebSocketAdapter() {
            // message from obu
            @Override  // Called when a text message was received
            public void onTextMessage(WebSocket ws, String message) throws Exception {


                System.out.printf("\ndata recv fr car: %s\n", message);

                // 메시지에 포함되어있는 차량 번호 파악.
                int len = message.length(); // update
                Object json =  new JSONParser().parse(message.substring(1, len)); // 메시지 맨 앞에 'r'이 붙어있으므로 그 다음 단어부터 파싱.

                if (!(json instanceof JSONArray)) {
                throw new Exception("Expected the message must be JSON array");
                }
                JSONArray data = (JSONArray) json;
               
                carNum = data.get(data.size()-1).toString();
                

                
                // 차량번호의 저장유무에 따라 컨테이너 생성
                if(car.get(carNum) == null) { // RSU에 처음 접근한 차량                

                    car.put(carNum, true);
                    carTime.put(carNum, System.currentTimeMillis()); // RSU로 차량이 처음 데이터를 보냈을 때의 현 시간.
                    conResource_C.put(carNum, "1024"); // CpuShare default value: 1024
                    conResource_M.put(carNum, "256"); // Memory default value: 256MB

                    Process p_run = Runtime.getRuntime().exec(String.format("docker run -itd -e CAR_NUM=%s --network=host --name rsu-server%s rsu", carNum, carNum));
                    p_run.waitFor();  

                    Process p_setC = Runtime.getRuntime().exec(String.format("docker update --cpu-shares 1024 rsu-server%s", carNum)); 
                    p_setC.waitFor();
                    p_setC.destroy();

                    Process p_setM = Runtime.getRuntime().exec(String.format("docker update --memory=256m rsu-server%s", carNum)); 
                    p_setM.waitFor();
                    p_setM.destroy();

                    try {
                        // Rmi registry에 서버 IP, port를 설정한다.
                        //Registry registry = LocateRegistry.getRegistry("localhost",2000);
                        Registry registry = LocateRegistry.getRegistry(2000);

                        //InterfaceRMI stub = (InterfaceRMI) registry.lookup("rsuserver");
                        InterfaceRMI stub = (InterfaceRMI) registry.lookup(String.format("rsuserver%s", carNum));                      
                        String Evalmsg = stub.ServerContainer(message);  // server의 함수를 호출한다.
                        System.out.println("Evalmsg: " + Evalmsg);

                        // 차량의 첫 message의 내용 중 abnormal 상태가 감지되고, CpuShare가 "1024"이고, Memory가 "256MB"이면 자원 더 할당.
                        if(Evalmsg.charAt(0) == 'a' && conResource_C.get(carNum) == "1024" && conResource_M.get(carNum) == "256")  { 

                            System.out.printf("carNum: %s --> abnormal data detected\n", carNum);
                            System.out.printf("carNum: %s --> allocate more cpu resources\n", carNum);
                            System.out.printf("carNum: %s --> allocate more memory resources\n", carNum);

                            Process p_upC = Runtime.getRuntime().exec(String.format("docker update --cpu-shares 2048 rsu-server%s", carNum));
                            p_upC.waitFor();
                            p_upC.destroy();
                            
                            Process p_upM = Runtime.getRuntime().exec(String.format("docker update --memory=512m rsu-server%s", carNum));
                            p_upM.waitFor();
                            p_upM.destroy();

                            conResource_C.put(carNum, "2048");
                            conResource_M.put(carNum, "512");
                            System.out.printf("carNum: %s --> changed container-cpuShare: 1024 to %s\n", carNum, conResource_C.get(carNum));
                            System.out.printf("carNum: %s --> changed container-memory: 256MB to %sMB\n", carNum, conResource_M.get(carNum));
                        }                                           

                        mConn.sendText(Evalmsg);       

                    } catch (Exception e) {
                        //System.out.println("Client exception: " + e.toString());
                        //e.printStackTrace();
                        TryRmiUntilConnected(message);
                    }           

                    p_run.destroy();
                }

                else if(car.get(carNum)) { // 차량번호가 이미 저장되어있음

                    System.out.println("already carNum exists");

                    carTime.put(carNum, System.currentTimeMillis());
                    // Long current_time = System.currentTimeMillis();
                    // con.set_Time(current_time);
                    // carToContainer.put(carNum, con);
                    
                    try {
                        // Rmi registry에 서버 IP, port를 설정한다.
                        Registry registry = LocateRegistry.getRegistry("localhost",2000);

                        //InterfaceRMI stub = (InterfaceRMI) registry.lookup("rsuserver");
                        InterfaceRMI stub = (InterfaceRMI) registry.lookup(String.format("rsuserver%s",carNum));
                        String Evalmsg = stub.ServerContainer(message); // server의 함수를 호출한다.                                        
                        System.out.println("Evalmsg : " + Evalmsg);

                        // 차량의 message의 내용 중 abnormal 상태가 감지되고, CpuShare가 "1024"이고, Memory가 "256MB"이면 자원 더 할당.
                        if(Evalmsg.charAt(0) == 'a' && conResource_C.get(carNum) == "1024" && conResource_M.get(carNum) == "256")  { 

                            System.out.printf("carNum: %s --> abnormal data detected\n", carNum);
                            System.out.printf("carNum: %s --> allocate more cpu resources\n", carNum);
                            System.out.printf("carNum: %s --> allocate more memory resources\n", carNum);

                            Process p_upC = Runtime.getRuntime().exec(String.format("docker update --cpu-shares 2048 rsu-server%s", carNum));
                            p_upC.waitFor();
                            p_upC.destroy();
                            
                            Process p_upM = Runtime.getRuntime().exec(String.format("docker update --memory=512m rsu-server%s", carNum));
                            p_upM.waitFor();
                            p_upM.destroy();                           
                          
                            conResource_C.put(carNum, "2048");
                            conResource_M.put(carNum, "512");
                            System.out.printf("carNum: %s --> changed container-cpuShare: 1024 to %s\n", carNum, conResource_C.get(carNum));
                            System.out.printf("carNum: %s --> changed container-memory: 256MB to %sMB\n", carNum, conResource_M.get(carNum));
                        }    
                        
                        // 차량의 message의 내용 중 normal 상태가 감지되고, CpuShare가 "2048"이고, Memory가 "512MB"이면 자원 원상태로 할당.
                        else if(Evalmsg.charAt(0) == 'o' && conResource_C.get(carNum) == "2048" && conResource_M.get(carNum) == "512") {
                            
                            System.out.printf("carNum: %s --> normal data detected\n", carNum);
                            System.out.printf("carNum: %s --> free allocated cpu resources\n", carNum);
                            System.out.printf("carNum: %s --> free allocated memory resources\n", carNum);
                            
                            Process p_upC = Runtime.getRuntime().exec(String.format("docker update --cpu-shares 1024 rsu-server%s", carNum));
                            p_upC.waitFor();
                            p_upC.destroy();

                            Process p_upM = Runtime.getRuntime().exec(String.format("docker update --memory=256m rsu-server%s", carNum));
                            p_upM.waitFor();
                            p_upM.destroy();     

                            conResource_C.put(carNum, "1024");
                            conResource_M.put(carNum, "256");
                            System.out.printf("carNum: %s --> changed container-cpuShare: 2048 to %s\n", carNum, conResource_C.get(carNum));
                            System.out.printf("carNum: %s --> changed container-memory: 512MB to %sMB\n", carNum, conResource_M.get(carNum));
                        }


                        mConn.sendText(Evalmsg);       
                    } catch (Exception e) {
                        //System.out.println("Client exception: " + e.toString());
                        //e.printStackTrace();
                        TryRmiUntilConnected(message);
                    }  

                    
                }
                

            } // public void onTextMessage() 끝

            @Override
            public void onDisconnected(WebSocket ws, WebSocketFrame server, WebSocketFrame client,
                    boolean closedByServer) throws Exception {
                System.out.println("disconnected...");

                if (mStopped) {
                    return;
                }

                System.out.println("closed by server...");
                System.out.println("try reconnecting...");

                TryStartUntilConnected();

                System.out.println("connected");
            }

        }); // this.mConn.addListener() 끝


        // RSU가 같은 차량으로부터 10초 안에 데이터를 받지 못하면, 해당 컨테이너 제거.
        TimerTask ctTimer = new TimerTask() {
            public void run() {
                try {
                    if(car.containsValue(true)){
                        
                        for(Entry<String, Long> entry : carTime.entrySet())
                        {
                            if((System.currentTimeMillis() - carTime.get(entry.getKey())) / 1000 > 10){ // 10초
                                
                                System.out.printf("\n");    
                                System.out.println("remove container after timeout --> carNum: " + entry.getKey());
                                System.out.printf("\n"); 

                                Process p_stop = Runtime.getRuntime().exec(String.format("docker stop rsu-server%s", entry.getKey()));
                                p_stop.waitFor();
                                p_stop.destroy();
                                                
                                Process p_rm = Runtime.getRuntime().exec(String.format("docker rm rsu-server%s", entry.getKey()));
                                p_rm.waitFor();
                                p_rm.destroy();
                    
                                car.remove(entry.getKey());
                                carTime.remove(entry.getKey());
                                conResource_C.remove(entry.getKey());
                               
                            }  
                        }                       
                    }
                } catch (Exception e) {
                                        
                }
                
            }
        };

        Timer timer = new Timer();
        timer.schedule(ctTimer, 0, 1000); // 1초마다 차량의 시간 체크.        

    } // 생성자 끝



    public void Start() throws Exception {
        mStopped = false;
        mConn.connect();
    }

    public void TryStartUntilConnected() throws Exception { // RSU.java: ws.TryStartUntilConnected();
        int tried = 0;

        try {
            this.Start();
            return;
        } catch (Exception e) {

        }

        while (true) {
            try {
                mConn = mConn.recreate().connect();
                System.out.printf("recreate...\n");
                break;
            } catch (WebSocketException e) {
                System.out.printf("reconnect failed...(%d)\n", tried);
                tried++;
                Thread.sleep(1000 * 1);
            }
        }
    }

    public void Stop() {
        mStopped = true;
        mConn.disconnect();
    }


    public void TryRmiUntilConnected(String message) throws Exception{
        try {

            Registry registry = LocateRegistry.getRegistry("localhost",2000);

            //InterfaceRMI stub = (InterfaceRMI) registry.lookup("rsuserver");
            InterfaceRMI stub = (InterfaceRMI) registry.lookup(String.format("rsuserver%s",carNum));
    
            String Evalmsg = stub.ServerContainer(message);
                     
            System.out.println("Evalmsg: " + Evalmsg);       


            // 차량의 message의 내용 중 abnormal 상태가 감지되고, CpuShare가 "1024"이고, Memory가 "256MB"이면 자원 더 할당.
            if(Evalmsg.charAt(0) == 'a' && conResource_C.get(carNum) == "1024" && conResource_M.get(carNum) == "256")  { 
                
                System.out.printf("carNum: %s --> abnormal data detected\n", carNum);
                System.out.printf("carNum: %s --> allocate more cpu resources\n", carNum);
                System.out.printf("carNum: %s --> allocate more memory resources\n", carNum);

                Process p_upC = Runtime.getRuntime().exec(String.format("docker update --cpu-shares 2048 rsu-server%s", carNum));
                p_upC.waitFor();
                p_upC.destroy();
                            
                Process p_upM = Runtime.getRuntime().exec(String.format("docker update --memory=512m rsu-server%s", carNum));
                p_upM.waitFor();
                p_upM.destroy();                           
                          
                conResource_C.put(carNum, "2048");
                conResource_M.put(carNum, "512");
                System.out.printf("carNum: %s --> changed container-cpuShare: 1024 to %s\n", carNum, conResource_C.get(carNum));
                System.out.printf("carNum: %s --> changed container-memory: 256MB to %sMB\n", carNum, conResource_M.get(carNum));
            }    
                        
            // 차량의 message의 내용 중 normal 상태가 감지되고, CpuShare가 "2048"이고, Memory가 "512MB"이면 자원 원상태로 할당.
            else if(Evalmsg.charAt(0) == 'o' && conResource_C.get(carNum) == "2048" && conResource_M.get(carNum) == "512") {
                                            
                System.out.printf("carNum: %s --> normal data detected\n", carNum);
                System.out.printf("carNum: %s --> free allocated cpu resources\n", carNum);
                System.out.printf("carNum: %s --> free allocated memory resources\n", carNum);
                            
                Process p_upC = Runtime.getRuntime().exec(String.format("docker update --cpu-shares 1024 rsu-server%s", carNum));
                p_upC.waitFor();
                p_upC.destroy();

                Process p_upM = Runtime.getRuntime().exec(String.format("docker update --memory=256m rsu-server%s", carNum));
                p_upM.waitFor();
                p_upM.destroy();     

                conResource_C.put(carNum, "1024");
                conResource_M.put(carNum, "256");
                System.out.printf("carNum: %s --> changed container-cpuShare: 2048 to %s\n", carNum, conResource_C.get(carNum));
                System.out.printf("carNum: %s --> changed container-memory: 512MB to %sMB\n", carNum, conResource_M.get(carNum));
            }


            mConn.sendText(Evalmsg); 
        } catch (Exception e) {
            //System.out.println("Client exception: " + e.toString());
            //e.printStackTrace();
            TryRmiUntilConnected(message);
        }         
    }


}
