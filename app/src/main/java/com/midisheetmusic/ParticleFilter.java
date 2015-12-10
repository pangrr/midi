package com.midisheetmusic;


import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Set;

/**
 * Created by rpang on 10/19/15.
 */
public class ParticleFilter {
    private Particle[] particles;
    private MidiSegments segments;
    private Set<Integer> particleSegmentIndex;


    public ParticleFilter(MidiSegments segments, int nParticles, double initSpeed, int initPosition) {
        this.segments = segments;
        if(initPosition <= 0) initPosition = segments.getStartTime();

        particles = new Particle[nParticles];
        double tmp = (initSpeed*2-initSpeed/2)/nParticles;
        for(int i = 0; i < nParticles; i++) {
            particles[i] = new Particle(initPosition, initSpeed, initSpeed/2 + tmp*i, segments, -1);
        }
    }

    public Particle[] getParticles() {
        return particles;
    }


    /* Every time an audio chroma feature is given, All particles move to their next position.
     * Return the average position after this move. */
    public int move(double[] audioChromaFeature) {
        for(Particle p: particles) {
            p.move();
        }
        Map<Integer, Double> weights = computeWeights(audioChromaFeature);
        setWeights(weights);
        resample();
        return getAvgPulse();
    }

    public Set<Integer> getParticleSegmentIndex() {
        return particleSegmentIndex;
    }

    /* Get average position of all particles in terms of pulse. */
    private int getAvgPulse() {
        int sum = 0;
        for(Particle p: particles) {
            // print particles' segments
            sum += p.getPulse();
        }
        return sum / particles.length;
    }



    private void resample() {
        Particle[] newParticles = new Particle[particles.length];
        double totalWeight = 0.0;
        for(Particle p: particles) {
            totalWeight += p.getWeight();
        }

        for(int i = 0; i < particles.length; i++) {
            double random = Math.random() * totalWeight;
            for(Particle p: particles) {
                random -= p.getWeight();
                if(random <= 0.0) {
                    newParticles[i] = p.clone();
                    break;
                }
            }
        }
        particles = newParticles;
    }


    private void setWeights(Map<Integer, Double> weights) {
        for(Particle p: particles) {
            p.setWeight(weights);
        }
    }

    // Return <segmentIndex, weight>
    private Map<Integer, Double> computeWeights(double[] audioChromaFeature) {
        collectParticleSegmentIndex();
        Map<Integer, Double> weights = new HashMap<Integer, Double>();
        for(int i: particleSegmentIndex) {
            weights.put(i, computeWeight(i, audioChromaFeature));
        }
        return weights;
    }

    // Get segment index for all particles
    private void collectParticleSegmentIndex() {
        particleSegmentIndex = new HashSet<>();
        for(Particle p: particles) {
            particleSegmentIndex.add(p.getSegmentIndex());
        }
    }

    // Compute weight for given segment
    private double computeWeight(int segmentIndex, double[] audioChromaFeature) {
        double[] segmentChromaFeature = segments.getSegment(segmentIndex).getChromaFeature();
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
