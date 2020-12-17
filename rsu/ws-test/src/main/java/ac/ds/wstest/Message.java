package ac.ds.wstest;

//import android.location.Location;
//import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;




public class Message
{

    // TODO: optimize
    public static class Data {
        private static final String _keyname_desribe = "describing";
        private static final String _keyname_value = "value";
        public static final String keyDescribe = _keyname_desribe;
        public static final String keyValue = _keyname_value;

        public Data(AbnormalTypes type){
            _type = type;
        }

        public String getTypeName(){
            return _type.name();
        }

        public JSONArray getValues(){
            return _values;
        }

        public AbnormalTypes type(){
            return _type;
        }

        public Data addValue(String describe, String value) {
            try {
                for (int i = 0; i < _values.length(); i++) {
                    JSONObject trg = _values.getJSONObject(i);
                    if (trg.getString(_keyname_desribe).equals(describe)
                            && trg.getString(_keyname_value).equals(value)) return this;
                }

                JSONObject rst = new JSONObject();
                rst.put(_keyname_desribe, describe)
                        .put(_keyname_value, value);
                _values.put(rst);
            } catch (JSONException e){
                e.printStackTrace();
            }

            return this;
        }

        // public boolean equals(Data other){

        //     return _value.getJSONString(_keyname_desribe)
        //             .equals(other.getJSONString(_keyname_desribe))
        //         && _value.getJSONString(_keyname_value)
        //             .equals(other.getJSONString(_keyname_value));
        // }
        private AbnormalTypes _type;
        private JSONArray _values = new JSONArray();
    }




    public Message() {
        _format = new JSONObject();
        try {
            _format
                    .put("seq", 0)
                    .put("type", "")
                    .put("localTime", 0)
                    .put("vehicleID", "")
                    .put("vehicleLisense", "")
                    .put("vehicleType", "")
                    .put("status", new JSONArray())
                    .put("latitude", "37.5846952")
                    .put("longitude", "127.0273503")
                    .put("data", new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public enum MsgType {
        normal, abnormal
    }

    public enum SensorType {
        infrared, thermoHygrometer,
        shock, gas, accelerometer,
        GPS,
    }

    public enum VehicleType {
        Passenger, Van, Truck,
    }

    private String EnumToString(Enum<?> obj){
        return obj.toString();
    }

    public Message setType(MsgType type) {
        String trg = EnumToString(type);
        try {
            _format
                    .put("type", trg);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return this;
    }
/*
    public Message setGPS(Location location){
        if(location == null) {
            Log.d("ASDFQWER", "Still null");
            return setGPS(0, 0);
        }
        return setGPS(location.getLatitude(), location.getLongitude());
    }

    public Message setGPS(double latitude, double longitude) {

        try {
            _format
                    .put("latitude", latitude)
                    .put("longitude", longitude);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return this;
    }
  */

    public Message setVehicleLisense(String lisense) throws JSONException {
        _format
                .put("vehicleLisense", lisense);
        return this;
    }

    public Message setVehicleInfo(String id, String lisense, VehicleType vType){
        try {
            _format
                    .put("vehicleID", id)
                    .put("vehicleLisense", lisense)
                    .put("vehicleType", EnumToString(vType));
        } catch (JSONException e){

        }

        return this;
    }

    public Message setTimeToNow() {
        try {
            _format
                    .put("localTime", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return this;
    }

    public Message setSeq(int seq) throws JSONException {
        _format
                .put("seq", seq);

        return this;
    }

    public Message incSeq() throws JSONException {
        this.setSeq(_format.getInt("seq") + 1);

        return this;
    }

    public Message addData(Data data) {
        if(data.getValues() == null) return this;
        try {
            String type = data.getTypeName();
            JSONArray dataArr = _format.getJSONArray("data");
            JSONObject dataDup = null;

            for (int i = 0; i < dataArr.length(); i++) {
                JSONObject trg = dataArr.getJSONObject(i);
                if (trg.getString("type").equals(type)) {
                    dataDup = trg;
                    break;
                }
            }

            if (dataDup == null) {
                JSONObject rst = new JSONObject();
                rst.put("type", data.getTypeName())
                        .put("values", data.getValues());

                dataArr.put(rst);
            } else {
                dataDup.put("values", data.getValues());
            }
        } catch (JSONException e){
            e.printStackTrace();
        }

        return this;
    }

    private JSONObject __findBy(String which, JSONArray src, String trg) throws JSONException {
        for(int i = 0; i < src.length(); i++){
            JSONObject rst = src.getJSONObject(i);
            if(rst.getString(which).equals(trg)) return rst;
        }
        return null;
    }
    public int merge(Message other) throws JSONException {
        int cntChanged = 0; // this is adHoc
        JSONObject o = other._format;
        _format
                .put("localTime", o.getLong("localTime"))
                .put("latitude", o.getDouble("latitude"))
                .put("longitude", o.getDouble("longitude"));

        JSONArray TDataSet = _format.getJSONArray("data");
        JSONArray ODataSet = o.getJSONArray("data");
        int odatasetl = ODataSet.length();
        for(int i = 0; i < odatasetl; i++){
            JSONObject OData = ODataSet.getJSONObject(i);
            String trg = OData.getString("type");
            JSONObject TData = __findBy("type", TDataSet, trg);

            if(TData == null){
                TDataSet.put(OData);
                cntChanged++;
                continue;
            }

            JSONArray TItems = TData.getJSONArray("values");
            JSONArray OItems = OData.getJSONArray("values");
            int titeml = TItems.length();
            int oiteml = OItems.length();
            for(int j = 0; j < oiteml; j++){
                JSONObject OItem = OItems.getJSONObject(j);
                boolean itemdiff = true;
                for(int k = 0; k < titeml; k++){
                    JSONObject TItem = TItems.getJSONObject(k);

                    if(OItem.getString("describing").equals(TItem.getString("describing"))
                            && OItem.getString("value").equals(TItem.getString("value"))){
                        itemdiff = false;
                        break;
                    }
                }

                if(itemdiff){
                    cntChanged++;
                    JSONObject item = new JSONObject();
                    item.put("describing", OItem.getString("describing"));
                    item.put("value", OItem.get("value"));

                    TItems.put(item);
                }
            }
        }


        return cntChanged;
    }

    public String toString(){
        return _format
                .toString();
    }

    public Message resolve() {
        try {
            _format
                    .put("localTime", 0)
                    .put("data", new JSONArray())
                    .put("latitude", "37.5846952")
                    .put("longitude", "127.0273503");
        } catch (JSONException e) {

        }

        return this;
    }

    public JSONObject getJSONObject(){
        return _format;
    }

    public Message clone(){
        Message rst = null;
        rst = new Message();

        // TODO: it causes performance issue
        // i think it is shallwo copy
        // => rst._format = new JSONObject(this._format, JSONObject.getNames(this._format));
        // add method which deep copying the JSONObject
        try {
            rst._format = new JSONObject(this._format.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return rst;
    }

    private JSONObject _format;
}
