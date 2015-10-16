package com.midisheetmusic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rpang on 10/15/15.
 * Parse MidiFile into segments.
 */
public class MidiSegments {
    private List<Segment> segments;

    public MidiSegments(MidiFile midiFile) {
        segments = parseMidiFile(midiFile);
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void writeFile(String fileName) {

    }

    private List<Segment> parseMidiFile(MidiFile midiFile) {
        /* Get all the note numbers for each pulse. */
        Pulse[] pulseArray = new Pulse[midiFile.getTotalPulses()];
        for(MidiTrack track: midiFile.getTracks()) {
            for(MidiNote note: track.getNotes()) {
                int noteNumber = note.getNumber();
                for(int i = note.getStartTime(); i <= note.getEndTime(); i++) {
                    pulseArray[i].addNoteNumber(noteNumber);
                }
            }
        }

        /* Generate segments from pulse profiles. */
        Pulse prePulse = new Pulse();
        Pulse curPulse = null;
        Segment segment = null;
        List<Segment> segments = new ArrayList<Segment>();

        for(int i = 0; i < pulseArray.length; i++) {
            curPulse = pulseArray[i];
            if(curPulse.hasNewNoteNumber(prePulse)) {
                if(segment != null) {   // End of current segment.
                    segment.setEndTime(i-1);
                    segments.add(segment);
                }
                segment = new Segment(i);   // Create new segment.
            }
            if(segment != null) {
                segment.addNoteNumbers(curPulse.getNoteNumbers());
            }
            prePulse = curPulse;
        }
        if(segment != null) {
            segments.add(segment);
        }
        return segments;
    }

    public class Segment {
        private int startTime; // Start time of this segment in terms of pulses.
        private int endTime; // End time of this segment in terms of pulses.
        private Set<Integer> noteNumbers; // Numbers of all notes in this segment.

        public Segment(int startTime) {
            this.startTime = startTime;
            this.endTime = startTime;
            this.noteNumbers = new HashSet<Integer>();
        }

        public void addNoteNumbers(Set<Integer> noteNumbers) {
            this.noteNumbers.addAll(noteNumbers);
        }

        public int getStartTime() {
            return startTime;
        }

        public int getEndTime() {
            return endTime;
        }

        public Set<Integer> getNoteNumbers() {
            return noteNumbers;
        }

        public boolean setEndTime(int endTime) {
            if(endTime <= this.startTime) {
                return false;
            }
            this.endTime = endTime;
            return true;
        }
    }

    public class Pulse {
        private Set<Integer> noteNumbers;
        public Pulse() {
            noteNumbers = new HashSet<Integer>();
        }
        public void addNoteNumber(int noteNumber) {
            noteNumbers.add(noteNumber);
        }
        public boolean removeNoteNumber(int noteNumber) {
            return noteNumbers.remove(noteNumber);
        }
        public Set<Integer> getNoteNumbers() {
            return noteNumbers;
        }
        public boolean hasNewNoteNumber(Pulse anotherPulse) {
            return !anotherPulse.getNoteNumbers().containsAll(noteNumbers);
        }
    }


}
