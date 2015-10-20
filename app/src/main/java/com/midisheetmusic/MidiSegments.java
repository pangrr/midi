package com.midisheetmusic;

import java.util.ArrayList;
import java.util.List;

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
        FileWriter.writeFile(fileName, toString());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for(Segment segment: segments) {
            sb.append(Integer.toString(segment.getStartTime()));
            sb.append(",");
            sb.append(Integer.toString(segment.getEndTime()));
            for(int noteNumber: segment.getNoteNumbers()) {
                sb.append(",");
                sb.append(noteNumber);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public Segment getSegment(int index) {
        return segments.get(index);
    }

    public int getSegmentSize() {
        return segments.size();
    }

    /* Get the start time in pulses of the first segment. */
    public int getStartTime() {
        if(segments == null || segments.isEmpty()) {
            return -1;
        } else {
            return segments.get(0).getStartTime();
        }
    }

    private List<Segment> parseMidiFile(MidiFile midiFile) {
        /* Get all the note for each pulse. */
        Pulse[] pulseArray = new Pulse[midiFile.getTotalPulses()+1];
        for(int i = 0; i < pulseArray.length; i++) {
            pulseArray[i] = new Pulse();
        }
        for(MidiTrack track: midiFile.getTracks()) {
            for(MidiNote note: track.getNotes()) {
                for(int i = note.getStartTime(); i < note.getEndTime(); i++) {
                    pulseArray[i].addNote(note);
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
            if(curPulse.hasNewNote(prePulse)) {   // End of current segment.
                if(segment != null) {
                    segment.complete(i-1);
                    segments.add(segment);
                }
                segment = new Segment(i);   // Create new segment.
            }
            if(segment != null) {
                segment.addNotes(curPulse.getNotes());
            }
            prePulse = curPulse;
        }
        if(segment != null) {
            segment.complete(pulseArray.length - 1);
            segments.add(segment);
        }
        return segments;
    }
}
