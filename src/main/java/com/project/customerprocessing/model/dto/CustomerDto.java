package com.project.customerprocessing.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
  private String name;
  private String branch;
  private String city;
  private String state;
  private String zip;
  private String phone;
  private String email;
  private String ip;
}

