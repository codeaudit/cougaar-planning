package org.cougaar.planning.plugin.deletion;

import org.cougaar.core.plugin.deletion.*;
import org.cougaar.planning.ldm.policy.IntegerRuleParameter;
import org.cougaar.planning.ldm.policy.LongRuleParameter;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.planning.ldm.policy.PredicateRuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;
import org.cougaar.util.UnaryPredicate;

public class DeletionPolicyBase extends Policy implements DeletionPolicy {
  // Cached parameter values
  private UnaryPredicate thePredicate;
  private long theDeletionDelay;
  private int thePriority;
  public static final String PREDICATE_KEY = "predicate";
  public static final String DELETION_DELAY_KEY = "deletionDelay";
  public static final String PRIORITY_KEY = "priority";


  public DeletionPolicyBase() {
  }
  public void init(UnaryPredicate aPredicate, long deletionDelay) {
    init(null, aPredicate, deletionDelay, 0);
  }
  public void init(
    UnaryPredicate aPredicate,
    long deletionDelay,
    int priority) {
    init(null, aPredicate, deletionDelay, priority);
  }
  public void init(
    String aName,
    UnaryPredicate aPredicate,
    long deletionDelay) {
    init(aName, aPredicate, deletionDelay, 0);
  }
  /**
   * Initialize this policy.
   * @param aName a name for this policy used in printouts and for debugging
   * @param aPredicate A predicate for selecting Tasks for which this
   * policy applies
   * @param deletionDelay The age the tasks must reach before
   * being deleted
   * @param priority When multiple policies apply to a task, the
   * highest priority policy wins and the rest are ignored.
   **/
  public void init(
    String aName,
    UnaryPredicate aPredicate,
    long deletionDelay,
    int priority) {
    setName(aName);
    try {
      Add(new PredicateRuleParameter(PREDICATE_KEY, aPredicate));
      Add(
        new IntegerRuleParameter(
          PRIORITY_KEY,
          MIN_PRIORITY,
          MAX_PRIORITY,
          priority));
      Add(
        new LongRuleParameter(
          DELETION_DELAY_KEY,
          0L,
          Long.MAX_VALUE,
          deletionDelay));
    } catch (RuleParameterIllegalValueException e) {
      // No way this should happen because x <= x <= x is never false;
      e.printStackTrace();
    }
  }
  public void Add(RuleParameter param) {
    clearCache();
    super.Add(param);
  }
  public void clearCache() {
    thePredicate = null;
    theDeletionDelay = NO_DELETION_DELAY;
    thePriority = NO_PRIORITY;
  }
  public UnaryPredicate getPredicate() {
    if (thePredicate == null) {
      PredicateRuleParameter prp =
        (PredicateRuleParameter) Lookup(PREDICATE_KEY);
      thePredicate = prp.getPredicate();
    }
    return thePredicate;
  }
  public void setDeletionDelay(long deletionDelay) {
    try {
      theDeletionDelay = deletionDelay;
      Replace(
        new LongRuleParameter(
          DELETION_DELAY_KEY,
          deletionDelay,
          deletionDelay,
          deletionDelay));
    } catch (RuleParameterIllegalValueException e) {
      // No way this should happen because x <= x <= x is never false;
      e.printStackTrace();
    }
  }
  public long getDeletionDelay() {
    if (theDeletionDelay == NO_DELETION_DELAY) {
      theDeletionDelay =
        ((LongRuleParameter) Lookup(DELETION_DELAY_KEY)).longValue();
    }
    return theDeletionDelay;
  }
  public int getPriority() {
    if (thePriority == NO_PRIORITY) {
      thePriority = ((IntegerRuleParameter) Lookup(PRIORITY_KEY)).intValue();
    }
    return thePriority;
  }
}
