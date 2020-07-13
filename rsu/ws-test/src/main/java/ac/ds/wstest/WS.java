package ac.ds.wstest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.neovisionaries.ws.client.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import java.time.Instant;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;

//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;


public class WS { // RSU.java: WS ws = new WS(attURL, rmtURL);
    private WebSocket mConn = null;
    private boolean mStopped = false;
    private Retrofit mRequester;
    private TestAPI mATGService;
    private ATGReportMessage mReportMessage = new ATGReportMessage();
    private ArrayList<Evaluator> mEvaluators;
    private Message mMsg = new Message(); // update
    private File file = new File("test1.txt");
    private FileWriter writer = null;

   // private GpsService mGPSSvc; // update

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

        this.mEvaluators = new ArrayList<Evaluator>();
        while (mEvaluators.size() < 55) {
            mEvaluators.add(null);
        }
        mSetEvaluators(this.mEvaluators);

	// Create a web socket and set 5000 milliseconds as a timeout(default)
        this.mConn = new WebSocketFactory().createSocket(src);

	// Register a listener to receive web socket events
        this.mConn.addListener(new WebSocketAdapter() {

            // message from obu
            @Override
            public void onTextMessage(WebSocket ws, String message) throws Exception {
                long epochMilli = Instant.now().toEpochMilli();

                System.out.printf("msg from rsu: %s", message);

                Call<String> c = mATGService.report("{\"greet\":\"hello world!\"}");
                // send message to ATG
                c.enqueue(new Callback<String>() {
                    // response from ATG
                    @Override
                    public void onResponse(Call<String> call, Response<String> res) {
                        // TODO: ATG: handle response

                        if (res.body() == null) {
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        //System.out.println("got error");
                    }
                });
                /* ????????
                if (true) {
                    return;
                }
                */
                // System.out.println("rcv msg");

                int len = message.length();
                Object json =  new JSONParser().parse(message.substring(1, len)); // 어떤 유형의 사고타입인지 모르는 사고정보 메시지 파싱작업?

                if (!(json instanceof JSONArray)) {
                    throw new Exception("Expected the message must be JSON array");
                }
                JSONArray data = (JSONArray) json;
                HashMap<String, Double> abnormals = new HashMap<String, Double>();
                
                String carNum = data.get(data.size()-1).toString(); // update               

                mMsg.resolve(); // update

                Object startTImeObject = data.get(data.size()-3);
                long startTime = (Long) startTImeObject;
                long completeTime = epochMilli - startTime;

                for (int i = 0; i < data.size()-3; i++) {  // update "data.size()-1"
                    Evaluator evaluator = mEvaluators.get(i);
                    if (evaluator == null) {
                        continue;
                    }

                    Double sample;
                    Object val = data.get(i);
                    if (val instanceof Double) {
                        sample = (Double) val;
                    } else if (val instanceof Integer) {
                        sample = new Double((Integer) val);
                    } else if (val instanceof String) {
                        continue;
                    } else if (val instanceof Float) {
                        sample = new Double((Float) val);
                    } else if (val instanceof Long) {
                        sample = new Double((Long) val);
                    } else {
                        continue;
                    }

                    boolean isAbnormal = evaluator.eval(sample);
                    if (!isAbnormal) {
                        continue;
                    }

                    mMsg.addData(new Message.Data(evaluator.type()).addValue(evaluator.name(), sample.toString())); // update

                    abnormals.put(evaluator.name(), sample);
                    mReportMessage.addStatus(evaluator.type().toString()); // update, ".toString() 추가"
                }

                // there is no abnormal state
                // so, it is not needed to send report the server.
                if (abnormals.isEmpty()) {
                    System.out.println("normal!");
                    mConn.sendText("o["+data.get(data.size()-2).toString()+"]");
                    return;
                }
               

                  

                // TODO: send alert to obu.
                // n[{Abnormal information}]
                // need to discuss how to format {Abnormal information}.
                //
                // mConn.sendText(abnormals.to???l!");

                // TODO: send alert to obu.
                // n[{Abnormal information}]
                // need to discuss how to format {Abnormal information}.
                //
                // mConn.sendText(abnormals.to???

                int packetNum = Integer.parseInt(data.get(data.size()-2).toString());

                try {
                    writer = new FileWriter(file, true);
                    writer.write(data.get(data.size()-2).toString()+","+Long.toString(completeTime)+"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if(writer != null) writer.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }


                Message rst = mMsg.setType(Message.MsgType.abnormal).setTimeToNow().setSeq(packetNum).clone(); // update
               
                //Message rst = mMsg.setType(Message.MsgType.abnormal).setTimeToNow()
                //.setGPS(mGPSSvc.getLatitude(), mGPSSvc.getLongitude()).clone();

                mConn.sendText("a"+rst);

                mReportMessage.setTimeToNow().incSeq();
                for (Map.Entry<String, Double> abnormal : abnormals.entrySet()) {
                    mReportMessage.addData(abnormal.getKey(), "", abnormal.getValue());
                }

                Call<String> call = mATGService.report(mReportMessage.toString());

                // send message to ATG  비동기적 호출
                call.enqueue(new Callback<String>() {
                    // response from ATG
                    @Override
                    public void onResponse(Call<String> call, Response<String> res) {
                        // TODO: ATG: handle response

                        if (res.body() == null) {
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        System.out.println("got error");
                    }
                });
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

        mRequester = new Retrofit.Builder().baseUrl(dst).addConverterFactory(ScalarsConverterFactory.create()).build();
        mATGService = mRequester.create(TestAPI.class);
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





    static private void mSetEvaluators(ArrayList<Evaluator> trg) {
        trg.set(0, new Evaluator("Velocity", AbnormalTypes.Collision) { // [0]속도
            Double prev = new Double(-1);

            @Override
            public boolean eval(Double val) {
                if (prev < -1) {
                    prev = val;
                    return false;
                }

                Double rst = Math.abs(val - prev);
                return rst > 8;
            }
        });
        trg.set(9, new Evaluator("Gx", AbnormalTypes.Overturn) { // [9]가속도g(x)
            @Override
            public boolean eval(Double val) {

                if (val >= -10.0 && val <= 10) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(10, new Evaluator("Gy", AbnormalTypes.Overturn) { // [10]가속도g(y)
            @Override
            public boolean eval(Double val) {

                if (val >= -10.0 && val <= 10) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(11, new Evaluator("Gz", AbnormalTypes.Overturn) { // [11]가속도g(z)
            @Override
            public boolean eval(Double val) {

                if (val >= -10.0 && val <= 10) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(12, new Evaluator("Gc", AbnormalTypes.Overturn) { // [12]보정 가속도
            @Override
            public boolean eval(Double val) {

                if (val >= -1 && val <= 1) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(13, new Evaluator("Catalyst_Temperature", AbnormalTypes.Engine) { // [13]촉매 온도
            @Override
            public boolean eval(Double val) {

                if (val >= 200.0 && val <= 550.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(16, new Evaluator("Barometric_Pressure", AbnormalTypes.LowPressure) { // [16]공기압
            @Override
            public boolean eval(Double val) {
                if (val >= 13.6 && val <= 14.8) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(19, new Evaluator("DPF_temperature", AbnormalTypes.Engine) { // [19]배기 가스 온도
            @Override
            public boolean eval(Double val) {
                if (val >= -41.0 && val <= 40.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(21, new Evaluator("Engine_Kw", AbnormalTypes.BatteryDead) { // [21]엔진 전력
            @Override
            public boolean eval(Double val) {
                System.out.println(val);
                return val == 0.0;
            }
        });
        trg.set(27, new Evaluator("O2_Sensor_1_W", AbnormalTypes.Engine) { // [27]산소 센서 전압
            @Override
            public boolean eval(Double val) {
                if (val >= 0.0 && val <= 1.1) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(35, new Evaluator("O2_Sensor1_E", AbnormalTypes.Engine) { // [35]산소 공기 비율
            @Override
            public boolean eval(Double val) {
                if (val >= 1.0 && val <= 2.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(38, new Evaluator("Relative_Throttle_Position", AbnormalTypes.Engine) { // [38]상대적 스로틀 포지션
            @Override
            public boolean eval(Double val) {
                if (val >= 90.0 && val <= 100.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(40, new Evaluator("Engine_RPM", AbnormalTypes.Engine) { // [40]엔진 RPM
            @Override
            public boolean eval(Double val) {
                if (val >= 0.0 && val <= 2400.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(41, new Evaluator("Engine_Coolant_Temperature", AbnormalTypes.Engine) { // [41]냉각수 온도
            @Override
            public boolean eval(Double val) {
                if (val >= 70.0 && val <= 95.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(43, new Evaluator("Fuel_Rail_Pressure", AbnormalTypes.Engine) { // [43]연료 레일 압력
            @Override
            public boolean eval(Double val) {
                if (val >= 0.0 && val <= 20000.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(47, new Evaluator("Transmission_Temperature", AbnormalTypes.Engine) { // [47]트랜스 미션 온도
            @Override
            public boolean eval(Double val) {
                return !(val >= 70.0 && val <= 120.0);
            }
        });
        trg.set(48, new Evaluator("Intake_Air_Temperature", AbnormalTypes.Engine) { // [48]흡기 공기 온도

            @Override
            public boolean eval(Double val) {

                if (val >= 23.0 && val <= 70.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(50, new Evaluator("Gas_leakage", AbnormalTypes.GasLeakage) { // [50]가스 유출 감지
            @Override
            public boolean eval(Double val) {
                if (val >= 0.0 && val < 1.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(51, new Evaluator("Car_Temperature", AbnormalTypes.Burning) { // [51]자동차 온도
            @Override
            public boolean eval(Double val) {

                if (val >= 0.0 && val <= 94.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(49, new Evaluator("Rest_Gas", AbnormalTypes.OutOfGas) { // [49]남은 연료
            @Override
            public boolean eval(Double val) {
                if (val >= 0.0 && val < 40.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
    }




}




