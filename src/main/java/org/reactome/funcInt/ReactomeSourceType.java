/*
 * Created on Sep 28, 2006
 *
 */
package org.reactome.funcInt;

/**
 * The types that indicate what classes are used to extract functional interctions. There
 * are only three types available: COMPLEX, REACTION and INTERACTION (classes defined in the
 * Reactome model), and TARGETED_INTERACTION
 * @author guanming
 *
 */
public enum ReactomeSourceType {
   COMPLEX,
   REACTION,
   INTERACTION,
   TARGETED_INTERACTION
}
