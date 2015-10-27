package com.midisheetmusic;


import java.util.HashSet;
import java.util.Set;

/**
 * Created by rpang on 10/19/15.
 */
public class ParticleFilter {
    private Particle[] particles;
    private MidiSegments segments;


    public ParticleFilter(MidiSegments segments, int nParticles, double initBaseSpeed, int initPosition) {
        this.segments = segments;
        if(initPosition <= 0) initPosition = segments.getStartTime();

        particles = new Particle[nParticles];
        for(int i = 0; i < nParticles; i++) {
            particles[i] = new Particle(initPosition, initBaseSpeed, segments, -1);
        }
    }


    /* User can set the base speed for all particles at pause.*/
    public void setBaseSpeed(double baseSpeed) {
        for(Particle p: particles) {
            p.setSpeed(baseSpeed);
        }
    }

    /* Every time an audio chroma feature is given, All particles move to their next position.
     * Return the average position after this move. */
    public int move(double[] audioChromaFeature) {
        for(Particle p: particles) {
            p.move();
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


    private void setWeights(double[] audioChromaFeature) {
        for(Particle p: particles) {
            p.setWeight(segments, audioChromaFeature);
        }
    }




}
