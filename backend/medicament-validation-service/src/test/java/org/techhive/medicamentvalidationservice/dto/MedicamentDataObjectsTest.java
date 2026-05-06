package org.techhive.medicamentvalidationservice.dto;

import org.junit.jupiter.api.Test;
import org.techhive.medicamentvalidationservice.entity.ValidMedicament;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicamentDataObjectsTest {

    @Test
    void dataObjects_shouldHonorAccessorsAndEqualityContracts() {
        List<Class<?>> classes = List.of(
                BatchValidationResultDTO.class,
                ValidationResultDTO.class,
                ValidMedicament.class,
                OpenFdaDrugResponse.class,
                OpenFdaDrugResponse.Meta.class,
                OpenFdaDrugResponse.Meta.ResultsMeta.class,
                OpenFdaDrugResponse.DrugResult.class,
                OpenFdaDrugResponse.Product.class,
                OpenFdaDrugResponse.ActiveIngredient.class,
                OpenFdaDrugResponse.OpenFdaInfo.class
        );

        for (Class<?> type : classes) {
            assertDataObjectContract(type);
        }
    }

    @Test
    void validMedicamentPrePersist_shouldSetLoadedAtWhenEntityIsCreated() throws Exception {
        ValidMedicament medicament = new ValidMedicament();
        medicament.setLoadedAt(null);

        Method onCreate = ValidMedicament.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(medicament);

        assertNotNull(medicament.getLoadedAt());
    }

    @Test
    void validationResultConstructor_shouldKeepSuggestionsInitialized() {
        ValidationResultDTO result = new ValidationResultDTO("Aspirin", true, "Valid");

        assertEquals("Aspirin", result.getDrugName());
        assertTrue(result.isValid());
        assertEquals("Valid", result.getMessage());
        assertNotNull(result.getSuggestions());
        assertTrue(result.getSuggestions().isEmpty());
    }

    private void assertDataObjectContract(Class<?> type) {
        Object populated = newInstance(type);
        Object sameValues = newInstance(type);
        List<Field> fields = fieldsOf(type);

        for (Field field : fields) {
            Object value = sampleValue(field.getType(), 0);
            setProperty(populated, field, value);
            setProperty(sameValues, field, value);
            assertEquals(value, getProperty(populated, field), type.getSimpleName() + "." + field.getName());
        }

        assertEquals(populated, populated);
        assertNotEquals(populated, null);
        assertNotEquals(populated, "different type");
        assertEquals(populated, sameValues, type.getSimpleName() + " should compare equal for same values");
        assertEquals(populated.hashCode(), sameValues.hashCode());
        assertTrue(populated.toString().contains(type.getSimpleName()));

        Object defaults = newInstance(type);
        Object sameDefaults = newInstance(type);
        assertEquals(defaults, sameDefaults);

        for (Field field : fields) {
            Object changed = newInstance(type);
            for (Field copied : fields) {
                setProperty(changed, copied, getProperty(populated, copied));
            }
            setProperty(changed, field, sampleValue(field.getType(), 1));
            assertNotEquals(populated, changed, type.getSimpleName() + " should include " + field.getName());

            if (!field.getType().isPrimitive()) {
                Object oneNull = newInstance(type);
                Object oneValue = newInstance(type);
                setProperty(oneValue, field, sampleValue(field.getType(), 0));
                assertNotEquals(oneNull, oneValue, type.getSimpleName() + " should distinguish null " + field.getName());
            }
        }
    }

    private List<Field> fieldsOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    private Object newInstance(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new AssertionError("Could not instantiate " + type.getName(), e);
        }
    }

    private void setProperty(Object target, Field field, Object value) {
        try {
            Method setter = target.getClass().getMethod("set" + capitalize(field.getName()), field.getType());
            setter.invoke(target, value);
        } catch (Exception e) {
            throw new AssertionError("Could not set " + target.getClass().getSimpleName() + "." + field.getName(), e);
        }
    }

    private Object getProperty(Object target, Field field) {
        try {
            String getterName = field.getType() == boolean.class ? "is" + capitalize(field.getName()) : "get" + capitalize(field.getName());
            Method getter = target.getClass().getMethod(getterName);
            return getter.invoke(target);
        } catch (Exception e) {
            throw new AssertionError("Could not get " + target.getClass().getSimpleName() + "." + field.getName(), e);
        }
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private Object sampleValue(Class<?> type, int variant) {
        if (type == String.class) {
            return variant == 0 ? "primary-value" : "alternate-value";
        }
        if (type == Long.class || type == long.class) {
            return variant == 0 ? 10L : 20L;
        }
        if (type == Integer.class || type == int.class) {
            return variant == 0 ? 5 : 9;
        }
        if (type == Boolean.class || type == boolean.class) {
            return variant == 0;
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.of(2026, 5, variant == 0 ? 5 : 6, 8, 0);
        }
        if (List.class.isAssignableFrom(type)) {
            return variant == 0 ? List.of("primary") : List.of("alternate");
        }
        if (type == OpenFdaDrugResponse.Meta.class) {
            return variant == 0 ? new OpenFdaDrugResponse.Meta() : withResultsMeta();
        }
        if (type == OpenFdaDrugResponse.Meta.ResultsMeta.class) {
            OpenFdaDrugResponse.Meta.ResultsMeta resultsMeta = new OpenFdaDrugResponse.Meta.ResultsMeta();
            resultsMeta.setTotal(variant == 0 ? 10 : 20);
            return resultsMeta;
        }
        if (type == OpenFdaDrugResponse.OpenFdaInfo.class) {
            OpenFdaDrugResponse.OpenFdaInfo info = new OpenFdaDrugResponse.OpenFdaInfo();
            info.setBrandName(List.of(variant == 0 ? "Brand" : "OtherBrand"));
            return info;
        }
        throw new AssertionError("Unsupported sample type " + type.getName());
    }

    private OpenFdaDrugResponse.Meta withResultsMeta() {
        OpenFdaDrugResponse.Meta meta = new OpenFdaDrugResponse.Meta();
        OpenFdaDrugResponse.Meta.ResultsMeta resultsMeta = new OpenFdaDrugResponse.Meta.ResultsMeta();
        resultsMeta.setTotal(20);
        meta.setResultsMeta(resultsMeta);
        return meta;
    }
}
