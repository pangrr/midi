package com.midisheetmusic;


import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by rpang on 10/19/15.
 */
public class ParticleFilter {
    private Particle[] particles;
    private MidiSegments segments;
    private Set<Integer> particleSegmentIndex;
    public TreeMap<Integer, Double> segmentWeight;


    public ParticleFilter(MidiSegments segments, int nParticles, double baseSpeed, int initPosition) {
        this.segments = segments;
        if(initPosition <= 0) initPosition = segments.getStartTime();

        particles = new Particle[nParticles];
        double tmp = (baseSpeed*2-baseSpeed/2)/nParticles;
        for(int i = 0; i < nParticles; i++) {
            particles[i] = new Particle(initPosition, baseSpeed, baseSpeed/2 + tmp*i, segments, -1);
        }
    }

    public Particle[] getParticles() {
        return particles;
    }


    /* Every time an audio chroma feature is given, All particles move to their next position.
     * Return the average position after this move. */
    public int move(double[] audioChromaFeature, long microSecPassedSinceLastRead) {
        for(Particle p: particles) {
            p.move(microSecPassedSinceLastRead);
        }
//        Map<Integer, Double> weights = computeWeights(audioChromaFeature);
//        setWeights(weights);
        segmentWeight = computeWeights(audioChromaFeature);
        setWeights(segmentWeight);
        resample();
        return getAvgPulse();
    }


    public Set<Integer> getParticleSegmentIndex() {
        return particleSegmentIndex;
    }

    /* Get average position of all particles in terms of pulse. */
    private int getAvgPulse() {
        double sum = 0.0;
        for(Particle p: particles) {
            // print particles' segments
            sum += p.getPulse();
        }
        return (int) sum / particles.length;
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


    private void setWeights(TreeMap<Integer, Double> weights) {
        for(Particle p: particles) {
            p.setWeight(weights);
        }
    }

    // Return <segmentIndex, weight>
    private TreeMap<Integer, Double> computeWeights(double[] audioChromaFeature) {
        collectParticleSegmentIndex();
        TreeMap<Integer, Double> weights = new TreeMap<Integer, Double>();
        for(int i: particleSegmentIndex) {
            weights.put(i, computeWeight(i, audioChromaFeature));
        }
        // normalize
//        double sum = 0;
//        for(int i: particleSegmentIndex) {
//            sum += weights.get(i);
//        }
//        double multiplier = 1/ sum;
//        for(int i: particleSegmentIndex) {
//            weights.put(i, weights.get(i) * multiplier);
//        }

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
        double alpha = Math.acos(product / segmentNorm / audioNorm);
        return Math.exp(-alpha*alpha);
    }


}
