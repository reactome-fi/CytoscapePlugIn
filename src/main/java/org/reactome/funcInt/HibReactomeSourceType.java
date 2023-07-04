/*
 * Created on Sep 28, 2006
 *
 */
package org.reactome.funcInt;

public class HibReactomeSourceType extends IntEnumUserType<ReactomeSourceType> {
    
    public HibReactomeSourceType() {
        super(ReactomeSourceType.class,
              ReactomeSourceType.values());
    }
    
}
