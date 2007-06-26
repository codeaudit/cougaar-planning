package org.cougaar.planning.ldm.measure;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/**
 * Generic derivative - represents a fraction of any measure over any other, e.g. Volume/Duration 
 * would be equivalent to the current FlowRate class.  Could also be other derivates like Volume/Area, etc.
 *
 * @see GenericRate for the Measure/Duration shorthand that comes up a lot
 */
public class GenericDerivative<N extends Measure, D extends Measure> implements Serializable, Derivative {
  private double theValue;
  private N numerator;
  private D denominator;
  private static final NumberFormat format = new DecimalFormat("#.###");

  /** No-arg constructor is only for use by serialization **/
  public GenericDerivative() {}

  /** @param num An instance of numerator
   *  @param den An instance of denominator
   **/
  public GenericDerivative(N num, D den) {
    double numInCommonUnit = num.getValue(num.getCommonUnit());
    double denInCommonUnit = den.getValue(den.getCommonUnit());
    theValue = numInCommonUnit / denInCommonUnit;

    //System.err.println("num " + numInCommonUnit + " denom " + denInCommonUnit + " value " + theValue);
    numerator = num;
    denominator = den;
  }


  public int getCommonUnit() {
    return 0;
  }

  /**
   * TODO : Not sure what to do here.
   * @paramx denom
   * @return
   */
/*  public <D extends Measure> GenericDerivative newGenericDerivative(D denom) {
    //return new GenericDerivative<<GenericDerivative<N,D>>,D>(this, denom);
    return null;
  }*/

  public int getMaxUnit() { return numerator.getMaxUnit()* denominator.getMaxUnit(); }

  /** @param unit One of the constant units of  **/
  public final String getUnitName(int unit) {
    return numerator.getUnitName(unit/denominator.getMaxUnit())+"/"+denominator.getUnitName(unit%denominator.getMaxUnit());
  }

  /** @paramx num An instance of num to use as numerator
   *  @paramx den An instance of denom use as denominator
   * @return generic rate
   **/
/*
  public static final newGenericDerivative newGenericRate(Measure num, Measure den) {
    return new newGenericDerivative(num.getValue(0)/den.getValue(0));
  }
*/

  // simple math : addition and subtraction

  public Measure add(Measure toAdd) {
    return add((GenericDerivative<N, D>)toAdd);
  }

  public Measure subtract(Measure toSubtract) {
    return subtract((GenericDerivative<N, D>)toSubtract);
  }

  public GenericDerivative<N, D> add(GenericDerivative<N, D> toAdd) {
    D commonDenom = (D) denominator.scale(toAdd.denominator.getNativeValue());
    double otherNumerUnitless = toAdd.numerator.getNativeValue();
    double otherDenomUnitless = toAdd.denominator.getNativeValue();
    double leftNumerator = numerator.getNativeValue() * otherDenomUnitless;
    double rightNumerator = otherNumerUnitless * denominator.getNativeValue();

    N newNumer = (N) numerator.valueOf(leftNumerator + rightNumerator);

    return newInstance(newNumer, commonDenom);
  }

  public GenericDerivative<N, D> subtract(GenericDerivative<N, D> toSubtract) {
    D commonDenom = (D) denominator.scale(toSubtract.denominator.getNativeValue());
    double otherNumerUnitless = toSubtract.numerator.getNativeValue();
    double otherDenomUnitless = toSubtract.denominator.getNativeValue();
    double leftNumerator = numerator.getNativeValue() * otherDenomUnitless;
    double rightNumerator = otherNumerUnitless * denominator.getNativeValue();

    N newNumer = (N) numerator.valueOf(leftNumerator - rightNumerator);

    return newInstance(newNumer, commonDenom);
  }

  public N multiply(D other) {
    return (N) numerator.scale(other.getNativeValue()/denominator.getNativeValue());
  }

  /** @param unit1 One of the constant units of Numer
   *  @param unit2 One of the constant units of Denom
   **/
  public double getValue(int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= numerator.getMaxUnit() &&
      unit2 >= 0 && unit2 <= denominator.getMaxUnit())
      return (numerator.getValue(unit1)/denominator.getValue(unit2));
    else
      throw new UnknownUnitException();
  }


  public double getValue(int unit) {
    if (denominator == null) {
      return 0;
    }
    return getValue(unit/denominator.getMaxUnit(), unit % denominator.getMaxUnit());
  }

  public boolean equals(Object o) {
    return ( o instanceof GenericDerivative &&
      theValue == ((GenericDerivative) o).theValue);
  }

  public String toString() {
    if (numerator == null) {
      return "numerator null";
    }
    if (denominator == null) {
      return "denominator null";
    }
    String numeratorUnit   = numerator.getUnitName(numerator.getCommonUnit());
    String denominatorUnit = denominator.getUnitName(denominator.getCommonUnit());
    String formatted;
    synchronized(format) {
      formatted = format.format(getValue(getCommonUnit()));
    }
    return formatted + " " + numeratorUnit + "/" + denominatorUnit;
  }

  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // Derivative
  public final Class getNumeratorClass() { return numerator.getClass(); }
  public final Class getDenominatorClass() { return denominator.getClass(); }

  public Measure computeNumerator(Measure denominator) {
    return multiply((D)denominator);
  }

  public Measure getCanonicalDenominator() {
    return denominator.valueOf(0);
  }

  public Measure getCanonicalNumerator() {
    return numerator.valueOf(0);
  }

  public Measure negate() {
    return scale(-1);
  }

  public Measure scale(double scale) {
    N newNumer = (N) numerator.scale(scale);
    return newInstance(newNumer, denominator);
  }

  /**
   * Does floor of numerator and denominator.
   *
   * May not be what you want
   * @return floor value
   */
  public Measure floor(int unit) {
    return floor(unit%numerator.getMaxUnit(),unit/denominator.getMaxUnit());
  }

  public Measure floor(int numeratorUnit, int denominatorUnit) {
    return newInstance((N)numerator.floor(numeratorUnit), (D)denominator.floor(denominatorUnit));
  }

  /**
   * Only way to make an instance of the object
   *
   * @param numerator to use
   * @param denominator to use
   * @return new instance of this derivative or subclass
   */
  protected GenericDerivative<N, D> newInstance(N numerator, D denominator) {
    return new GenericDerivative<N, D>(numerator, denominator);
  }

  /**
   * This is a rate in the common unit of the numerator, by the denominator.
   *
   * So for instance if this rate is 10 gallons/hr, valueOf (5) would be 5 liters/hour,
   * since liters is Volume's common unit.
   * @param value to use for numerator
   * @return GenericDerivative instance or subclass
   */
  public Measure valueOf(double value) {
    return newInstance((N)numerator.valueOf(value),denominator);
  }

  /**
   * This is a rate in the specified unit of the numerator, by the denominator.
   *
   * So for instance if this rate is 10 gallons/hr, valueOf (5, Volume.GALLONS)
   * would be 5 gallons/hour.
   *
   * @param value to use for numerator
   * @return GenericDerivative instance or subclass
   */
  public Measure valueOf(double value, int unit) {
    return newInstance((N)numerator.valueOf(value, unit),denominator);
  }

  public int getNativeUnit() {
    return 0; 
  }

  public double getNativeValue() {
    return theValue;
  }

  // serialization
/*  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(theValue);
    out.write
  }
  public void readExternal(ObjectInput in) throws IOException {
    theValue = in.readDouble();
  }*/
}
