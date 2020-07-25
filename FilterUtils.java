/**
 * Author: Gaurav Kumar
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to perform field selection (reset to null) object
 */
public class FilterUtils
{
    private static Map<String, Set<String>> classFieldsMap = new ConcurrentHashMap<>();

    public static void applyFieldSelection(Object object, Collection<String> fields,
        Set<String> defaultFields)
    {
        if (object instanceof Collection)
        {
            for (Object obj : (Collection)object)
            {
                applyFieldSelection(obj, fields, defaultFields);
            }
            return;
        }

        if (!classFieldsMap.containsKey(object.getClass().getCanonicalName()))
        {
            classFieldsMap.put(object.getClass().getCanonicalName(), getFields(object));
        }
        // filter
        Set<String> subjectedFields = classFieldsMap.get(object.getClass().getCanonicalName());

        if (CollectionUtils.isNotEmpty(fields))
        {
            subjectedFields.removeAll(fields);
        }

        if (CollectionUtils.isNotEmpty(defaultFields))
        {
            subjectedFields.removeAll(defaultFields);
        }

        // find setter for the filed and call with null;
        for(String f: subjectedFields)
        {
            try
            {
                resetField(object, f);
            }
            catch (IllegalAccessException|InvocationTargetException ignored)
            {
            }
        }
    }
    private static Set<String> getFields(final Object object)
    {
        Set<String> declaredFields = new HashSet<>();

        // store all the fields
        for (Field field : object.getClass().getDeclaredFields())
        {
            JsonProperty property = field.getAnnotation(JsonProperty.class);
            if (property != null)
            {
                String value = property.value();
                if (Strings.isNotBlank(value))
                {
                    declaredFields.add(value);
                }
            }
        }
        return declaredFields;
    }

    private static void resetField(Object object, String name) 
        throws InvocationTargetException, IllegalAccessException
    {
        String methodSign = "set" + StringUtils.capitalize(name);
        String booleanMethodSign = "is" + StringUtils.capitalize(name);
        for(Method method: object.getClass().getMethods())
        {
            if (methodSign.equals(method.getName()) || booleanMethodSign.equals(method.getName()))
            {
               method.invoke(object, new Object[] { null });
            }
        }
    }
}
