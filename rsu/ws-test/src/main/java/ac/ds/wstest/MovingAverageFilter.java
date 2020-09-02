package ac.ds.wstest;

import java.util.LinkedList;
import java.util.Queue;

public class MovingAverageFilter {
    private final Queue<Double> initValue = new LinkedList<Double>();   // 이동 평균 값을 담을 리스트
    private int n;     // n은 이동 평균의 전체 값에서 나눌 때 분모 부분의 갯수 n이 너무 크면 딜레이가 생김 자료 갯수의 1/10 정도가 적당할듯
    private double sum;

    public MovingAverageFilter(){
        this.n = 1;
    }

    //  num의 합을 구해감
    public void newNum(double num) {
        sum += num;
        initValue.add(num);
        if (initValue.size() > n) {
            sum -= initValue.remove();
        }
    }

    //  이동 평균을 구해서 리턴
    public double getAvg() {
        if (initValue.isEmpty()) return 0.0;
        return sum / initValue.size();
    }
}



/*
package ac.ds.wstest;

import java.util.LinkedList;
import java.util.Queue;

public class MovingAverageFilter {
    private final Queue<Double> initValue = new LinkedList<Double>(); // 이동 평균 값을 담을 리스트
    private int n; // n은 이동 평균의 전체 값에서 나눌 때 분모 부분의 갯수 n이 너무 크면 딜레이가 생김 자료 갯수의 1/10 정도가 적당할듯
    private double sum;

    public MovingAverageFilter(int n) {
        this.n = n;
    }

    // num의 합을 구해감
    public double newNum(double num) {
        sum += num;
        initValue.add(num);
        int size = initValue.size();

        if (size > n) {
            sum -= initValue.remove();
        } else if (size < n) {
            return Double.NEGATIVE_INFINITY;
        }
        return sum / size;

    }
}
*/