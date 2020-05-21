package ac.ds.wstest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
class ATGReportMessage { // WS.java: private ATGReportMessage mReportMessage = new ATGReportMessage();
    private JSONObject mBase = new JSONObject();

    public ATGReportMessage() {
        this.mBase.put("seq", 0);
        this.mBase.put("type", "");
        this.mBase.put("localTime", 0);
        this.mBase.put("vehicleID", "");
        this.mBase.put("vehicleLisense", "");
        this.mBase.put("status", new JSONArray());
        this.mBase.put("latitude", 0);
        this.mBase.put("longitude", 0);
        this.mBase.put("data", new JSONArray());
    }

    public ATGReportMessage setVihicleInfo(String id, String lisense) {
        this.mBase.put("vehicleID", id);
        this.mBase.put("vehicleLisense", lisense);
        return this;
    }

    public ATGReportMessage setSeq(int seq) {
        this.mBase.put("seq", seq);
        return this;
    }

    public ATGReportMessage incSeq() {
        int cur = ((Integer) mBase.get("seq")).intValue();
        setSeq(cur + 1);
        return this;
    }

    public ATGReportMessage setGPS(double lat, double lon) {
        this.mBase.put("latitude", lat);
        this.mBase.put("longitude", lon);
        return this;
    }

    public ATGReportMessage setTime(long time) {
        this.mBase.put("localTime", time);
        return this;
    }

    public ATGReportMessage setTimeToNow() {
        setTime(System.currentTimeMillis());
        return this;
    }

    public String toString() {
        return mBase.toJSONString();
    }

    public ATGReportMessage addStatus(String status) {
        JSONArray statusArr = (JSONArray) this.mBase.get("status");
        for (int i = 0; i < statusArr.size(); i++) {
            String s = (String) statusArr.get(i);
            if (s.equals(status)) {
                return this;
            }
        }
        statusArr.add(status);
        return this;
    }

    public void addData(String type, String desc, double val) {
        JSONArray dataArr = (JSONArray) mBase.get("data");
        for (int i = 0; i < dataArr.size(); i++) {
            JSONObject data = (JSONObject) dataArr.get(i);
            String t = (String) data.get("Type");

            if (t != type) {
                continue;
            }

            JSONArray values = (JSONArray) data.get("Values");
            if (values.size() == 0) {
                continue;
            }

            for (int j = 0; j < values.size(); j++) {
                JSONObject value = (JSONObject) values.get(j);
                String d = (String) value.get("Describing");
                if (!d.equals("Describing")) {
                    continue;
                }
                value.put("Value", val);
                return;
            }

            JSONObject value = new JSONObject();
            value.put("Describing", desc);
            value.put("Value", value);

            values.add(value);
            return;
        }

        JSONObject value = new JSONObject();
        value.put("Describing", desc);
        value.put("Value", value);

        JSONArray values = new JSONArray();
        values.add(value);

        JSONObject data = new JSONObject();
        data.put("Type", type);
        data.put("Values", values);
    }
}
