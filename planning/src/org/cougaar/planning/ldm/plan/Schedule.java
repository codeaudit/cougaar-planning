/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.planning.ldm.plan;

import org.cougaar.util.*;
import java.util.*;

/**
 * A Schedule is an encapsulation of spatio-temporal relationships.
 * Current thought is to bind up both time and space into a single
 * object which may then be queried in various ways to test for
 * overlaps, inclusion, exclusion, etc with other schedules.
 *
 * Extends Collection to provide direct access to the collection api
 * accessors, and TimeSpan to allow comparison of whole schedules to 
 * each other.
 **/

public interface Schedule
  extends List, TimeSpan
{
  /** get a string describing what type of schedule this is (i.e.
   * what kind of scheduleelements this schedule containts).
   * Values are defined in ScheduleElementType.
   * @see org.cougaar.planning.ldm.plan.ScheduleElementType
   * @return String
   **/
  Class getScheduleElementType();
  
  /** get a description of what type of Schedule this is
   *  Values are defined in ScheduleType
   *  @see org.cougaar.planning.ldm.plan.ScheduleType
   *  @return ScheduleType
   **/
  String getScheduleType();
                
  /** get an enumeration of all of the schedule elements of this schedule.
   *  Enumeration backed by a copy of the Schedule.
   * @return Enumeration{ScheduleElement}
   */
  Enumeration getAllScheduleElements();
        
  /** Apply a Thunk to each ScheduleElement in the Schedule **/
  void applyThunkToScheduleElements(Thunk t);

  /** return subset of elements which meet the specified predicate
   * @return Collection(ScheduleElements)
   */
  Collection filter(UnaryPredicate predicate);

  /**
   * @deprecated Use getScheduleElementsWithTime(long aTime)
   */
  Collection getScheduleElementsWithDate(Date aDate);

  /** @return a sorted Collection of schedule elements that include this date.
   * Note that each schedule element will have a start date less than or equal to 
   * the specified date and an end date after the specified date.
   */
  Collection getScheduleElementsWithTime(long aTime);
        
  /** 
   * @deprecated Use getOverlappingScheduleElements(long, long)
   */
  Collection getOverlappingScheduleElements(Date startDate, Date endDate);

  /** @return a sorted Collection of schedule elements that have dates in the
   * given range of dates.  Note that these schedule elements may
   * or may not be fully bound by the date range - they may overlap.
   * Note that enddates are not included in time spans.
   */
  Collection getOverlappingScheduleElements(long startTime, long endTime);
        
  /** @deprecated use getEncapsulatedScheduleElements(long, long)
   */
  Collection getEncapsulatedScheduleElements(Date startDate, Date endDate);
  /**
   * @return a sorted collection of schedule elements that are fully bound
   * or encapsulated by a date range.
   * Note that enddates are not included in time spans.
   */
  Collection getEncapsulatedScheduleElements(long startTime, long endTime);
        
  /** @deprecated use getStartTime() **/
  Date getStartDate();

  /** @deprecated use getEndTime() **/
  Date getEndDate();

} 
