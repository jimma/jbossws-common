/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ws.common.utils;

import java.lang.reflect.Array;

/**
 * Collected methods which allow easy implementation of <code>hashCode</code>.
 *
 * Example use case:
 * <pre>
 *  public int hashCode(){
 *    int result = HashCodeUtil.SEED;
 *    //collect the contributions of various fields
 *    result = HashCodeUtil.hash(result, fPrimitive);
 *    result = HashCodeUtil.hash(result, fObject);
 *    result = HashCodeUtil.hash(result, fArray);
 *    return result;
 *  }
 * </pre>
 */
public final class HashCodeUtil
{

   /**
    * An initial value for a <code>hashCode</code>, to which is added contributions
    * from fields. Using a non-zero value decreases collisons of <code>hashCode</code>
    * values.
    */
   public static final int SEED = 23;

   /**
    * booleans.
    */
   public static int hash(int aSeed, boolean aBoolean)
   {
      return org.jboss.ws.common.utils.HashCodeUtil.firstTerm(aSeed) + (aBoolean ? 1 : 0);
   }

   /**
    * chars.
    */
   public static int hash(int aSeed, char aChar)
   {
      return org.jboss.ws.common.utils.HashCodeUtil.firstTerm(aSeed) + (int)aChar;
   }

   /**
    * ints.
    */
   public static int hash(int aSeed, int aInt)
   {
      /*
       * Implementation Note
       * Note that byte and short are handled by this method, through
       * implicit conversion.
       */
      return org.jboss.ws.common.utils.HashCodeUtil.firstTerm(aSeed) + aInt;
   }

   /**
    * longs.
    */
   public static int hash(int aSeed, long aLong)
   {
      return org.jboss.ws.common.utils.HashCodeUtil.firstTerm(aSeed) + (int)(aLong ^ (aLong >>> 32));
   }

   /**
    * floats.
    */
   public static int hash(int aSeed, float aFloat)
   {
      return org.jboss.ws.common.utils.HashCodeUtil.hash(aSeed, Float.floatToIntBits(aFloat));
   }

   /**
    * doubles.
    */
   public static int hash(int aSeed, double aDouble)
   {
      return org.jboss.ws.common.utils.HashCodeUtil.hash(aSeed, Double.doubleToLongBits(aDouble));
   }

   /**
    * <code>aObject</code> is a possibly-null object field, and possibly an array.
    *
    * If <code>aObject</code> is an array, then each element may be a primitive
    * or a possibly-null object.
    */
   public static int hash(int aSeed, Object aObject)
   {
      int result = aSeed;
      if (aObject == null)
      {
         result = org.jboss.ws.common.utils.HashCodeUtil.hash(result, 0);
      }
      else if (!org.jboss.ws.common.utils.HashCodeUtil.isArray(aObject))
      {
         result = org.jboss.ws.common.utils.HashCodeUtil.hash(result, aObject.hashCode());
      }
      else
      {
         int length = Array.getLength(aObject);
         for (int idx = 0; idx < length; ++idx)
         {
            Object item = Array.get(aObject, idx);
            //recursive call!
            result = org.jboss.ws.common.utils.HashCodeUtil.hash(result, item);
         }
      }
      return result;
   }

   /// PRIVATE ///
   private static final int fODD_PRIME_NUMBER = 37;

   private static int firstTerm(int aSeed)
   {
      return org.jboss.ws.common.utils.HashCodeUtil.fODD_PRIME_NUMBER * aSeed;
   }

   private static boolean isArray(Object aObject)
   {
      return aObject.getClass().isArray();
   }
}
