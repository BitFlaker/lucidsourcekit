package com.bitflaker.lucidsourcekit.charts;

import java.util.ArrayList;
import java.util.List;

public class FrequencyList {
    private List<FrequencyData> frequencyData;

    public FrequencyList(){
        frequencyData = new ArrayList<>();
    }

    public void add(FrequencyData data){
        frequencyData.add(data);
    }

    public void remove(int i){
        frequencyData.remove(i);
    }

    public void remove(FrequencyData data){
        frequencyData.remove(data);
    }

    public float getDuration(){
        float duration = 0.0f;
        for (FrequencyData data : frequencyData) {
            duration += data.getDuration();
        }
        return duration;
    }

    public FrequencyData get(int i){
        return frequencyData.get(i);
    }

    public int size(){
        return frequencyData.size();
    }

    public float getDurationUntilAfter(int i) {
        return getDurationUntil(i) + frequencyData.get(i).getDuration();
    }

    public float getDurationUntil(int i) {
        float duration = 0.0f;
        for (int j = 0; j < i; j++){
            duration += frequencyData.get(j).getDuration();
        }
        return duration;
    }

    public double getFrequencyAtDuration(double duration) {
        double durationCounter = 0.0;
        for (int i = 0; i < frequencyData.size(); i++){
            FrequencyData current = frequencyData.get(i);
            durationCounter += current.getDuration();
            if(durationCounter >= duration) {
                if(Float.isNaN(current.getFrequencyTo())){
                    return current.getFrequency();
                }
                else{
                    double k = (current.getFrequencyTo() - current.getFrequency()) / (double)current.getDuration();
                    return k * (duration - (durationCounter - current.getDuration())) + current.getFrequency();
                }
            }
        }
        return -1;
    }
}
