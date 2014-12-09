package org.nd4j.linalg.learning;


import static org.nd4j.linalg.ops.transforms.Transforms.*;
import static org.nd4j.linalg.ops.transforms.Transforms.pow;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.Serializable;




/**
 *
 * Vectorized Learning Rate used per Connection Weight
 *
 * Adapted from: http://xcorr.net/2014/01/23/adagrad-eliminating-learning-rates-in-stochastic-gradient-descent/
 *
 * @author Adam Gibson
 *
 */
public class AdaGrad implements Serializable {

    /**
     *
     */
    protected static final long serialVersionUID = -4754127927704099888L;
    protected double masterStepSize = 1e-1; // default for masterStepSize (this is the numerator)
    //protected double squaredGradientSum = 0;
    public INDArray historicalGradient;
    public INDArray adjustedGradient;
    public double fudgeFactor = 1e-6;
    public int[] shape;
    protected int numIterations = 0;
    protected double lrDecay = 0.95;
    protected boolean decayLr;
    protected double minLearningRate = 1e-4;
    protected double numericalGrad = 1.0;
    protected double adjustedNumericalGrad = 1.0;

    public AdaGrad( int rows, int cols, double gamma) {
        this.shape = new int[]{rows,cols};
        this.masterStepSize = gamma;
        this.decayLr = false;


    }


    /**
     * Create adagrad with the specified shape
     * @param shape
     */
    public AdaGrad(int[] shape) {
        this.shape = shape;
        this.masterStepSize = 1e-1;
        this.decayLr = false;


    }

    /**
     * Initializes adagrad with a gamma of 1e-2
     * @param rows the rows for the gradients
     * @param cols the number of columns for the gradient
     */
    public AdaGrad( int rows, int cols) {
        this(rows,cols,0.1);

    }




    /**
     * Gets feature specific learning rates
     * Adagrad keeps a history of gradients being passed in.
     * Note that each gradient passed in becomes adapted over time, hence
     * the name adagrad
     * @param gradient the gradient to get learning rates for
     * @param column the slice of the gradient history to use
     * @param shape  the shape of the nd array for the historical gradient
     * @return the feature specific learning rates
     */
    public synchronized double getGradient(double gradient, int column, int[] shape) {
        boolean historicalInitialized = false;
        if(this.historicalGradient == null) {
            this.historicalGradient = Nd4j.ones(shape);
            historicalInitialized = true;
        }

        double sqrtHistory = !historicalInitialized ? Math.sqrt(historicalGradient.getDouble(column)) : historicalGradient.getDouble(column);
        double learningRates = (masterStepSize) / sqrtHistory;
        double adjustedGradient = gradient * (learningRates);

        historicalGradient.putScalar(column,historicalGradient.getDouble(column) + Math.pow(gradient,2));
        numIterations++;

        //ensure no zeros
        return adjustedGradient;
    }

    /**
     * Gets feature specific learning rates
     * Adagrad keeps a history of gradients being passed in.
     * Note that each gradient passed in becomes adapted over time, hence
     * the name adagrad
     * @param gradient the gradient to get learning rates for
     * @param slice the slice of the gradient history to use
     * @param shape  the shape of the nd array for the historical gradient
     * @return the feature specific learning rates
     */
    public synchronized INDArray getGradient(INDArray gradient, int slice, int[] shape) {
        boolean historicalInitialized = false;
        if(this.historicalGradient == null || this.historicalGradient.length() != gradient.length()) {
            this.historicalGradient = Nd4j.ones(shape);
            historicalInitialized = true;
        }

        INDArray sqrtHistory = !historicalInitialized ? sqrt(historicalGradient.slice(slice)) : historicalGradient;
        INDArray learningRates = sqrtHistory.rdivi(masterStepSize);
        this.adjustedGradient = gradient.mul(learningRates);

        this.historicalGradient.addi(pow(gradient,2));
        numIterations++;

        //ensure no zeros
        return adjustedGradient;
    }


    /**
     * Gets feature specific learning rates
     * Adagrad keeps a history of gradients being passed in.
     * Note that each gradient passed in becomes adapted over time, hence
     * the name adagrad
     * @param gradient the gradient to get learning rates for
     * @return the feature specific learning rates
     */
    public synchronized INDArray getGradient(INDArray gradient) {
        boolean historicalInitialized = false;
        if(this.historicalGradient == null || this.historicalGradient.length() != gradient.length()) {
            this.historicalGradient = Nd4j.ones(gradient.rows(), gradient.columns());
            historicalInitialized = true;
        }

        INDArray sqrtHistory = !historicalInitialized ? sqrt(historicalGradient) : historicalGradient;
        INDArray learningRates = sqrtHistory.rdivi(masterStepSize);
        this.adjustedGradient = gradient.mul(learningRates);

        this.historicalGradient.addi(pow(gradient,2));
        numIterations++;

        //ensure no zeros
        return adjustedGradient;
    }

    public  double getMasterStepSize() {
        return masterStepSize;
    }

    public  void setMasterStepSize(double masterStepSize) {
        this.masterStepSize = masterStepSize;
    }

    public synchronized boolean isDecayLr() {
        return decayLr;
    }

    public synchronized void setDecayLr(boolean decayLr) {
        this.decayLr = decayLr;
    }




}