package com.cisco.vss.foundation.configuration.xml;

import com.cisco.vss.foundation.configuration.xml.jaxb.NamespaceDefinition;
import com.cisco.vss.foundation.configuration.xml.jaxb.NamespaceDependency;
import com.cisco.vss.foundation.configuration.xml.jaxb.NamespaceIdentifier;

import java.util.ArrayList;
import java.util.List;

public class Namespace {
    private NamespaceDefinition namespaceDef = null;
    private String id = null;
    private List<String> directDependencyIds = null;

    public Namespace(NamespaceDefinition def) throws XmlException {
        if(def == null) {
            throw new XmlException("The given NamespaceDefinition was null");
        }
        id = getNamespaceToken(def.getNamespaceIdentifier());
        directDependencyIds = createDirectDependencyIdList(id, def);
        namespaceDef = def;
    }

    public final String getId() {
        return(id);
    }
    public final NamespaceDefinition getNamespaceDefinition() {
        return(namespaceDef);
    }
    public final List<String> getDirectDependencyIds() {
        return(directDependencyIds);
    }

    @Override
    public boolean equals(Object e) {
        try {
            return(getId().equals(((Namespace)e).getId()));
        } catch(Throwable t) {
            return(false);
        }
    }

    private List<String> createDirectDependencyIdList(String id, NamespaceDefinition def) throws XmlException {

        if(def == null) {
            throw new XmlException("Cannot create Direct Dependency Id List from null");
        }

        ArrayList<String> dependencies = new ArrayList<String>();

        List<NamespaceDependency> deps = def.getNamespaceDependencies();
        if( (deps != null) && (!deps.isEmpty()) ) {
            for(NamespaceDependency dep: deps) {
                String token = getNamespaceToken(dep.getNamespaceIdentifier());
                if(dependencies.contains(token)) {
                    throw new XmlException("Duplicate dependency found in " + id + ": " + token);
                }
                if(id.equals(token)) {
                    throw new XmlException("Namespace, " + id + ", depends on itself");
                }
                dependencies.add(token);
            }
        }

        return(dependencies);
    }
    private String getNamespaceToken(NamespaceIdentifier namespaceId) throws XmlException {
        if(namespaceId == null) {
            throw new XmlException("The given namespace has no NamespaceIdentifier");
        }
        String name = namespaceId.getName();
        String version = namespaceId.getVersion();

        if( (name==null) || (name.trim().length() == 0) ) {
            throw new XmlException("The given namespace has no NamespaceIdentifier.name");
        }
        if( (version==null) || (version.trim().length() == 0) ) {
            throw new XmlException("The given namespace has no NamespaceIdentifier.version");
        }
        return("[" + name.trim() + "].[" + version.trim() + "]");
    }
    @Override
    public String toString() {
        return(getId() + ": " + getDirectDependencyIds());
    }
}
