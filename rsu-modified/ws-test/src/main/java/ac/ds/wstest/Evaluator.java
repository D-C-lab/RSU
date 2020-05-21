package ac.ds.wstest;

class Evaluator {
    private String mName;
    private String mType;

    public Evaluator(String name, AbnormalTypes type) {
        this.mName = name;
        this.mType = type.toString();
    }

    public String name() {
        return this.mName;
    }

    public String type() {
        return this.mType.toString();
    }

    public boolean eval(Double val) {
        return false;
    }
}