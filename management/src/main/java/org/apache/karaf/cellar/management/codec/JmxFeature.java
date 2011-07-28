/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.management.codec;

import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.cellar.management.CellarFeaturesMBean;

import javax.management.openmbean.*;
import java.util.Collection;
import java.util.List;

/**
 * JMX representation of a feature.
 */
public class JmxFeature {

    static final CompositeType FEATURE;
    static final TabularType FEATURE_TABLE;

    static {
        FEATURE = createFeatureType();
        FEATURE_TABLE = createFeatureTableType();
    }

    private final CompositeData data;

    public JmxFeature(FeatureInfo feature, boolean installed) {
        try {
            String[] itemNames = CellarFeaturesMBean.FEATURE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = feature.getName();
            itemValues[1] = feature.getVersion();
            itemValues[2] = installed;
            data = new CompositeDataSupport(FEATURE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(Collection<JmxFeature> features) {
        TabularDataSupport table = new TabularDataSupport(FEATURE_TABLE);
        for (JmxFeature feature : features) {
            table.put(feature.asCompositeData());
        }
        return table;
    }

    private static CompositeType createFeatureType() {
        try {
            String description = "Description of a Karaf features managed into Cellar cluster";
            String[] itemNames = CellarFeaturesMBean.FEATURE;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];

            itemTypes[0] = SimpleType.STRING;
            itemDescriptions[0] = "The name of the feature";

            itemTypes[1] = SimpleType.STRING;
            itemDescriptions[1] = "The version of the feature";

            itemTypes[2] = SimpleType.BOOLEAN;
            itemDescriptions[2] = "Whether a feature is installed or not";

            return new CompositeType("Feature", description, itemNames, itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build feature type", e);
        }
    }

    private static TabularType createFeatureTableType() {
        try {
            return new TabularType("Features", "Table of all Karaf Cellar features", FEATURE, new String[]{CellarFeaturesMBean.FEATURE_NAME});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build feature table type", e);
        }
    }

    public static TabularData tableFrom(List<JmxFeature> features) {
        TabularDataSupport table = new TabularDataSupport(FEATURE_TABLE);
        for (JmxFeature feature : features) {
            table.put(feature.asCompositeData());
        }
        return table;
    }

}
