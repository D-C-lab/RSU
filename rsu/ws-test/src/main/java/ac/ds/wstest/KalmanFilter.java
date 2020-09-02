

package ac.ds.wstest;

public class KalmanFilter {
    private double Q = 0.00001;     // 센서 노이즈 공분산 상수
    private double R = 0.001;       // 측정 공분산
    private double x = 0, P = 1, K; // 초기화

    // 생성자에 초기 센서값 넣어야함(이전 값들이 있어야함)
    KalmanFilter(double initValue) {
        x = initValue;
    }

    // 이전의 값들을 공식을 이용하여 계산
    private void MeasurementUpdate() {
        K = (P + Q) / (P + Q + R);
        P = R * (P + Q) / (R + P + Q);

    }

    // 현재값을 받아 계산된 공식을 적용하고 반환
    public double Update(double measurement) {
        MeasurementUpdate();

        x = x + (measurement - x) * K;

        return x;
    }
}

/*
package ac.ds.wstest;

public class KalmanFilter {
    private double Q = 0.00001; // 센서 노이즈 공분산 상수
    private double R = 0.001; // 측정 공분산
    private double x = 0, P = 1, K; // 초기화

    // 생성자에 초기 센서값 넣어야함(이전 값들이 있어야함)
    // KalmanFilter(double initValue) {
    // }

    // 이전의 값들을 공식을 이용하여 계산
    private void MeasurementUpdate() {
        K = (P + Q) / (P + Q + R);
        P = R * (P + Q) / (R + P + Q);

    }

    // 현재값을 받아 계산된 공식을 적용하고 반환
    public double Update(double measurement) {
        MeasurementUpdate();

        x = x + (measurement - x) * K;

        return x;
    }

}
*/