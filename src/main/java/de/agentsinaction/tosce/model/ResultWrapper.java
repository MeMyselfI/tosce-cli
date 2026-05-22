package de.agentsinaction.tosce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultWrapper {
    public boolean success;
    public int total;
    public JsonNode root;

    public boolean isSuccess() { return success; }
}
