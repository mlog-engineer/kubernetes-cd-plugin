package com.microsoft.jenkins.kubernetes;

import java.util.Map;

/**
 * Created by qixl on 2020/7/17.
 */
public class BaseResponse {
  private int code;
  private String msg;
  private Map data;

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public Map getData() {
    return data;
  }

  public void setData(Map data) {
    this.data = data;
  }
}
