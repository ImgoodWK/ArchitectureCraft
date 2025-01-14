package gcewing.architecture;

import java.util.Collection;

import net.minecraft.util.EnumFacing;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

public class PropertyDirection extends PropertyEnum<EnumFacing> {

    protected PropertyDirection(String name, Collection<EnumFacing> values) {
        super(name, EnumFacing.class, values);
    }

    /**
     * Create a new PropertyDirection with the given name
     */
    public static PropertyDirection create(String name) {
        /**
         * Create a new PropertyDirection with all directions that match the given Predicate
         */
        return create(name, Predicates.<EnumFacing>alwaysTrue());
    }

    /**
     * Create a new PropertyDirection with all directions that match the given Predicate
     */
    public static PropertyDirection create(String name, Predicate<EnumFacing> filter) {
        /**
         * Create a new PropertyDirection for the given direction values
         */
        return create(name, Collections2.<EnumFacing>filter(Lists.newArrayList(EnumFacing.values()), filter));
    }

    /**
     * Create a new PropertyDirection for the given direction values
     */
    public static PropertyDirection create(String name, Collection<EnumFacing> values) {
        return new PropertyDirection(name, values);
    }
}
