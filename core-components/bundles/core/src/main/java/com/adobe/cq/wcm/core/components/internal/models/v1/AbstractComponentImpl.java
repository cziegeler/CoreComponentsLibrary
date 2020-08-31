/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.internal.models.v1;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.wcm.core.components.internal.DataLayerConfig;
import com.adobe.cq.wcm.core.components.internal.Utils;
import com.adobe.cq.wcm.core.components.internal.models.v1.datalayer.ComponentDataImpl;
import com.adobe.cq.wcm.core.components.models.Component;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.components.ComponentContext;
import com.fasterxml.jackson.annotation.JsonIgnore;

import static com.adobe.cq.wcm.core.components.internal.Utils.ID_SEPARATOR;

/**
 * Abstract class that can be used as a base class for {@link Component} implementations.
 */
public abstract class AbstractComponentImpl implements Component {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractComponentImpl.class);

    @SlingObject
    protected Resource resource;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Nullable
    protected ComponentContext componentContext;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Nullable
    private Page currentPage;

    private String id;
    private Boolean dataLayerEnabled;
    private ComponentData componentData;

    @Nullable
    @Override
    public String getId() {
        if (id == null) {
            if (resource != null) {
                ValueMap properties = resource.getValueMap();
                id = properties.get(Component.PN_ID, String.class);
            }
            if (StringUtils.isEmpty(id)) {
                id = generateId();
            } else {
                id = StringUtils.replace(StringUtils.normalizeSpace(StringUtils.trim(id)), " ", ID_SEPARATOR);
            }
        }
        return id;
    }

    @NotNull
    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }

    /**
     * Returns an auto generated component ID.
     *
     * The ID is the first 10 characters of an SHA-1 hexadecimal hash of the component path,
     * prefixed with the component name. Example: title-810f3af321
     *
     * If the component is referenced, the path is taken to be a concatenation of the component path,
     * with the path of the first parent context resource that exists on the page or in the template.
     * This ensures the ID is unique if the same component is referenced multiple times on the same page or template.
     *
     * Collision
     * ---------
     * c = expected collisions
     * c ~= (i^2)/(2m) - where i is the number of items and m is the number of possibilities for each item.
     * m = 16^n - for a hexadecimal string, where n is the number of characters.
     *
     * For i = 1000 components with the same name, and n = 10 characters:
     *
     * c ~= (1000^2)/(2*(16^10))
     * c ~= 0.00000045
     *
     * @return the auto generated component ID
     */
    private String generateId() {
        String resourceType = resource.getResourceType();
        String prefix = StringUtils.substringAfterLast(resourceType, "/");
        String path = resource.getPath();
        if (currentPage != null && componentContext != null) {
            PageManager pageManager = currentPage.getPageManager();
            Page containingPage = pageManager.getContainingPage(resource);
            Template template = currentPage.getTemplate();
            boolean inCurrentPage = (containingPage != null && StringUtils.equals(containingPage.getPath(), currentPage.getPath()));
            boolean inTemplate = (template != null && path.startsWith(template.getPath()));
            if (!inCurrentPage && !inTemplate) {
                ComponentContext parentContext = componentContext.getParent();
                while (parentContext != null) {
                    Resource parentContextResource = parentContext.getResource();
                    if (parentContextResource != null) {
                        Page parentContextPage = pageManager.getContainingPage(parentContextResource);
                        inCurrentPage = (parentContextPage != null && StringUtils.equals(parentContextPage.getPath(), currentPage.getPath()));
                        inTemplate = (template != null && parentContextResource.getPath().startsWith(template.getPath()));
                        if (inCurrentPage || inTemplate) {
                            path = parentContextResource.getPath().concat(resource.getPath());
                            break;
                        }
                    }
                    parentContext = parentContext.getParent();
                }
            }

        }

        return Utils.generateId(prefix, path);
    }

    private boolean isDataLayerEnabled() {
        if (dataLayerEnabled == null) {
            dataLayerEnabled = false;
            if (resource != null) {
                ConfigurationBuilder builder = resource.adaptTo(ConfigurationBuilder.class);
                if (builder != null) {
                    DataLayerConfig dataLayerConfig = builder.as(DataLayerConfig.class);
                    dataLayerEnabled = dataLayerConfig.enabled();
                }
            }
        }
        return dataLayerEnabled;
    }


    /**
     * See {@link Component#getData()}
     *
     * @return The component data
     */
    @Override
    @Nullable
    public ComponentData getData() {
        if (!isDataLayerEnabled()) {
            return null;
        }
        if (componentData == null) {
            componentData = getComponentData();
        }
        return componentData;
    }

    /**
     * Data layer specific methods. Each component can choose to implement some of these, to override or feed the data model.
     */

    /**
     * Override this method to provide a different data model for your component. This will be called by
     * {@link AbstractComponentImpl#getData()} in case the datalayer is activated
     *
     * @return The component data
     */
    @NotNull
    protected ComponentData getComponentData() {
        return new ComponentDataImpl(this, resource);
    }

    @JsonIgnore
    public Resource getDataLayerAssetResource() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerTitle() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerDescription() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerText() {
        return null;
    }

    @JsonIgnore
    public String[] getDataLayerTags() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerUrl() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerLinkUrl() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerTemplatePath() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerLanguage() {
        return null;
    }

    @JsonIgnore
    public String[] getDataLayerShownItems() {
        return null;
    }
}
