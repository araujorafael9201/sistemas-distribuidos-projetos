package utils;
import java.io.Serializable;

public class SensorDTO implements Serializable {
    double co2; 
    double co;
    double no2;
    double so2;
    double pm5;
    double pm10;
    double umidade;
    double temperatura;
    double ruido;
    double radiacaoUV;
    long timestamp;
    long checksum;

    public SensorDTO(
        double co2, 
        double co,
        double no2,
        double so2,
        double pm5,
        double pm10,
        double umidade,
        double temperatura,
        double ruido,
        double radiacaoUV,
        long timestamp
    ) {

        this.co2 = co2;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
        this.pm5 = pm5;
        this.pm10 = pm10;
        this.umidade = umidade;
        this.temperatura = temperatura;
        this.ruido = ruido;
        this.radiacaoUV = radiacaoUV;
        this.timestamp = timestamp;
    }

    public SensorDTO(
        double co2, 
        double co,
        double no2,
        double so2,
        double pm5,
        double pm10,
        double umidade,
        double temperatura,
        double ruido,
        double radiacaoUV,
        long timestamp,
        long checksum
    ) {

        this.co2 = co2;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
        this.pm5 = pm5;
        this.pm10 = pm10;
        this.umidade = umidade;
        this.temperatura = temperatura;
        this.ruido = ruido;
        this.radiacaoUV = radiacaoUV;
        this.timestamp = timestamp;
        this.checksum = checksum;
    }

    // Getters
    public double getCo2() { return co2; }
    public double getCo() { return co; }
    public double getNo2() { return no2; }
    public double getSo2() { return so2; }
    public double getPm5() { return pm5; }
    public double getPm10() { return pm10; }
    public double getUmidade() { return umidade; }
    public double getTemperatura() { return temperatura; }
    public double getRuido() { return ruido; }
    public double getRadiacaoUV() { return radiacaoUV; }
    public long getTimestamp() { return timestamp; }
    public long getChecksum() { return checksum; }

    // Setters
    public void setCo2(double co2) { this.co2 = co2; }
    public void setCo(double co) { this.co = co; }
    public void setNo2(double no2) { this.no2 = no2; }
    public void setSo2(double so2) { this.so2 = so2; }
    public void setPm5(double pm5) { this.pm5 = pm5; }
    public void setPm10(double pm10) { this.pm10 = pm10; }
    public void setUmidade(double umidade) { this.umidade = umidade; }
    public void setTemperatura(double temperatura) { this.temperatura = temperatura; }
    public void setRuido(double ruido) { this.ruido = ruido; }
    public void setRadiacaoUV(double radiacaoUV) { this.radiacaoUV = radiacaoUV; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setChecksum(long checksum) { this.checksum = checksum; }

    public String dataString() {
        return co2 + "," + co + "," + no2 + "," + so2 + "," + pm5 + "," + pm10 + "," + umidade + "," + temperatura + "," + ruido + "," + radiacaoUV + "," + timestamp;
    }
    @Override
    public String toString() {
        return co2 + "," + co + "," + no2 + "," + so2 + "," + pm5 + "," + pm10 + "," + umidade + "," + temperatura + "," + ruido + "," + radiacaoUV + "," + timestamp + "," + checksum;
    }

    public static SensorDTO fromString(String str) {
        String[] values = str.split(",");
        if (values.length != 12) {
            throw new IllegalArgumentException("Invalid SensorDTO string format: expected 12 values");
        }
        double co2 = Double.parseDouble(values[0]);
        double co = Double.parseDouble(values[1]);
        double no2 = Double.parseDouble(values[2]);
        double so2 = Double.parseDouble(values[3]);
        double pm5 = Double.parseDouble(values[4]);
        double pm10 = Double.parseDouble(values[5]);
        double umidade = Double.parseDouble(values[6]);
        double temperatura = Double.parseDouble(values[7]);
        double ruido = Double.parseDouble(values[8]);
        double radiacaoUV = Double.parseDouble(values[9]);
        long timestamp = Long.parseLong(values[10]);
        long checksum = Long.parseLong(values[11]);
        return new SensorDTO(co2, co, no2, so2, pm5, pm10, umidade, temperatura, ruido, radiacaoUV, timestamp, checksum);
    }

}
