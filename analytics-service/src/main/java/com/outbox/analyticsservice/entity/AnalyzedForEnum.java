package com.outbox.analyticsservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AnalyzedForEnum {
  FOR_CUSTOMER(18.0),
  FOR_COMPANY(22.5);

  private final double tax;
}
