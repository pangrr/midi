package com.midisheetmusic;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rpang on 10/19/15.
 */
public class Pulse {
    private Set<MidiNote> notes;

    public Pulse() {
        notes = new HashSet<MidiNote>();
    }

    public void addNote(MidiNote note) {
        notes.add(note);
    }

    public Set<MidiNote> getNotes() {
        return notes;
    }

    public boolean hasNewNote(Pulse anotherPulse) {
        return !anotherPulse.getNotes().containsAll(notes);
    }
}
