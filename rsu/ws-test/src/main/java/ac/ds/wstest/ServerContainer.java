//package ac.ds.servertest;
package ac.ds.wstest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;

import com.neovisionaries.ws.client.*;

// import java.io.PipedInputStream; // update
// import java.io.PipedOutputStream; // update

public class ServerContainer {


    private WebSocket mConn = null;

    private ArrayList<Evaluator> mEvaluators;
    private Message mMsg = new Message(); // update
   // private GpsService mGPSSvc; // update

    public ServerContainer(String message) throws Exception {   
        //System.out.println("ServerContainer.java 접속 완료");
        // TODO: ATG: set vehicle info
        // uncomment following line:
        // mReportMessage.setVihicleInfo(id, lisense);

        this.mEvaluators = new ArrayList<Evaluator>();
        while (mEvaluators.size() < 55) {
            mEvaluators.add(null);
        }
        mSetEvaluators(this.mEvaluators);

	

        int len = message.length(); // update
        Object json =  new JSONParser().parse(message.substring(1, len)); // 어떤 유형의 사고타입인지 모르는 사고정보 메시지 파싱작업?

        if (!(json instanceof JSONArray)) {
            throw new Exception("Expected the message must be JSON array");
        }
        JSONArray data = (JSONArray) json;
        HashMap<String, Double> abnormals = new HashMap<String, Double>();
                
        String carNum = data.get(data.size()-1).toString(); // update               

        mMsg.resolve(); // update


        for (int i = 0; i < data.size()-1; i++) {  // update "data.size()-1"
        Evaluator evaluator = mEvaluators.get(i);
        if (evaluator == null) {
            continue;
        } 
                                        
        Double sample;
        Object val = data.get(i);
        if (val instanceof Double) {
            sample = (Double) val;
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
        //mReportMessage.addStatus(evaluator.type().toString()); // update, ".toString() 추가"
        }
                
             
    if (abnormals.isEmpty()) {
        System.out.printf("o%s\n",data.get(data.size()-1).toString());
        return;
    }


    Message rst = mMsg.setType(Message.MsgType.abnormal).setVehicleInfo(carNum,"lisense",Message.VehicleType.Truck).setTimeToNow().clone(); // update
    

    //System.out.println("send to obu");
    System.out.printf("a%s\n", rst.toString()); // update
        
    } // 생성자 끝







    static private void mSetEvaluators(ArrayList<Evaluator> trg) {
        trg.set(0, new Evaluator("Velocity", AbnormalTypes.Collision) { // [0]속도
            private double value = 0;
            private MovingAverageFilter filter = new MovingAverageFilter();
            Double prev = new Double(-1);

            @Override
            public boolean eval(Double val) {
                return !(val< 200);
//                if(prev < 0){
//                    prev = val;
//                    return false;
//                }
//
//                filter.newNum(val);
//                val = filter.getAvg();
//
//                Double rst = Math.abs(val - prev);
//                prev = val;
//                if (rst > 8) {
//                    return true;
//                } else {
//                    return false;
//                }
            }
        });
        trg.set(9, new Evaluator("Gx", AbnormalTypes.Overturn) { // [9]가속도g(x)
            private KalmanFilter filter = new KalmanFilter(1);
            @Override
            public boolean eval(Double val) {
                val = filter.Update(val);

                if (val >= -10.0 && val <= 10) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(10, new Evaluator("Gy", AbnormalTypes.Overturn) { // [10]가속도g(y)
            private KalmanFilter filter = new KalmanFilter(1);
            @Override
            public boolean eval(Double val) {
                val = filter.Update(val);

                if (val >= -10.0 && val <= 10) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(11, new Evaluator("Gz", AbnormalTypes.Overturn) { // [11]가속도g(z)
            private KalmanFilter filter = new KalmanFilter(1);
            @Override
            public boolean eval(Double val) {
                val = filter.Update(val);

                if (val >= -10.0 && val <= 10) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(12, new Evaluator("Gc", AbnormalTypes.Overturn) { // [12]보정 가속도
            private KalmanFilter filter = new KalmanFilter(1);
            @Override
            public boolean eval(Double val) {
                val = filter.Update(val);

                if (val >= -1 && val <= 1) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(13, new Evaluator("Catalyst_Temperature", AbnormalTypes.Engine) { // [13]촉매 온도
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 200.0 && val <= 550.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(16, new Evaluator("Barometric_Pressure", AbnormalTypes.LowPressure) { // [16]공기압
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 13.6 && val <= 14.8) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(19, new Evaluator("DPF_temperature", AbnormalTypes.Engine) { // [19]배기 가스 온도
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= -41.0 && val <= 40.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(21, new Evaluator("Engine_Kw", AbnormalTypes.BatteryDead) { // [21]엔진 전력
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val == 0.0) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        trg.set(27, new Evaluator("O2_Sensor_1_W", AbnormalTypes.Engine) { // [27]산소 센서 전압
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 0.0 && val <= 1.1) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(35, new Evaluator("O2_Sensor1_E", AbnormalTypes.Engine) { // [35]산소 공기 비율
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 1.0 && val <= 2.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(38, new Evaluator("Relative_Throttle_Position", AbnormalTypes.Engine) { // [38]상대적 스로틀 포지션
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 90.0 && val <= 100.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(40, new Evaluator("Engine_RPM", AbnormalTypes.Engine) { // [40]엔진 RPM
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 0.0 && val <= 2400.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(41, new Evaluator("Engine_Coolant_Temperature", AbnormalTypes.Engine) { // [41]냉각수 온도
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 70.0 && val <= 95.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(43, new Evaluator("Fuel_Rail_Pressure", AbnormalTypes.Engine) { // [43]연료 레일 압력
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 0.0 && val <= 20000.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(47, new Evaluator("Transmission_Temperature", AbnormalTypes.Engine) { // [47]트랜스 미션 온도
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                return !(val >= 70.0 && val <= 120.0);
            }
        });
        trg.set(48, new Evaluator("Intake_Air_Temperature", AbnormalTypes.Engine) { // [48]흡기 공기 온도
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 23.0 && val <= 70.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(50, new Evaluator("Gas_leakage", AbnormalTypes.GasLeakage) { // [50]가스 유출 감지
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 0.0 && val < 1.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(51, new Evaluator("Car_Temperature", AbnormalTypes.Burning) { // [51]자동차 온도
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 0.0 && val <= 94.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        trg.set(49, new Evaluator("Rest_Gas", AbnormalTypes.OutOfGas) { // [49]남은 연료
            private MovingAverageFilter filter = new MovingAverageFilter();
            @Override
            public boolean eval(Double val) {
                filter.newNum(val);
                val = filter.getAvg();

                if (val >= 0.0 && val < 40.0) {
                    return false;
                } else {
                    return true;
                }
            }
        });

    }

}



/*
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
*/







































