package org.datavyu.views.scriptcreator;

public class RubyArg {
    private String name;
    private String defaultValue;
    private String value;
    private boolean optional;
    private boolean returnValue;
    private String type;
    private String description;

    public RubyArg(RubyArg a) {
        this.name = a.getName();
        this.defaultValue = a.getDefaultValue();
        this.value = a.getValue();
        this.optional = a.getOptional();
        this.returnValue = a.isReturnValue();
        this.type = a.getType();
        this.description = a.getDescription();
    }

    public RubyArg(String name) {
        this.name = name;
        this.defaultValue = "";
        this.optional = false;
        this.returnValue = false;
        this.value = (defaultValue.length() > 0) ? defaultValue : "";
        this.type = "";
        this.description = "";
    }

    public RubyArg(String name, String defaultValue, boolean optional) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.optional = optional;
        this.returnValue = false;
        this.value = (defaultValue.length() > 0) ? defaultValue : "";
        this.type = "";
        this.description = "";
    }

    public RubyArg(String name, String defaultValue, boolean optional, boolean returnValue) {
        this(name, defaultValue, optional);
        this.returnValue = returnValue;
    }

    public RubyArg(String name, String defaultValue, boolean optional, String type, String description, boolean returnValue) {
        this(name, defaultValue, optional, returnValue);
        this.type = type;
        this.description = description;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        if(value.length() > 0)
            return value;
        else
            return getDefaultValue();
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean getOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isReturnValue() {
        return returnValue;
    }

    public void setReturnValue(boolean returnValue) {
        this.returnValue = returnValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String displayWithName() {
        if(getValue().length() == 0) {
            return getName();
        }
        return getName() + " = " + getValue();
    }

    public String display() {
        return getValue();
    }

    @Override
    public String toString() {
        if(getValue().length() > 0) {
            return displayWithName();
        } else {
            return displayWithName();
        }
    }
}
