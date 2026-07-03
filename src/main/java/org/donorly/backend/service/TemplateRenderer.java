package org.donorly.backend.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateRenderer {

  public String render(String template, Map<String, String> variables) {
    if (template == null) {
      return "";
    }
    String result = template;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String placeholder = "{{" + entry.getKey() + "}}";
      String value = entry.getValue() != null ? entry.getValue() : "";
      result = result.replace(placeholder, value);
    }
    return result;
  }
}
