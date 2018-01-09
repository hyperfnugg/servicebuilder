package no.obos.util.template;


import com.google.common.collect.ImmutableList;
import no.obos.util.servicebuilder.model.ServiceDefinition;
import no.obos.util.template.resources.TemplateResource;

import java.util.List;

public class TemplateDefinition implements ServiceDefinition {
    public static final String NAME = "template";
    public static final ImmutableList<Class> RESOURCES = ImmutableList.of(TemplateResource.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<Class> getResources() {
        return RESOURCES;
    }

    public static final TemplateDefinition instance = new TemplateDefinition();
}