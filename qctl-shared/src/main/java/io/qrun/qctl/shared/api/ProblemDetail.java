package io.qrun.qctl.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {
  public String type;
  public String title;
  public Integer status;
  public String detail;
  public String instance;
  public String[] errors;
}
