/*
 * Created on Dec 12, 2015
 *
 */
package org.reactome.cytoscape.service;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * @author gwu
 *
 */
public class AnimationPlayerControl extends JPanel {
    private AnimationPlayer player;
    private JButton playBtn;
    private JButton backBtn;
    private JButton forwardBtn;
    // Used to switch
    private ImageIcon playIcon;
    private ImageIcon stopIcon;
    // track state
    private boolean isPlaying;
    // Animation time
    private int interval = 1000; // 1 second as default
    
    /**
     * Default constructor.
     */
    public AnimationPlayerControl() {
        init();
    }
    
    private void init() {
        setLayout(new GridLayout(1, 3));
        playBtn = new JButton();
        playIcon = PlugInObjectManager.getManager().createImageIcon("Play16.gif");
        stopIcon = PlugInObjectManager.getManager().createImageIcon("Stop16.gif");
        playBtn.setIcon(playIcon);
        forwardBtn = new JButton();
        forwardBtn.setIcon(PlugInObjectManager.getManager().createImageIcon("StepForward16.gif"));
        backBtn = new JButton();
        backBtn.setIcon(PlugInObjectManager.getManager().createImageIcon("StepBack16.gif"));
        add(backBtn);
        add(playBtn);
        add(forwardBtn);
        installListeners();
    }
    
    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    private void installListeners() {
        playBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                handlePlayBtn();
            }
        });
        backBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                backward();
            }
        });
        forwardBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                forward();
            }
        });
    }
    
    private void backward() {
        isPlaying = false;
        if (player != null)
            player.backward();
    }
    
    private void forward() {
        isPlaying = false;
        if (player != null)
            player.forward();
    }
    
    private void play() {
        if (player == null)
            return;
        Thread t = new Thread() {
            public void run() {
                while (isPlaying) {
                    player.forward();
                    try {
                        Thread.sleep(interval); 
                    }
                    catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
    }

    public AnimationPlayer getPlayer() {
        return player;
    }

    public void setPlayer(AnimationPlayer player) {
        this.player = player;
    }

    private void handlePlayBtn() {
        if (isPlaying) {
            isPlaying = false;
            playBtn.setIcon(playIcon);
        }
        else {
            isPlaying = true;
            play();
            playBtn.setIcon(stopIcon);
        }
    }
    
}
