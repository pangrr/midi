package com.midisheetmusic;


import android.util.Log;

import java.util.Map;
import java.util.Random;

/**
 * Created by rpang on 10/18/15.
 * Representing a particle in particle filtering.
 */
public class Particle {
    private double initSpeed;
    private double pulse; // Position of this particle in the MidiFile in terms of pulse.
    private double speed;
    private Random random;
    private MidiSegments segments;
    private int segmentIndex;    // Position in terms of segment.
    private double weight;

    public Particle(double pulse, double initSpeed, double speed, MidiSegments segments, int segmentIndex) {
        this.initSpeed = initSpeed;
        this.speed = speed;
        random = new Random();
        this.pulse = pulse;
        this.segments = segments;
        this.weight = 0;
        this.segmentIndex = Math.max(0, segmentIndex);
    }

    public void setWeight(Map<Integer, Double> weights) {
        this.weight = weights.get(segmentIndex);
    }


    private void updateSegmentIndex() {
        for(int i = segmentIndex; i < segments.getSegmentSize(); i++) {
            Segment s = segments.getSegment(i);
            if(pulse >= s.getStartTime() && pulse < s.getEndTime()) {
                if(i != segmentIndex) {
                    speed += initSpeed/4*random.nextGaussian();
                    speed = Math.max(speed, initSpeed * 0.5);
                    speed = Math.min(speed, initSpeed * 1.5);
                    segmentIndex = i;
                }
                return;
            }
        }
    }

    public double getWeight() {
        return weight;
    }


    public int getSegmentIndex() {
        return segmentIndex;
    }

    public void move(long microSec) {
        pulse += microSec * speed;
        // set pulse upper bound
        updateSegmentIndex();
    }

    public double getPulse() {
        return pulse;
    }

    @Override
    public Particle clone() {
        Particle p =  new Particle(pulse, initSpeed, speed, segments, segmentIndex);
        p.speed += random.nextGaussian();
        p.speed = Math.max(p.speed, initSpeed * 0.5);
        p.speed = Math.min(p.speed, initSpeed * 1.5);
        p.pulse += random.nextGaussian() * 1.2;
        p.pulse = Math.max(p.pulse, 1);
        speed += random.nextGaussian();
        speed = Math.max(speed, initSpeed * 0.5);
        speed = Math.min(speed, initSpeed * 1.5);
        pulse += random.nextGaussian() * 1.2;
        pulse = Math.max(pulse, 1);
        // set upper bound for particle pulse
        return p;
    }

//    public void setWeight(MidiSegments segments, double[] audioChromaFeature) {
//        double chromaFeatureSimilarity = getChromaFeatureSimilarity(segments, audioChromaFeature);
//        weight = Math.exp(chromaFeatureSimilarity) / Math.sqrt(2*Math.PI);
//    }

    // No need to compute for each particle!
//    private double getChromaFeatureSimilarity(MidiSegments segments, double[] audioChromaFeature) {
//        // normalize audio chroma feature
//        double[] segmentChromaFeature = segments.getSegment(segmentIndex).getChromaFeature();
//        double product = 0.0;
//        double segmentNorm = 0.0;
//        double audioNorm = 0.0;
//        for(int i = 0; i < segmentChromaFeature.length; i++) {
//            product += segmentChromaFeature[i] * audioChromaFeature[i];
//            segmentNorm += segmentChromaFeature[i] * segmentChromaFeature[i];
//            audioNorm += audioChromaFeature[i] * audioChromaFeature[i];
//        }
//        segmentNorm = Math.sqrt(segmentNorm);
//        audioNorm = Math.sqrt(audioNorm);
//        return Math.acos(product / segmentNorm / audioNorm);
//    }
}
