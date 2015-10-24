package com.midisheetmusic;


import java.util.Random;

/**
 * Created by rpang on 10/18/15.
 * Representing a particle in particle filtering.
 */
public class Particle {
    private double position; // Position of this particle in the MidiFile in terms of pulse.
    private double speed;
    private Random random;
    private int segment;    // Position in terms of segment.
    private double weight;

    public Particle(double position, double baseSpeed, int segment) {
        random = new Random();
        this.position = position;
        this.speed = baseSpeed + random.nextGaussian();
        this.segment = segment;
        this.weight = 0.0;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setWeight(MidiSegments segments, double[] audioChromaFeature) {
        double chromaFeatureSimilarity = getChromaFeatureSimilarity(segments, audioChromaFeature);
        weight = Math.exp(chromaFeatureSimilarity) / Math.sqrt(2*Math.PI);
    }


    public void setSegment(MidiSegments segments) {
        for(int i = segment; i < segments.getSegmentSize(); i++) {
            Segment s = segments.getSegment(i);
            if(position >= s.getStartTime() && position < s.getEndTime()) {
                segment = i;
                return;
            }
        }
    }

    public double getWeight() {
        return weight;
    }


    public int getSegment() {
        return segment;
    }

    public void move() {
        position += speed;
        speed = Math.max(speed + random.nextGaussian() * 10, 0);

    }

    public double getPosition() {
        return position;
    }

    @Override
    public Particle clone() {
        return new Particle(position, speed, segment);
    }

    private double getChromaFeatureSimilarity(MidiSegments segments, double[] audioChromaFeature) {
        double[] segmentChromaFeature = segments.getSegment(segment).getChromaFeature();
        double product = 0.0;
        double segmentNorm = 0.0;
        double audioNorm = 0.0;
        for(int i = 0; i < segmentChromaFeature.length; i++) {
            product += segmentChromaFeature[i] * audioChromaFeature[i];
            segmentNorm += segmentChromaFeature[i] * segmentChromaFeature[i];
            audioNorm += audioChromaFeature[i] * audioChromaFeature[i];
        }
        segmentNorm = Math.sqrt(segmentNorm);
        audioNorm = Math.sqrt(audioNorm);
        return Math.acos(product / segmentNorm / audioNorm);
    }
}
