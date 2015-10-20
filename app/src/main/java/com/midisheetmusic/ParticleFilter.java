package com.midisheetmusic;


import java.util.HashSet;
import java.util.Set;

/**
 * Created by rpang on 10/19/15.
 */
public class ParticleFilter {
    private Set<Particle> particles;
    private MidiSegments segments;


    public ParticleFilter(MidiSegments segments, int nParticles, double initBaseSpeed) {
        this.segments = segments;
        int initPosition = segments.getStartTime();

        particles = new HashSet<Particle>();
        for(int i = 0; i < nParticles; i++) {
            particles.add(new Particle(initPosition, initBaseSpeed, 0));
        }
    }


    /* Every time an audio chroma feature is given, All particles move to their next position.
     * Return the average position after this move. */
    public int move(double[] audioChromaFeature) {
        for(Particle p: particles) {
            p.move();
            p.setSegment(segments);
        }
        setWeights(audioChromaFeature);
        resample();
        return getAvgPosition();
    }

    /* Get average position of all particles in terms of pulse. */
    private int getAvgPosition() {
        int sum = 0;
        for(Particle p: particles) {
            sum += p.getPosition();
        }
        return sum / particles.size();
    }

    private void resample() {
        Set<Particle> newParticles = new HashSet<Particle>();
        double totalWeight = 0.0;
        for(Particle p: particles) {
            totalWeight += p.getWeight();
        }

        for(int i = 0; i < particles.size(); i++) {
            double random = Math.random() * totalWeight;
            for(Particle p: particles) {
                random -= p.getWeight();
                if(random <= 0.0) {
                    if(newParticles.contains(p)) {
                        newParticles.add(p.clone());
                    } else {
                        newParticles.add(p);
                    }
                    break;
                }
            }
        }
        particles = newParticles;
    }


    private void setWeights(double[] audioChromaFeature) {
        for(Particle p: particles) {
            p.setWeight(segments, audioChromaFeature);
        }
    }




}
