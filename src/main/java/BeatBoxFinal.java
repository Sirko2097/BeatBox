import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

/**
 * The final version of BeatBox.
 * This class draw a panel and establish GUI for comfortable using.
 * */
public class BeatBoxFinal {
    JFrame theFrame;
    JPanel mainPanel;
    JList incomingList;
    JTextField userMessage;
    ArrayList<JCheckBox> checkBoxList;
    int nextNum;
    Vector<String> listVector = new Vector<String>();
    String userName;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

    Sequencer sequencer;
    Sequence sequence;
    Sequence mySequence = null;
    Track track;

    String[] instrumentNames = {
            "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
            "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap",
            "Low-mid Tom", "High Agogo", "Open Hi Conga"
    };

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        new BeatBoxFinal().startUp("Serhii");
    }

    /**
     * This method establish connection to th local server with user's name.
     * And then, call other method for correct working.
     * @param name of user.
     * */
    private void startUp(String name) {
        userName = name;

        try {
            Socket socket = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch (Exception ex) {
            System.out.println("Couldn't connect - you'll have to play alone.");
        }
        setUpMidi();
        buildGUI();
    } //close startUp

    /**
     * This method builds a GUI for comfortable using.
     * GUI includes all buttons and check boxes for making midi-music
     * and control panel with appropriate buttons like "Stop", "Start" ect.
     * */
    private void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        checkBoxList = new ArrayList<JCheckBox>();

        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton sendIt = new JButton("sendIt");
        sendIt.addActionListener(new MySendListener());
        buttonBox.add(sendIt);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);
        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }

        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    } //close buildGUI

    /**
     * This method includes MidiSystem to main program
     * for creating and playing melodies, which users create or send to each other.
     * */
    private void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    } //close setIpMidi

    /**
     * This method creates and plays tracks.
     * */
    private void buildTrackAndStart() {
        ArrayList<Integer> trackList = null;
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<Integer>();

            for (int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) checkBoxList.get(j + (16 * i));
                if (jc.isSelected()){
                    int key = instruments[i];
                    trackList.add(key);
                } else {
                    trackList.add(null);
                }
            }//close nested loop
            makeTracks(trackList);
        } //close loop
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    } //close buildTrackAndStart

    /**
     * Controller for button "Start"
     * */
    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            buildTrackAndStart();
        }
    }

    /**
     * Controller for button "Stop"
     * */
    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            sequencer.stop();
        }
    }


    /**
     * Controller for button "Up Tempo"
     * */
    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 1.03));
        }
    }

    /**
     * Controller for button "Down Tempo"
     * */
    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * .97));
        }
    }

    /**
     * Controller for button "Send", which sends messages.
     * */
    public class MySendListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            boolean[] checkboxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox)checkBoxList.get(i);
                if (check.isSelected()) {
                    checkboxState[i] = true;
                }
            }
            String messageToSend = null;
            try {
                out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
                out.writeObject(checkboxState);
            } catch (Exception ex) {
                System.out.println("Sorry, dude. Couldn't send it to the server.");
            }
            userMessage.setText("");
        }
    }

    public class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                String selected = (String)incomingList.getSelectedValue();
                if (selected != null) {
                    boolean[] selectedState = (boolean[])otherSeqsMap.get(selected);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }

    /**
     * This thread is extra connection with server.
     * It receives all messages from server to your application.
     * Thread will be killed only when application will be closed.
     * */
    public class RemoteReader implements Runnable {
        boolean[] checkboxState = null;
        String nameToShow = null;
        Object obj = null;

        public void run() {
            try {
                while ((obj = in.readObject()) != null) {
                    System.out.println("got an object from server");
                    System.out.println(obj.getClass());
                    String nameToShow = (String)obj;
                    checkboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow, checkboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class MyPlayMineListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (mySequence != null) {
                sequence = mySequence;
            }
        }
    }

    public void changeSequence(boolean[] checkboxState) {
        for (int i = 0; i < 256; i++) {
            JCheckBox check = (JCheckBox)checkBoxList.get(i);
            if (checkboxState[i]) {
                check.setSelected(true);
            } else {
                check.setSelected(false);
            }
        }
    }

    private void makeTracks(ArrayList list) {
        Iterator it = list.iterator();
        for (int i = 0; i < 16; i++) {
            Integer num = (Integer)it.next();
            if (num != null) {
                int numKey = num;
                track.add(makeEvent(144, 9, numKey, 100, i));
                track.add(makeEvent(128, 9, numKey, 100, i + 1));
            }
        }
    }

    private MidiEvent makeEvent(int comd, int chan, int one, int two, int trick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, trick);
        } catch (Exception ignored) {}
        return event;
    }
}
