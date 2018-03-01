/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.embeddediq.voctune;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.jtransforms.fft.FloatFFT_1D;

/**
 *
 * @author cottr
 */
public class analyse {
    
    // For 44,100 samples/second
    // Nyquist-Shannon max frequency is 22,050 Hz

    int sample_sz = 4096;
    // Number of bins = 4096
    // 22,050 / 4096 FFT bins = 5 Hz per bin
    // This is fine for middle A4 (440 Hz)
    // But below E3 this may not be enough resolution
    // However, this is probably OK for Alto voice

    // In time, 4096 samples equates to
    // 4096 samples / 44100 Hz = 92 ms
    // Which equates to 646 bpm
    
    // Example::
    // If song is 72 bpm (example) and 
    // smallest quantisation is 1/8 (quaver)
    
    // So long story short - we need to compromise
    // if we want to use FFT to modifify the original
    // audio in any way. It is fine for a "quick" visualistaion
    // of the original audio, but not enough resolution for
    // the actual frequency based functions.
    
    // We could double or quadruple sample rate 
    // (using linear interpolation) to provide more fidelity
    // This would give the illusion of an increased
    // range e.g. x4 -> 1.25 Hz per bin and 1/32 notes at 72 bpm
    
    // Or we could increase the sample size
    // e.g. 8192 or 16284 samples to give x4
    // e.g. 1.25 Hz per bin but 1/2 notes at 72 bpm
    
    public void start() throws UnsupportedAudioFileException, IOException
    {
        URL x = analyse.class.getResource("/audio/all/39184__jobro__piano-ff-037.wav");
        // Open audio stream
        try (AudioInputStream as = AudioSystem.getAudioInputStream(x))
        {
            try (BufferedWriter os = new BufferedWriter(new FileWriter("text.csv")))
            {
                AudioFormat af = as.getFormat();

                //int rate = (int)as.getFormat().getSampleRate(); // 44100;
                //int bytes = as.getFormat().getFrameSize();
                int step = af.getChannels() * af.getFrameSize();
                int sample_sz_bytes = step * sample_sz; // Get stereo data

                // Load the audio and FFT library
                byte [] raw_data = new byte[sample_sz_bytes];
                float [] data = new float[sample_sz];
                //FloatLargeArray data = new FloatLargeArray(duration);
                //data.setShort(as.read(bytes), 0);
                FloatFFT_1D fft = new FloatFFT_1D(sample_sz);
                
                
                // Process the audio
                long offset = 0;
                long duration = as.getFrameLength(); //  data.length();
                while (offset < duration) {
                    as.read(raw_data, 0, sample_sz_bytes);
                    ByteBuffer bb = ByteBuffer.wrap(raw_data);
                    bb = bb.order(ByteOrder.nativeOrder());
                    ShortBuffer sb = bb.asShortBuffer();
                    // IntBuffer ib = bb.asIntBuffer();
                    for (int i=0; i<sample_sz; i++)
                    {
                        data[i] = (float)sb.get(i);
                    }
                    fft.realForward(data);
                    for (float f: data)
                    {
                        os.write(String.format("%.2f, ", f));
                    }
                    os.write("\r\n");
                    offset += sample_sz;

                    // TODO - draw sample
                }
            }
        }
    }
}
