/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.embeddediq.voctune;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
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

    //int sample_sz = 4096;
    float step_sz = 2.5f; // 5 Hz
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
        // Open audio stream
        try (BufferedWriter os = new BufferedWriter(new FileWriter("text.csv")))
        {
            String[] res = new String[] {
                "/audio/all/39184__jobro__piano-ff-037.wav",
                "/audio/all/39185__jobro__piano-ff-038.wav",
                "/audio/all/39186__jobro__piano-ff-039.wav",
            };
            // AudioFormat af = getFormat();
            URL url = analyse.class.getResource(res[0]);
            AudioFormat af;
            try (AudioInputStream as = AudioSystem.getAudioInputStream(url)) {
                af = as.getFormat();
            }

            // Step size, sample_sz and byte count
            int sample_sz = (int)(af.getSampleRate() / (2.0f * step_sz));
            int step = af.getChannels() * af.getFrameSize();
            int sample_sz_bytes = step * sample_sz; // Get stereo data

            // Write CSV header
            for (int i=0; i<(sample_sz >> 1); i++)
            {
                os.write(String.format("%.1f, ", i * step_sz));
            }
            os.write("\r\n");

            // Load the audio and FFT library
            byte [] raw_data = new byte[sample_sz_bytes];
            float [] data = new float[sample_sz];
            
            for (String resStr: res)
            {
                url = analyse.class.getResource(resStr);
                try (AudioInputStream as = AudioSystem.getAudioInputStream(url))
                {
                    // Create the fourier transform
                    FloatFFT_1D fft = new FloatFFT_1D(sample_sz);                

                    // Process the audio
                    long offset = 0;
                    long duration = as.getFrameLength(); //  data.length();
                    while (offset < duration) {
                        
                        // Read a block of samples
                        as.read(raw_data, 0, sample_sz_bytes);
                        ByteBuffer bb = ByteBuffer.wrap(raw_data);
                        bb = bb.order(af.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
                        ShortBuffer sb = bb.asShortBuffer();
                        
                        // Calculate Discrete FFT
                        for (int i=0; i<sample_sz; i++)
                        {
                            data[i] = (float)sb.get(i);
                        }
                        fft.realForward(data);
                        
                        // Create a CSV output
                        for (int i=0; i<(sample_sz-1); i+=2) // float f: data)
                        {
                            os.write(String.format("%.2f, ", Math.sqrt(Math.abs(Math.pow(data[i], 2) - Math.pow(data[i+1], 2)))));
                        }
                        os.write("\r\n");
                        offset += sample_sz;

                        // TODO - draw sample
                    }
                }
            }
        }
    }
}
