package reuiot2015.smartwatch.sensors_msband;

import com.microsoft.band.sensors.SampleRate;

import reuiot2015.smartwatch.sensors.Sensor;
import reuiot2015.smartwatch.sensors.Types;

/** An extension of the Sensor class for Microsoft Band sensors.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public abstract class MSBandSensor extends Sensor {
    private SampleRate sampleRate; // In ms until sample.
    private float sampleFrequency; // In samples per sec.

    /**
     * Constructs an instance of MSBandSensor.
     *
     * @param label     The name of the sensor.
     * @param dimensionLabels The labels for each dimension.
     * @param dimensionType The type of data stored in each dimension.
     * @param sampleRate The microsoft sampling rate.
     */
    public MSBandSensor(String label, String[] dimensionLabels, Types[] dimensionType, SampleRate sampleRate) {
        super(label, dimensionLabels, dimensionType);
        setSampleRate(sampleRate);
    }

    /** Register sensor with Microsoft Band. */
    public abstract void register();

    /** Unregister sensor from Microsoft Band. */
    public abstract void unregister();

    /** Sets the sampling rate for the sensor using the Microsoft defined SampleRate.
     *
     * @param sampleRate The appropriate SampleRate object.
     */
    public void setSampleRate(SampleRate sampleRate) {
        this.sampleRate = sampleRate;
        this.sampleFrequency = msSampleRateToFrequency(sampleRate);
    }

    /** Grabs the current SampleRate of this sensor.
     *
     * @return The SampleRate.
     */
    public SampleRate getSampleRate() {
        return this.sampleRate;
    }

    /** Grabs the current sampling frequency of the sensor.
     *
     * @return The sampling frequency in Hertz (samples per second).
     */
    public float getSampleFrequency() {
        return this.sampleFrequency;
    }

    /** Calculates the sample frequency from the Microsoft SampleRate.
     *
     * @param sampleRate A SampleRate value.
     * @return The frequency in samples/sec.
     */
    private float msSampleRateToFrequency(SampleRate sampleRate) {
        if (sampleRate == null) return -1.0f;
        switch (sampleRate) {
            case MS16: return 1000.0f / 16.0f;
            case MS32: return 1000.0f / 32.0f;
            case MS128: return 1000.0f / 128.0f;
        }
        return -1.0f;
    }
}
