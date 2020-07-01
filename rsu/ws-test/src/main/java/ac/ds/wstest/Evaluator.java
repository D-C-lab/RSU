package ac.ds.wstest;

class Evaluator { // WS.java: private ArrayList<Evaluator> mEvaluators; this.mEvaluators = new ArrayList<Evaluator>();
    private String mName;
    private AbnormalTypes mType; // update
    //private String mType;

    public Evaluator(String name, AbnormalTypes type) { // WS.java: trg.set(0, new Evaluator("Velocity", AbnormalTypes.Collision) {
        this.mName = name;
        this.mType = type; // update
        //this.mType = type.toString();
    }

    public String name() {
        return this.mName;
    }

    public AbnormalTypes type() {
        return this.mType; // update
        // return this.mType.toString(); 
    }

    public boolean eval(Double val) {
        return false;
    }
}

