# midi
android studio project with Bochen

For app users:

1. Put midi files under midi/app/src/main/assets/
2. Build app using android studio and run the app in either emulator or an android phone.
3. Choose the midi file you want to open in the first page shown as you run the app.
4. Touch "Play" button and recording will start when the device detect a loud enough sound.
5. While recording, a bar is trying to highlight the position of the palying sound. Touch anywhere to pause recording.
6. Touch "Stop" button to reset the midi file to the original status.

For app developers:

This app inherits http://midisheetmusic.sourceforge.net/index.html. Major changes on the original app include:

1. MidiSheetMusicActivity.java
    This is the entrance of the whole app. It calls ChooseSongActivity.java on load.
    
2. ChooseSongActivity.java
    This calls MidiFile.java to create an object for the chosen midi file and starts Recorder.java.
    
2. Recorder.java
    This calls MidiSegments.java to create segments for the midi file. This records sound and calls ParicleFilter.java to to do the processing.
    
3. ParticleFilter.java
    This file describes the collective properties and behavior of all particles. This file calls Particle.java. 
    
4. Paricle.java
    This file describes the properties and methods of a particle in particle filter.
    
4. MidiSegments.java
    This file does the segmentation on the given MidiFile. This calls Segment.java.

5. Segment.java
    This file describes the properties and methods for a midi segment.

